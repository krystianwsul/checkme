package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.criteria.Assignable
import com.krystianwsul.common.criteria.QueryMatchable
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.cache.ClearableInvalidatableManager
import com.krystianwsul.common.firebase.models.cache.InvalidatableCache
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.cache.invalidatableCache
import com.krystianwsul.common.firebase.models.interval.*
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.schedule.*
import com.krystianwsul.common.firebase.models.taskhierarchy.NestedTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

sealed class Task(
    val customTimeProvider: JsonTime.CustomTimeProvider,
    private val taskRecord: TaskRecord,
    val parentTaskDelegateFactory: ParentTaskDelegate.Factory,
    val clearableInvalidatableManager: ClearableInvalidatableManager,
    val rootModelChangeManager: RootModelChangeManager,
) : Endable, CurrentOffset, QueryMatchable, Assignable {

    abstract val parent: Parent
    abstract val project: Project<*>

    private val endDataProperty = invalidatableLazyCallbacks {
        taskRecord.endData?.let {
            EndData(
                ExactTimeStamp.Local(it.time),
                it.deleteInstances,
                ExactTimeStamp.Offset.fromOffset(it.time, it.offset),
            )
        }
    }
    val endData by endDataProperty

    @Suppress("PropertyName")
    protected val _schedules = mutableListOf<Schedule>()

    abstract val noScheduleOrParents: Collection<NoScheduleOrParent>

    val name get() = taskRecord.name

    val schedules: List<Schedule> get() = _schedules

    val startExactTimeStamp = ExactTimeStamp.Local(taskRecord.startTime)

    final override val startExactTimeStampOffset by lazy {
        taskRecord.run { ExactTimeStamp.Offset.fromOffset(startTime, startTimeOffset) }
    }

    final override val endExactTimeStamp get() = endData?.exactTimeStampLocal
    final override val endExactTimeStampOffset get() = endData?.exactTimeStampOffset

    val note get() = taskRecord.note

    abstract val taskKey: TaskKey

    val id get() = taskRecord.id

    val existingInstances: Map<InstanceScheduleKey, Instance> get() = _existingInstances

    val imageJson get() = taskRecord.image

    protected abstract val projectParentTaskHierarchies: Set<ProjectTaskHierarchy>

    val nestedParentTaskHierarchies = taskRecord.taskHierarchyRecords
        .mapValues { NestedTaskHierarchy(this, it.value, parentTaskDelegateFactory) }
        .toMutableMap()

    val parentTaskHierarchies get() = projectParentTaskHierarchies + nestedParentTaskHierarchies.values

    protected abstract val allowPlaceholderCurrentNoSchedule: Boolean

    val intervalInfoCache = invalidatableCache<IntervalInfo>(clearableInvalidatableManager) {
        checkNoIntervalUpdate()

        InvalidatableCache.ValueHolder(IntervalBuilder.build(this, allowPlaceholderCurrentNoSchedule)) { }
    }
    val intervalInfo by intervalInfoCache

    val childHierarchyIntervalsCache =
        invalidatableCache<List<HierarchyInterval>>(clearableInvalidatableManager) { invalidatableCache ->
            val hierarchyIntervalPairs = parent.getTaskHierarchiesByParentTaskKey(taskKey)
                .asSequence()
                .map { it.childTask }
                .distinct()
                .flatMap { childTask ->
                    childTask.intervalInfo
                        .parentHierarchyIntervals
                        .map { childTask to it }
                }
                .filter { it.second.taskHierarchy.parentTaskKey == taskKey }
                .toList()

            val childTasks = hierarchyIntervalPairs.map { it.first }.distinct()

            val hierarchyIntervals = hierarchyIntervalPairs.map { it.second }

            val childTaskIntervalInfoRemovables = childTasks.map {
                it.intervalInfoCache
                    .invalidatableManager
                    .addInvalidatable(invalidatableCache)
            }

            val changeManagerRemovable =
                rootModelChangeManager.rootModelInvalidatableManager.addInvalidatable(invalidatableCache)

            InvalidatableCache.ValueHolder(hierarchyIntervals) {
                childTaskIntervalInfoRemovables.forEach { it.remove() }

                changeManagerRemovable.remove()
            }
        }

    val childHierarchyIntervals by childHierarchyIntervalsCache

    private val _existingInstances = taskRecord.instanceRecords
        .values
        .map { Instance(this, it) }
        .associateBy { it.scheduleKey }
        .toMutableMap()

    var ordinal
        get() = taskRecord.ordinal ?: startExactTimeStamp.long.toOrdinal()
        set(value) {
            taskRecord.ordinal = value
        }

    /*
    todo this shouldn't include the project unconditionally.  For instance, if I'm searching in ShowTaskActivity, I already
    know they're all in the same project.  And the project name isn't even visible anywhere.
     */
    protected val normalizedFieldsDelegate = invalidatableLazy {
        listOfNotNull(name, note, project.name).map { it.normalized() }
    }
    final override val normalizedFields by normalizedFieldsDelegate

    abstract val projectCustomTimeIdProvider: JsonTime.ProjectCustomTimeIdProvider

    abstract val dependenciesLoaded: Boolean

    // hack24 = false -> basically, is it possible to add a subtask
    fun isVisible(now: ExactTimeStamp.Local, hack24: Boolean = false): Boolean {
        // can't add to deleted tasks
        if (!notDeleted) return false

        // in general, we can add a subtask to any task that is either unscheduled, or has not done instances.  Checking
        // for that will be difficult, though.

        val topLevelTask = getTopLevelTask()

        // if it's in the unscheduled tasks list, we can add a subtask
        if (topLevelTask.intervalInfo.isUnscheduled()) return true

        // ... and if not, we can just use getInstances() and check all of them.
        return getInstances(null, null, now).any { it.canAddSubtask(now, hack24) }
    }

    // hack24 = false -> basically, is it possible to add a subtask
    fun isVisibleHelper(now: ExactTimeStamp.Local, hack24: Boolean = false): Pair<Boolean, String> {
        // can't add to deleted tasks
        if (!notDeleted) return false to "deleted"

        // in general, we can add a subtask to any task that is either unscheduled, or has not done instances.  Checking
        // for that will be difficult, though.

        val topLevelTask = getTopLevelTask()

        // if it's in the unscheduled tasks list, we can add a subtask
        if (topLevelTask.intervalInfo.isUnscheduled()) return true to "is unscheduled"

        // ... and if not, we can just use getInstances() and check all of them.
        return getInstances(null, null, now).any { it.canAddSubtask(now, hack24) } to "based off instances"
    }

    fun canMigrateDescription(now: ExactTimeStamp.Local) = !note.isNullOrEmpty() && isVisible(now)

    fun getTopLevelTask(): Task = parentTask?.getTopLevelTask() ?: this

    fun isTopLevelTask() = parentTask == null

    fun getNestedTaskHierarchy(taskHierarchyId: TaskHierarchyId) = nestedParentTaskHierarchies.getValue(taskHierarchyId)

    private val parentTaskDataCache =
        invalidatableCache<Pair<Task, SingleSchedule?>?>(clearableInvalidatableManager) { invalidatableCache ->
            val intervalRemovable = intervalInfoCache.invalidatableManager.addInvalidatable(invalidatableCache)

            val interval = intervalInfo.intervals.last()

            when (val type = interval.type) {
                is Type.Child -> type.getHierarchyInterval(interval)
                    .taskHierarchy
                    .let {
                        val parentTaskRemovable = it.parentTaskCache
                            .invalidatableManager
                            .addInvalidatable(invalidatableCache)

                        InvalidatableCache.ValueHolder(it.parentTask to null) {
                            intervalRemovable.remove()
                            parentTaskRemovable.remove()
                        }
                    }
                is Type.Schedule -> {
                    // hierarchy hack
                    type.getScheduleIntervals(interval)
                        .singleOrNull()
                        ?.schedule
                        ?.let { it as? SingleSchedule }
                        ?.let { singleSchedule ->
                            val instance = singleSchedule.getInstance(this)

                            val parentInstanceRemovable = instance.parentInstanceCache
                                .invalidatableManager
                                .addInvalidatable(invalidatableCache)

                            InvalidatableCache.ValueHolder(
                                instance.parentInstance
                                    ?.task
                                    ?.let { it to singleSchedule }
                            ) {
                                intervalRemovable.remove()
                                parentInstanceRemovable.remove()
                            }
                        }
                        ?: InvalidatableCache.ValueHolder(null) { intervalRemovable.remove() }
                }
                is Type.NoSchedule -> InvalidatableCache.ValueHolder(null) { intervalRemovable.remove() }
            }
        }

    val parentTaskData by parentTaskDataCache
    val parentTask get() = parentTaskData?.first

    val isHierarchyHack get() = parentTaskData?.second != null

    fun getExistingInstances(
        startExactTimeStamp: ExactTimeStamp.Offset?,
        endExactTimeStamp: ExactTimeStamp.Offset?,
        onlyRoot: Boolean,
    ): Sequence<Instance> {
        return _existingInstances.values
            .asSequence()
            .run { if (onlyRoot) filter { it.isRootInstance() } else this }
            .map { it.instanceDateTime to it }
            .filter {
                InterruptionChecker.throwIfInterrupted()

                val exactTimeStamp = it.first.toLocalExactTimeStamp()

                if (startExactTimeStamp?.let { exactTimeStamp < it } == true) return@filter false

                if (endExactTimeStamp?.let { exactTimeStamp >= it } == true) return@filter false

                true
            }
            .sortedBy { it.first } // this evaluates everything earlier
            .map { it.second }
    }

    // contains only generated instances
    private fun getParentInstances(
        givenStartExactTimeStamp: ExactTimeStamp.Offset?,
        givenEndExactTimeStamp: ExactTimeStamp.Offset?,
        now: ExactTimeStamp.Local,
        excludedParentTasks: Set<TaskKey>,
    ): Sequence<InstanceInfo> {
        val instanceSequences = intervalInfo.parentHierarchyIntervals
            .map { it.taskHierarchy }
            .filter { it.parentTaskKey !in excludedParentTasks }
            .map {
                it.parentTask
                    .getInstances(
                        givenStartExactTimeStamp,
                        listOfNotNull(givenEndExactTimeStamp, it.endExactTimeStampOffset).minOrNull(),
                        now,
                        excludedParentTasks = excludedParentTasks + taskKey,
                    )
                    .filter { it.isVisible(now, Instance.VisibilityOptions(hack24 = true)) }
                    .map {
                        /*
                        Using InstanceInfo adds overhead, but:
                        1. The above parent.getInstances call doesn't filter by the hierarchyInterval's start/end, because
                        the logic for determining appropriate hierarchyIntervals is in
                        Instance.getTaskHierarchyParentInstance and it's kinda crazy.  I know that here I can't limit by
                        start, and I don't *think* I can limit by end, either.
                        2. But there are certain hierarchyInfo configurations in which this sequence will de facto have no
                        instances, but instead of returning an empty sequence, this function will iterate endlessly over
                        parent instances.  So, I need to return a placeholder for a given date, to indicate that it's been
                        checked.

                        Of course, the sane thing would be to rewrite this logic to return an empty sequence in that
                        situation.  (Presumably figure out what to do about the end range, since that extra logic in
                        Instance.getTaskHierarchyParentInstance may not be relevant for virtual instances.)  But that's,
                        like, super complicated, and this seems to be harmless for now.

                        I added EndedTaskHierarchyException to the Instance algorithm to narrow this down.  Next time I'm
                        sniffing around here, and that exception has never been logged, that means I'm in the clear to
                        limit the upper range with something like:

                        givenEndExactTimeStamp -> listOfNotNull(givenEndExactTimeStamp, hierarchyInterval.endOffset).minOrNull()

                        Update: I did narrow down givenEndExactTimeStamp.  I'm going to leave that exception floating around
                        for a while longer.  But, at this point it seems that InstanceInfo is no longer needed.  Next time
                        this comes up, I can remove InstanceInfo and undo the changes I made to create this mechanism.
                         */

                        InstanceInfo(
                            it.instanceDateTime,
                            it.getChildInstances()
                                .filter { it.taskKey == taskKey }
                                .singleOrEmpty()
                                ?.takeIf { !it.exists() },
                        )
                    }
            }

        return combineInstanceInfoSequences(instanceSequences)
    }

    // contains only generated, root instances that aren't virtual parents
    private fun getScheduleInstances(
        startExactTimeStamp: ExactTimeStamp.Offset?,
        endExactTimeStamp: ExactTimeStamp.Offset?,
    ): Sequence<Instance> {
        val scheduleSequence = getScheduleDateTimes(startExactTimeStamp, endExactTimeStamp)

        return scheduleSequence.flatMap {
            InterruptionChecker.throwIfInterrupted()

            it.asSequence()
                .map { it.first }
                .distinct()
                .map(::getInstance)
                .filter {
                    val tracker = TimeLogger.startIfLogDone("Task.getScheduleInstances filter")

                    val ret = !it.exists() && it.isRootInstance()

                    tracker?.stop()

                    ret
                } // I don't know if the root part is necessary, now that group tasks are removed
        }
    }

    /*
     Note: this groups by the DateTime's Date and HourMinute, not strict equality.  A list may have pairs with various
     customTimes, for example.
     */
    fun getScheduleDateTimes(
        startExactTimeStamp: ExactTimeStamp.Offset?,
        endExactTimeStamp: ExactTimeStamp.Offset?,
        originalDateTime: Boolean = false,
        checkOldestVisible: Boolean = true,
    ): Sequence<List<Pair<DateTime, ScheduleInterval>>> {
        if (endExactTimeStamp != null && startExactTimeStamp != null && endExactTimeStamp < startExactTimeStamp)
            return sequenceOf()

        val scheduleResults = intervalInfo.scheduleIntervals.map { scheduleInterval ->
            scheduleInterval.getDateTimesInRange(
                startExactTimeStamp,
                endExactTimeStamp,
                originalDateTime,
                checkOldestVisible,
            ).map { it to scheduleInterval }
        }

        return combineSequencesGrouping(scheduleResults) {
            val tracker = TimeLogger.startIfLogDone("Task.getScheduleDateTimes group")

            InterruptionChecker.throwIfInterrupted()

            val nextDateTime = it.filterNotNull()
                .minByOrNull { it.first }!!
                .first

            it.mapIndexed { index, dateTime -> index to dateTime }
                .filter { it.second?.first?.compareTo(nextDateTime) == 0 }
                .map { it.first }
                .also { tracker?.stop() }
        }
    }

    fun mayHaveRootInstances() = _schedules.isNotEmpty() || _existingInstances.isNotEmpty()

    private var gettingInstances = false

    enum class ExistingVirtual {

        EXISTING, VIRTUAL, ALL
    }

    /*
    todo: `now` seems to be only used for filtering parent instances.  But, I think this is redundant.  Removing the
    filtering doesn't break any tests.  If I want to remove `now` later, just make sure that production data looks the same
    before/after.
     */
    fun getInstances(
        startExactTimeStamp: ExactTimeStamp.Offset?,
        endExactTimeStamp: ExactTimeStamp.Offset?,
        now: ExactTimeStamp.Local,
        onlyRoot: Boolean = false,
        filterVisible: Boolean = true,
        excludedParentTasks: Set<TaskKey> = emptySet(),
        existingVirtual: ExistingVirtual = ExistingVirtual.ALL,
    ): Sequence<Instance> {
        check(!gettingInstances)
        gettingInstances = true

        return try {
            InterruptionChecker.throwIfInterrupted()

            if (filterVisible && !notDeleted && endData!!.deleteInstances) {
                if (existingVirtual == ExistingVirtual.VIRTUAL) {
                    emptySequence()
                } else {
                    getExistingInstances(startExactTimeStamp, endExactTimeStamp, onlyRoot).filter { it.done != null }
                }
            } else {
                val instanceInfoSequences = mutableListOf<Sequence<InstanceInfo>>()

                if (existingVirtual != ExistingVirtual.VIRTUAL) {
                    instanceInfoSequences +=
                        getExistingInstances(startExactTimeStamp, endExactTimeStamp, onlyRoot).map(::InstanceInfo)
                }

                if (existingVirtual != ExistingVirtual.EXISTING) {
                    if (!onlyRoot) {
                        instanceInfoSequences += getParentInstances(
                            startExactTimeStamp,
                            endExactTimeStamp,
                            now,
                            excludedParentTasks,
                        )
                    }

                    instanceInfoSequences += getScheduleInstances(startExactTimeStamp, endExactTimeStamp).map(::InstanceInfo)
                }

                combineInstanceInfoSequences(instanceInfoSequences).toInstances()
            }
        } finally {
            check(gettingInstances)
            gettingInstances = false
        }
    }

    fun getHierarchyExactTimeStamp(exactTimeStamp: ExactTimeStamp) =
        exactTimeStamp.coerceIn(startExactTimeStampOffset, endExactTimeStampOffset?.minusOne())

    fun getHierarchyChildTasks() = getChildTaskHierarchies().map { it.childTask }.toSet()

    fun getChildTasks(): Set<Task> {
        val taskHierarchyChildTasks = getHierarchyChildTasks()

        /**
         * todo if performance becomes an issue here, then I can try caching the part below.  I believe that I'd need to
         * reset the cache not only when tasks change, but also from within the instance when its parentInstance changes,
         * on both its old and new parentInstance.task
         */

        // hierarchy hack
        val instanceChildTasks = parent.getAllExistingInstances()
            .filter { it.parentInstance?.task == this }
            .map { it.task }
            .filter { it.parentTask == this }

        return taskHierarchyChildTasks + instanceChildTasks
    }

    fun getChildTaskHierarchies(): List<TaskHierarchy> {
        /**
         * todo if performance becomes an issue, then I can cache this, invalidating on childHierarchyIntervalsCache (this
         * covers it.notDeletedOffset() && it.taskHierarchy.notDeleted), then add invalidating on childTask.endDataProperty
         */

        val taskHierarchies = childHierarchyIntervals.filter {
            it.notDeletedOffset() && it.taskHierarchy.notDeleted && it.taskHierarchy.childTask.notDeleted
        }
            .map { it.taskHierarchy }
            .toMutableSet()

        return taskHierarchies.sortedBy { it.childTask.ordinal }
    }

    fun getImage(deviceDbInfo: DeviceDbInfo): ImageState? {
        val image = taskRecord.image ?: return null

        return if (image.uploaderUuid != null) {
            if (image.uploaderUuid == deviceDbInfo.uuid)
                ImageState.Local(image.imageUuid)
            else
                ImageState.Uploading
        } else {
            ImageState.Remote(image.imageUuid)
        }
    }

    init {
        _schedules += taskRecord.singleScheduleRecords
            .values
            .map { SingleSchedule(this, it) }

        _schedules += taskRecord.weeklyScheduleRecords
            .values
            .map { WeeklySchedule(this, it) }

        _schedules += taskRecord.monthlyDayScheduleRecords
            .values
            .map { MonthlyDaySchedule(this, it) }

        _schedules += taskRecord.monthlyWeekScheduleRecords
            .values
            .map { MonthlyWeekSchedule(this, it) }

        _schedules += taskRecord.yearlyScheduleRecords
            .values
            .map { YearlySchedule(this, it) }
    }

    fun setMyEndExactTimeStamp(endData: EndData?) {
        taskRecord.setEndData(
            endData?.let {
                RootTaskJson.EndData(it.exactTimeStampLocal.long, it.exactTimeStampLocal.offset, it.deleteInstances)
            }
        )

        endDataProperty.invalidate()
    }

    protected abstract fun deleteFromParent()

    var deleted = false
        private set

    fun delete() {
        deleted = true

        existingInstances.values
            .toMutableList()
            .forEach { it.delete() }

        schedules.toMutableList().forEach { it.delete() }

        deleteFromParent()
        taskRecord.delete()
    }

    fun deleteSchedule(schedule: Schedule) {
        check(_schedules.contains(schedule))

        _schedules.remove(schedule)
        invalidateIntervals()
    }

    fun createRemoteInstanceRecord(instance: Instance): InstanceRecord {
        check(generatedInstances.containsKey(instance.instanceKey))

        generatedInstances.remove(instance.instanceKey)

        val instanceRecord = taskRecord.newInstanceRecord(InstanceJson(), instance.scheduleKey)

        _existingInstances[instance.scheduleKey] = instance

        rootModelChangeManager.invalidateExistingInstances()

        return instanceRecord
    }

    fun deleteInstance(instance: Instance) {
        val scheduleKey = instance.scheduleKey

        check(_existingInstances.containsKey(scheduleKey))
        check(instance == _existingInstances[scheduleKey])

        _existingInstances.remove(scheduleKey)

        rootModelChangeManager.invalidateExistingInstances()
    }

    private fun getExistingInstanceIfPresent(instanceScheduleKey: InstanceScheduleKey) =
        _existingInstances[instanceScheduleKey]

    fun getInstance(scheduleDateTime: DateTime): Instance {
        val scheduleKey = InstanceScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        val existingInstance = getExistingInstanceIfPresent(scheduleKey)

        return (existingInstance ?: generateInstance(scheduleDateTime))
    }

    protected abstract fun getDateTime(instanceScheduleKey: InstanceScheduleKey): DateTime

    fun getInstance(instanceScheduleKey: InstanceScheduleKey) = getInstance(getDateTime(instanceScheduleKey))

    abstract fun getOrCopyTime(
        dayOfWeek: DayOfWeek,
        time: Time,
        customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        now: ExactTimeStamp.Local,
    ): Time

    abstract fun invalidateProjectParentTaskHierarchies()

    fun invalidateChildTaskHierarchies() = childHierarchyIntervalsCache.invalidate()

    fun invalidateIntervals() {
        val intervalUpdate = getIntervalUpdate()

        if (intervalUpdate != null) {
            intervalUpdate.invalidateIntervals()
        } else {
            intervalInfoCache.invalidate()
        }
    }

    fun getScheduleTextMultiline(scheduleTextFactory: ScheduleTextFactory): String {
        val currentScheduleIntervals = intervalInfo.currentScheduleIntervals

        return ScheduleGroup.getGroups(currentScheduleIntervals.map { it.schedule }).joinToString("\n") {
            scheduleTextFactory.getScheduleText(it, customTimeProvider)
        }
    }

    private val generatedInstances = mutableMapOf<InstanceKey, Instance>()
    private val generatedInstancesLog = mutableListOf<String>().synchronized()

    private fun generateInstance(scheduleDateTime: DateTime): Instance {
        val instanceKey = InstanceKey(taskKey, scheduleDateTime.date, scheduleDateTime.time.timePair)

        if (!generatedInstances.containsKey(instanceKey)) {
            generatedInstancesLog += "adding $instanceKey from " + getThreadInfo() + ", " + Exception().stackTraceToString()
            generatedInstances[instanceKey] = Instance(this, scheduleDateTime)
        }

        return generatedInstances[instanceKey]
            ?: throw InstanceKeyNotFoundException(
                "instanceKey: $instanceKey; \nmap keys: " + generatedInstances.keys.joinToString(
                    ", \n"
                ) + "; \n log: " + generatedInstancesLog.joinToString(", \n")
            )
    }

    fun getScheduleText(scheduleTextFactory: ScheduleTextFactory, showParent: Boolean = false): String? {
        val currentScheduleIntervals = intervalInfo.currentScheduleIntervals
        val parentTask = parentTask

        return if (parentTask == null) {
            ScheduleGroup.getGroups(currentScheduleIntervals.map { it.schedule }).joinToString(", ") {
                scheduleTextFactory.getScheduleText(it, customTimeProvider)
            }
        } else {
            parentTask.name.takeIf { showParent }
        }
    }

    fun correctIntervalEndExactTimeStamps() = intervalInfo.intervals
        .asSequence()
        .filterIsInstance<Interval.Ended>()
        .forEach { it.correctEndExactTimeStamps() }

    fun hasOtherVisibleInstances(now: ExactTimeStamp.Local, instanceKey: InstanceKey?): Boolean {
        instanceKey?.let { check(it.taskKey == taskKey) }

        return getInstances(
            null,
            null,
            now,
        ).filter { it.instanceKey != instanceKey }
            .filter { it.isVisible(now, Instance.VisibilityOptions()) }
            .any()
    }

    final override fun toString() = super.toString() + ", name: $name, taskKey: $taskKey"

    override fun getAssignedTo(): List<ProjectUser> {
        val currentScheduleIntervals = intervalInfo.currentScheduleIntervals

        return if (currentScheduleIntervals.isEmpty()) {
            listOf()
        } else {
            currentScheduleIntervals.map { it.schedule.assignedTo }
                .distinct()
                .single()
                .let(project::getAssignedTo)
                .map { it.value }
        }
    }

    fun deleteNestedTaskHierarchy(nestedTaskHierarchy: NestedTaskHierarchy) {
        check(nestedParentTaskHierarchies.containsKey(nestedTaskHierarchy.id))

        nestedParentTaskHierarchies.remove(nestedTaskHierarchy.id)

        nestedTaskHierarchy.invalidateTasks()
    }

    interface ScheduleTextFactory {

        fun getScheduleText(scheduleGroup: ScheduleGroup, customTimeProvider: JsonTime.CustomTimeProvider): String
    }

    data class EndData(
        val exactTimeStampLocal: ExactTimeStamp.Local,
        val deleteInstances: Boolean,
        val exactTimeStampOffset: ExactTimeStamp.Offset = exactTimeStampLocal.toOffset(),
    )

    private class InstanceKeyNotFoundException(message: String) : Exception(message)

    interface Parent {

        fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy>

        fun getTask(taskKey: TaskKey): Task

        fun getInstance(instanceKey: InstanceKey) = getTask(instanceKey.taskKey).getInstance(instanceKey.instanceScheduleKey)

        fun getAllExistingInstances(): Sequence<Instance>
    }
}
