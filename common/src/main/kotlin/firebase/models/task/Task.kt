package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.criteria.Assignable
import com.krystianwsul.common.criteria.QueryMatchable
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.models.*
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
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

sealed class Task(
    val customTimeProvider: JsonTime.CustomTimeProvider,
    private val taskRecord: TaskRecord,
    val parentTaskDelegate: ParentTaskDelegate,
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
        .mapValues { NestedTaskHierarchy(this, it.value, parentTaskDelegate) }
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
        get() = taskRecord.ordinal ?: startExactTimeStamp.long.toDouble()
        set(value) {
            taskRecord.ordinal = value
        }

    protected val normalizedFieldsDelegate = invalidatableLazy {
        listOfNotNull(name, note, project.name).map { it.normalized() }
    }
    final override val normalizedFields by normalizedFieldsDelegate

    abstract val projectCustomTimeIdProvider: JsonTime.ProjectCustomTimeIdProvider

    abstract val dependenciesLoaded: Boolean

    fun getParentName(exactTimeStamp: ExactTimeStamp) = getParentTask(exactTimeStamp)?.name ?: project.name

    // hack24 = false -> basically, is it possible to add a subtask
    fun isVisible(now: ExactTimeStamp.Local, hack24: Boolean = false): Boolean {
        // can't add to deleted tasks
        if (!notDeleted) return false

        // in general, we can add a subtask to any task that is either unscheduled, or has not done instances.  Checking
        // for that will be difficult, though.

        val topLevelTask = getTopLevelTask(now)

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

        val topLevelTask = getTopLevelTask(now)

        // if it's in the unscheduled tasks list, we can add a subtask
        if (topLevelTask.intervalInfo.isUnscheduled()) return true to "is unscheduled"

        // ... and if not, we can just use getInstances() and check all of them.
        return getInstances(null, null, now).any { it.canAddSubtask(now, hack24) } to "based off instances"
    }

    private fun getTopLevelTask(exactTimeStamp: ExactTimeStamp): Task =
        getParentTask(exactTimeStamp)?.getTopLevelTask(exactTimeStamp) ?: this

    fun isTopLevelTask(exactTimeStamp: ExactTimeStamp): Boolean {
        requireCurrentOffset(exactTimeStamp)

        return getParentTask(exactTimeStamp) == null
    }

    fun getNestedTaskHierarchy(taskHierarchyId: TaskHierarchyId) = nestedParentTaskHierarchies.getValue(taskHierarchyId)

    fun getParentTask(exactTimeStamp: ExactTimeStamp): Task? {
        requireNotDeletedOffset(exactTimeStamp)

        return intervalInfo.getParentTaskHierarchy(exactTimeStamp)?.run {
            requireNotDeletedOffset(exactTimeStamp)
            taskHierarchy.requireNotDeletedOffset(exactTimeStamp)

            taskHierarchy.parentTask.apply { requireNotDeletedOffset(exactTimeStamp) }
        }
    }

    private fun getExistingInstances(
        startExactTimeStamp: ExactTimeStamp.Offset?,
        endExactTimeStamp: ExactTimeStamp.Offset?,
        onlyRoot: Boolean,
    ): Sequence<Instance> {
        return _existingInstances.values
            .asSequence()
            .run { if (onlyRoot) filter { it.isRootInstance() } else this }
            .map { it.instanceDateTime to it }
            .filterByDateTime(startExactTimeStamp, endExactTimeStamp)
    }

    private fun <T> Sequence<Pair<DateTime, T>>.filterByDateTime(
        startExactTimeStamp: ExactTimeStamp.Offset?,
        endExactTimeStamp: ExactTimeStamp.Offset?,
    ) = filter {
        InterruptionChecker.throwIfInterrupted()

        val exactTimeStamp = it.first.toLocalExactTimeStamp()

        if (startExactTimeStamp?.let { exactTimeStamp < it } == true) return@filter false

        if (endExactTimeStamp?.let { exactTimeStamp >= it } == true) return@filter false

        true
    }.sortedBy { it.first }.map { it.second }

    // contains only generated instances
    private fun getParentInstances(
        givenStartExactTimeStamp: ExactTimeStamp.Offset?,
        givenEndExactTimeStamp: ExactTimeStamp.Offset?,
        now: ExactTimeStamp.Local,
        excludedParentTasks: Set<TaskKey>,
    ): Sequence<Instance> {
        val instanceSequences = intervalInfo.parentHierarchyIntervals
            .map { it.taskHierarchy }
            .filter { it.parentTaskKey !in excludedParentTasks }
            .map {
                it.parentTask
                    .getInstances(
                        givenStartExactTimeStamp,
                        givenEndExactTimeStamp,
                        now,
                        excludedParentTasks = excludedParentTasks + taskKey,
                    )
                    .filter { it.isVisible(now, Instance.VisibilityOptions(hack24 = true)) }
                    .mapNotNull {
                        it.getChildInstances()
                            .singleOrNull { it.taskKey == taskKey }
                            ?.takeIf { !it.exists() }
                    }
            }

        return combineInstanceSequences(instanceSequences)
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
                .filter { !it.exists() && it.isRootInstance() } // I don't know if the root part is necessary, now that group tasks are removed
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
            InterruptionChecker.throwIfInterrupted()

            val nextDateTime = it.filterNotNull()
                .minByOrNull { it.first }!!
                .first

            it.mapIndexed { index, dateTime -> index to dateTime }
                .filter { it.second?.first?.compareTo(nextDateTime) == 0 }
                .map { it.first }
        }
    }

    fun mayHaveRootInstances() = _schedules.isNotEmpty() || _existingInstances.isNotEmpty()

    private var gettingInstances = false

    fun getInstances(
        startExactTimeStamp: ExactTimeStamp.Offset?,
        endExactTimeStamp: ExactTimeStamp.Offset?,
        now: ExactTimeStamp.Local,
        onlyRoot: Boolean = false,
        filterVisible: Boolean = true,
        excludedParentTasks: Set<TaskKey> = emptySet(),
    ): Sequence<Instance> {
        check(!gettingInstances)
        gettingInstances = true

        InterruptionChecker.throwIfInterrupted()

        return if (filterVisible && !notDeleted && endData!!.deleteInstances) {
            getExistingInstances(startExactTimeStamp, endExactTimeStamp, onlyRoot).filter { it.done != null }
        } else {
            val instanceSequences = mutableListOf<Sequence<Instance>>()

            instanceSequences += getExistingInstances(startExactTimeStamp, endExactTimeStamp, onlyRoot)

            if (!onlyRoot)
                instanceSequences += getParentInstances(startExactTimeStamp, endExactTimeStamp, now, excludedParentTasks)

            instanceSequences += getScheduleInstances(startExactTimeStamp, endExactTimeStamp)

            combineInstanceSequences(instanceSequences)
        }.also {
            check(gettingInstances)
            gettingInstances = false
        }
    }

    fun getHierarchyExactTimeStamp(exactTimeStamp: ExactTimeStamp) =
        exactTimeStamp.coerceIn(startExactTimeStampOffset, endExactTimeStampOffset?.minusOne())

    fun getChildTaskHierarchies(
        exactTimeStamp: ExactTimeStamp,
        currentByHierarchy: Boolean = false,
    ): List<TaskHierarchy> {
        val taskHierarchies = childHierarchyIntervals.filter {
            val currentCheckExactTimeStamp = if (currentByHierarchy) {
                it.taskHierarchy
                    .childTask
                    .getHierarchyExactTimeStamp(exactTimeStamp)
            } else {
                exactTimeStamp
            }

            it.currentOffset(currentCheckExactTimeStamp)
                    && it.taskHierarchy.currentOffset(currentCheckExactTimeStamp)
                    && it.taskHierarchy.childTask.currentOffset(currentCheckExactTimeStamp)
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

    fun getScheduleTextMultiline(
        scheduleTextFactory: ScheduleTextFactory,
        exactTimeStamp: ExactTimeStamp,
    ): String {
        requireCurrentOffset(exactTimeStamp)

        val currentScheduleIntervals = intervalInfo.getCurrentScheduleIntervals(exactTimeStamp)
        currentScheduleIntervals.forEach { it.requireCurrentOffset(exactTimeStamp) }

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

    fun getScheduleText(
        scheduleTextFactory: ScheduleTextFactory,
        exactTimeStamp: ExactTimeStamp,
        showParent: Boolean = false,
    ): String? {
        requireCurrentOffset(exactTimeStamp)

        val currentScheduleIntervals = intervalInfo.getCurrentScheduleIntervals(exactTimeStamp)
        val parentTask = getParentTask(exactTimeStamp)

        return if (parentTask == null) {
            currentScheduleIntervals.forEach { it.requireCurrentOffset(exactTimeStamp) }

            ScheduleGroup.getGroups(currentScheduleIntervals.map { it.schedule }).joinToString(", ") {
                scheduleTextFactory.getScheduleText(it, customTimeProvider)
            }
        } else {
            check(currentScheduleIntervals.isEmpty())

            parentTask.name.takeIf { showParent }
        }
    }

    fun correctIntervalEndExactTimeStamps() = intervalInfo.intervals
        .asSequence()
        .filterIsInstance<Interval.Ended>()
        .forEach { it.correctEndExactTimeStamps() }

    fun hasOtherVisibleInstances(now: ExactTimeStamp.Local, instanceKey: InstanceKey?) = getInstances(
        null,
        null,
        now,
    ).filter { it.instanceKey != instanceKey }
        .filter { it.isVisible(now, Instance.VisibilityOptions()) }
        .any()

    final override fun toString() = super.toString() + ", name: $name, taskKey: $taskKey"

    final override fun getAssignedTo(now: ExactTimeStamp.Local): List<ProjectUser> {
        val currentScheduleIntervals = intervalInfo.getCurrentScheduleIntervals(getHierarchyExactTimeStamp(now))

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
