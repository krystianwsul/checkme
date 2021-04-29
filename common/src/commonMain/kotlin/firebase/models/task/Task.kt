package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.criteria.Assignable
import com.krystianwsul.common.criteria.QueryMatchable
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.firebase.models.interval.*
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

abstract class Task(
        private val copyScheduleHelper: CopyScheduleHelper,
        val customTimeProvider: JsonTime.CustomTimeProvider,
        private val taskRecord: TaskRecord,
        private val parentTaskDelegate: ParentTaskDelegate,
) : Current, CurrentOffset, QueryMatchable, Assignable {

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

    private val _schedules = mutableListOf<Schedule>()

    private val noScheduleOrParentsMap = taskRecord.noScheduleOrParentRecords
            .mapValues { NoScheduleOrParent(this, it.value) }
            .toMutableMap()

    val noScheduleOrParents: Collection<NoScheduleOrParent> get() = noScheduleOrParentsMap.values

    val name get() = taskRecord.name

    val schedules: List<Schedule> get() = _schedules

    final override val startExactTimeStamp = ExactTimeStamp.Local(taskRecord.startTime)

    final override val startExactTimeStampOffset by lazy {
        taskRecord.run { ExactTimeStamp.Offset.fromOffset(startTime, startTimeOffset) }
    }

    final override val endExactTimeStamp get() = endData?.exactTimeStampLocal
    final override val endExactTimeStampOffset get() = endData?.exactTimeStampOffset

    val note get() = taskRecord.note

    abstract val taskKey: TaskKey

    val id get() = taskRecord.id

    val existingInstances: Map<ScheduleKey, Instance> get() = _existingInstances

    val imageJson get() = taskRecord.image

    protected abstract val projectParentTaskHierarchies: Set<ProjectTaskHierarchy>

    val nestedParentTaskHierarchies = taskRecord.taskHierarchyRecords
            .mapValues { NestedTaskHierarchy(this, it.value, parentTaskDelegate) }
            .toMutableMap()

    val parentTaskHierarchies get() = projectParentTaskHierarchies + nestedParentTaskHierarchies.values

    val intervalsProperty = invalidatableLazyCallbacks { IntervalBuilder.build(this) }
    val intervals by intervalsProperty

    val scheduleIntervalsProperty = invalidatableLazyCallbacks {
        intervals.mapNotNull { (it.type as? Type.Schedule)?.getScheduleIntervals(it) }.flatten()
    }.apply { addTo(intervalsProperty) }
    val scheduleIntervals by scheduleIntervalsProperty

    val parentHierarchyIntervals get() = intervals.mapNotNull { (it.type as? Type.Child)?.getHierarchyInterval(it) }

    val noScheduleOrParentIntervals
        get() = intervals.mapNotNull {
            (it.type as? Type.NoSchedule)?.getNoScheduleOrParentInterval(it)
        }

    private val childHierarchyIntervalsProperty = invalidatableLazy {
        parent.getTaskHierarchiesByParentTaskKey(taskKey)
                .map { it.childTask }
                .distinct()
                .flatMap { it.parentHierarchyIntervals }
                .filter { it.taskHierarchy.parentTaskKey == taskKey }
    }
    val childHierarchyIntervals by childHierarchyIntervalsProperty

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

    private val normalizedFieldsDelegate = invalidatableLazy { listOfNotNull(name, note).map { it.normalized() } }
    final override val normalizedFields by normalizedFieldsDelegate

    val instanceHierarchyContainer by lazy { InstanceHierarchyContainer(this) }

    abstract val projectCustomTimeIdProvider: JsonTime.ProjectCustomTimeIdProvider

    fun getParentName(exactTimeStamp: ExactTimeStamp) = getParentTask(exactTimeStamp)?.name ?: project.name

    // hack24 = false -> basically, is it possible to add a subtask
    fun isVisible(now: ExactTimeStamp.Local, hack24: Boolean = false): Boolean {
        // can't add to deleted tasks
        if (!current(now)) return false

        // in general, we can add a subtask to any task that is either unscheduled, or has not done instances.  Checking
        // for that will be difficult, though.

        val topLevelTask = getTopLevelTask(now)

        // if it's in the unscheduled tasks list, we can add a subtask
        if (topLevelTask.isUnscheduled(now)) return true

        // ... and if not, we can just use getInstances() and check all of them.
        return getInstances(null, null, now).any { it.canAddSubtask(now, hack24) }
    }

    private fun getTopLevelTask(exactTimeStamp: ExactTimeStamp): Task =
            getParentTask(exactTimeStamp)?.getTopLevelTask(exactTimeStamp) ?: this

    fun getCurrentScheduleIntervals(exactTimeStamp: ExactTimeStamp): List<ScheduleInterval> {
        requireCurrentOffset(exactTimeStamp)

        return getInterval(exactTimeStamp).let {
            (it.type as? Type.Schedule)?.getScheduleIntervals(it)
                    ?.filter { it.schedule.currentOffset(exactTimeStamp) }
                    ?: listOf()
        }
    }

    fun getCurrentNoScheduleOrParent(now: ExactTimeStamp.Local) =
            getInterval(now).let {
                (it.type as? Type.NoSchedule)?.getNoScheduleOrParentInterval(it)
            }?.also {
                check(it.currentOffset(now))
                check(it.noScheduleOrParent.currentOffset(now))
            }

    fun isTopLevelTask(exactTimeStamp: ExactTimeStamp): Boolean {
        requireCurrentOffset(exactTimeStamp)

        return getParentTask(exactTimeStamp) == null
    }

    fun setEndData(
            // this is not recursive on children.  Get the whole tree beforehand.
            endData: EndData,
            taskUndoData: TaskUndoData? = null,
            recursive: Boolean = false,
    ) {
        val now = endData.exactTimeStampLocal

        requireCurrent(now)

        val scheduleIds = getCurrentScheduleIntervals(now).map {
            it.requireCurrentOffset(now)

            it.schedule.setEndExactTimeStamp(now.toOffset())

            it.schedule.id
        }.toSet()

        taskUndoData?.taskKeys?.put(taskKey, scheduleIds)

        if (!recursive) {
            getParentTaskHierarchy(now)?.let {
                it.requireCurrentOffset(now)
                it.taskHierarchy.requireCurrent(now)

                taskUndoData?.taskHierarchyKeys?.add(it.taskHierarchy.taskHierarchyKey)

                it.taskHierarchy.setEndExactTimeStamp(now)
            }
        }

        setMyEndExactTimeStamp(endData)
    }

    fun endAllCurrentTaskHierarchies(now: ExactTimeStamp.Local) = parentTaskHierarchies.filter { it.currentOffset(now) }
            .onEach { it.setEndExactTimeStamp(now) }
            .map { it.taskHierarchyKey }

    fun endAllCurrentSchedules(now: ExactTimeStamp.Local) = schedules.filter { it.currentOffset(now) }
            .onEach { it.setEndExactTimeStamp(now.toOffset()) }
            .map { it.id }

    fun endAllCurrentNoScheduleOrParents(now: ExactTimeStamp.Local) = noScheduleOrParents.filter { it.currentOffset(now) }
            .onEach { it.setEndExactTimeStamp(now.toOffset()) }
            .map { it.id }

    fun getNestedTaskHierarchy(taskHierarchyId: String) = nestedParentTaskHierarchies.getValue(taskHierarchyId)

    private fun getParentTaskHierarchy(exactTimeStamp: ExactTimeStamp): HierarchyInterval? {
        requireCurrentOffset(exactTimeStamp)

        return getInterval(exactTimeStamp).let { (it.type as? Type.Child)?.getHierarchyInterval(it) }
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp.Local) {
        requireNotCurrent(now)

        setMyEndExactTimeStamp(null)
    }

    fun getParentTask(exactTimeStamp: ExactTimeStamp): Task? {
        requireNotDeletedOffset(exactTimeStamp)

        return getParentTaskHierarchy(exactTimeStamp)?.run {
            requireNotDeletedOffset(exactTimeStamp)
            taskHierarchy.requireNotDeletedOffset(exactTimeStamp)

            taskHierarchy.parentTask.apply { requireNotDeletedOffset(exactTimeStamp) }
        }
    }

    private fun getExistingInstances(
            startExactTimeStamp: ExactTimeStamp.Offset?,
            endExactTimeStamp: ExactTimeStamp.Offset?,
            bySchedule: Boolean,
            onlyRoot: Boolean,
    ): Sequence<Instance> {
        return _existingInstances.values
                .asSequence()
                .run { if (onlyRoot) filter { it.isRootInstance() } else this }
                .map { it.getSequenceDate(bySchedule) to it }
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
            bySchedule: Boolean,
    ): Sequence<Instance> {
        val instanceSequences = parentHierarchyIntervals.map {
            it.taskHierarchy
                    .parentTask
                    .getInstances(givenStartExactTimeStamp, givenEndExactTimeStamp, now, bySchedule)
                    .filter { it.isVisible(now, Instance.VisibilityOptions(hack24 = true)) }
                    .mapNotNull {
                        it.getChildInstances()
                                .singleOrNull { it.taskKey == taskKey }
                                ?.takeIf { !it.exists() }
                    }
        }

        return combineInstanceSequences(instanceSequences, bySchedule)
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

        val scheduleResults = scheduleIntervals.map { scheduleInterval ->
            scheduleInterval.getDateTimesInRange(
                    startExactTimeStamp,
                    endExactTimeStamp,
                    originalDateTime,
                    checkOldestVisible
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

    fun getInstances(
            startExactTimeStamp: ExactTimeStamp.Offset?,
            endExactTimeStamp: ExactTimeStamp.Offset?,
            now: ExactTimeStamp.Local,
            bySchedule: Boolean = false,
            onlyRoot: Boolean = false,
            filterVisible: Boolean = true,
    ): Sequence<Instance> {
        InterruptionChecker.throwIfInterrupted()

        return if (filterVisible && !notDeleted(now) && endData!!.deleteInstances) {
            getExistingInstances(
                    startExactTimeStamp,
                    endExactTimeStamp,
                    bySchedule,
                    onlyRoot
            ).filter { it.done != null }
        } else {
            val instanceSequences = mutableListOf<Sequence<Instance>>()

            instanceSequences += getExistingInstances(
                    startExactTimeStamp,
                    endExactTimeStamp,
                    bySchedule,
                    onlyRoot,
            )

            if (!onlyRoot) {
                instanceSequences += getParentInstances(
                        startExactTimeStamp,
                        endExactTimeStamp,
                        now,
                        bySchedule,
                )
            }

            instanceSequences += getScheduleInstances(startExactTimeStamp, endExactTimeStamp)

            return combineInstanceSequences(instanceSequences, bySchedule)
        }
    }

    private data class ScheduleDiffKey(val scheduleData: ScheduleData, val assignedTo: Set<UserKey>)

    fun updateSchedules(
            ownerKey: UserKey,
            shownFactory: Instance.ShownFactory,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            now: ExactTimeStamp.Local,
            assignedTo: Set<UserKey>,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
    ) {
        val removeSchedules = mutableListOf<Schedule>()
        val addScheduleDatas = scheduleDatas.map { ScheduleDiffKey(it.first, assignedTo) to it }.toMutableList()

        val oldSchedules = getCurrentScheduleIntervals(now).map { it.schedule }

        val oldScheduleDatas = ScheduleGroup.getGroups(oldSchedules).map {
            ScheduleDiffKey(it.scheduleData, it.assignedTo) to it.schedules
        }

        for ((key, value) in oldScheduleDatas) {
            val existing = addScheduleDatas.singleOrNull { it.first == key }

            if (existing != null)
                addScheduleDatas.remove(existing)
            else
                removeSchedules.addAll(value)
        }

        /*
            requirements for mock:
                there was one old schedule, it was single and mocked, and it's getting replaced
                by another single schedule
         */

        val singleRemoveSchedule = removeSchedules.singleOrNull() as? SingleSchedule

        val singleAddSchedulePair = addScheduleDatas.singleOrNull()?.takeIf {
            it.first.scheduleData is ScheduleData.Single
        }

        if (singleRemoveSchedule != null && singleAddSchedulePair != null) {
            if (assignedTo.isNotEmpty()) singleRemoveSchedule.setAssignedTo(assignedTo)

            singleRemoveSchedule.getInstance(this).setInstanceDateTime(
                    shownFactory,
                    ownerKey,
                    singleAddSchedulePair.second.run { DateTime((first as ScheduleData.Single).date, second) },
                    customTimeMigrationHelper,
                    now,
            )
        } else {
            removeSchedules.forEach { it.setEndExactTimeStamp(now.toOffset()) }

            addSchedules(ownerKey, addScheduleDatas.map { it.second }, now, assignedTo, customTimeMigrationHelper)
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

    fun setImage(deviceDbInfo: DeviceDbInfo, imageState: ImageState?) {
        taskRecord.image = when (imageState) {
            null -> null
            is ImageState.Remote -> TaskJson.Image(imageState.uuid)
            is ImageState.Local -> TaskJson.Image(imageState.uuid, deviceDbInfo.uuid)
            is ImageState.Uploading -> throw IllegalArgumentException()
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

    protected fun setMyEndExactTimeStamp(endData: EndData?) {
        taskRecord.endData = endData?.let {
            TaskJson.EndData(it.exactTimeStampLocal.long, it.exactTimeStampLocal.offset, it.deleteInstances)
        }

        endDataProperty.invalidate()
    }

    abstract fun createChildTask(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double? = null,
    ): Task

    protected abstract fun deleteFromParent()

    fun delete() {
        existingInstances.values
                .toMutableList()
                .forEach { it.delete() }

        schedules.toMutableList().forEach { it.delete() }

        deleteFromParent()
        taskRecord.delete()
    }

    fun setName(name: String, note: String?) {
        check(name.isNotEmpty())

        taskRecord.name = name
        taskRecord.note = note

        normalizedFieldsDelegate.invalidate()
    }

    private fun addSchedules(
            ownerKey: UserKey,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            now: ExactTimeStamp.Local,
            assignedTo: Set<UserKey>,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
    ) = createSchedules(ownerKey, now, scheduleDatas, assignedTo, customTimeMigrationHelper)

    abstract fun addChild(childTask: Task, now: ExactTimeStamp.Local): TaskHierarchyKey

    fun deleteSchedule(schedule: Schedule) {
        check(_schedules.contains(schedule))

        _schedules.remove(schedule)
        invalidateIntervals()
    }

    fun deleteNoScheduleOrParent(noScheduleOrParent: NoScheduleOrParent) {
        check(noScheduleOrParentsMap.containsKey(noScheduleOrParent.id))

        noScheduleOrParentsMap.remove(noScheduleOrParent.id)
        invalidateIntervals()
    }

    fun createRemoteInstanceRecord(instance: Instance): InstanceRecord {
        check(generatedInstances.containsKey(instance.instanceKey))

        generatedInstances.remove(instance.instanceKey)

        val instanceRecord = taskRecord.newInstanceRecord(InstanceJson(), instance.scheduleKey)

        _existingInstances[instance.scheduleKey] = instance

        return instanceRecord
    }

    fun deleteInstance(instance: Instance) {
        val scheduleKey = instance.scheduleKey

        check(_existingInstances.containsKey(scheduleKey))
        check(instance == _existingInstances[scheduleKey])

        _existingInstances.remove(scheduleKey)
    }

    private fun getExistingInstanceIfPresent(scheduleKey: ScheduleKey) = _existingInstances[scheduleKey]

    fun getInstance(scheduleDateTime: DateTime): Instance {
        val scheduleKey = ScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        val existingInstance = getExistingInstanceIfPresent(scheduleKey)

        return (existingInstance ?: generateInstance(scheduleDateTime))
    }

    protected abstract fun getDateTime(scheduleKey: ScheduleKey): DateTime

    fun getInstance(scheduleKey: ScheduleKey) = getInstance(getDateTime(scheduleKey))

    protected abstract fun getOrCopyTime(
            ownerKey: UserKey,
            dayOfWeek: DayOfWeek,
            time: Time,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
            now: ExactTimeStamp.Local,
    ): Time

    fun createSchedules(
            ownerKey: UserKey,
            now: ExactTimeStamp.Local,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            assignedTo: Set<UserKey>,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
    ) {
        val assignedToKeys = assignedTo.map { it.key }.toSet()

        for ((scheduleData, time) in scheduleDatas) {
            when (scheduleData) {
                is ScheduleData.Single -> {
                    val date = scheduleData.date

                    val copiedTime = getOrCopyTime(
                            ownerKey,
                            date.dayOfWeek,
                            time,
                            customTimeMigrationHelper,
                            now,
                    )

                    val singleScheduleRecord = taskRecord.newSingleScheduleRecord(
                            copyScheduleHelper.newSingle(
                                    now.long,
                                    now.offset,
                                    null,
                                    null,
                                    date.year,
                                    date.month,
                                    date.day,
                                    copiedTime,
                                    assignedToKeys,
                            )
                    )

                    _schedules += SingleSchedule(this, singleScheduleRecord)
                }
                is ScheduleData.Weekly -> {
                    for (dayOfWeek in scheduleData.daysOfWeek) {
                        val copiedTime = getOrCopyTime(
                                ownerKey,
                                dayOfWeek,
                                time,
                                customTimeMigrationHelper,
                                now,
                        )

                        val weeklyScheduleRecord = taskRecord.newWeeklyScheduleRecord(
                                copyScheduleHelper.newWeekly(
                                        now.long,
                                        now.offset,
                                        null,
                                        null,
                                        dayOfWeek.ordinal,
                                        copiedTime,
                                        scheduleData.from?.toJson(),
                                        scheduleData.until?.toJson(),
                                        scheduleData.interval,
                                        assignedToKeys,
                                )
                        )

                        _schedules += WeeklySchedule(this, weeklyScheduleRecord)
                    }
                }
                is ScheduleData.MonthlyDay -> {
                    val (dayOfMonth, beginningOfMonth, _) = scheduleData

                    val today = Date.today()

                    val dayOfWeek = getDateInMonth(
                            today.year,
                            today.month,
                            scheduleData.dayOfMonth,
                            scheduleData.beginningOfMonth,
                    ).dayOfWeek

                    val copiedTime = getOrCopyTime(ownerKey, dayOfWeek, time, customTimeMigrationHelper, now)

                    val monthlyDayScheduleRecord = taskRecord.newMonthlyDayScheduleRecord(
                            copyScheduleHelper.newMonthlyDay(
                                    now.long,
                                    now.offset,
                                    null,
                                    null,
                                    dayOfMonth,
                                    beginningOfMonth,
                                    copiedTime,
                                    scheduleData.from?.toJson(),
                                    scheduleData.until?.toJson(),
                                    assignedToKeys,
                            )
                    )

                    _schedules += MonthlyDaySchedule(this, monthlyDayScheduleRecord)
                }
                is ScheduleData.MonthlyWeek -> {
                    val (weekOfMonth, dayOfWeek, beginningOfMonth) = scheduleData
                    val copiedTime = getOrCopyTime(ownerKey, dayOfWeek, time, customTimeMigrationHelper, now)

                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(
                            copyScheduleHelper.newMonthlyWeek(
                                    now.long,
                                    now.offset,
                                    null,
                                    null,
                                    weekOfMonth,
                                    dayOfWeek.ordinal,
                                    beginningOfMonth,
                                    copiedTime,
                                    scheduleData.from?.toJson(),
                                    scheduleData.until?.toJson(),
                                    assignedToKeys,
                            )
                    )

                    _schedules += MonthlyWeekSchedule(this, monthlyWeekScheduleRecord)
                }
                is ScheduleData.Yearly -> {
                    val copiedTime = getOrCopyTime(
                            ownerKey,
                            Date(Date.today().year, scheduleData.month, scheduleData.day).dayOfWeek,
                            time,
                            customTimeMigrationHelper,
                            now,
                    )

                    val yearlyScheduleRecord = taskRecord.newYearlyScheduleRecord(
                            copyScheduleHelper.newYearly(
                                    now.long,
                                    now.offset,
                                    null,
                                    null,
                                    scheduleData.month,
                                    scheduleData.day,
                                    copiedTime,
                                    scheduleData.from?.toJson(),
                                    scheduleData.until?.toJson(),
                                    assignedToKeys,
                            )
                    )

                    _schedules += YearlySchedule(this, yearlyScheduleRecord)
                }
            }
        }

        intervalsProperty.invalidate()
    }

    fun copySchedules(
            deviceDbInfo: DeviceDbInfo,
            now: ExactTimeStamp.Local,
            schedules: List<Schedule>,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
    ) {
        for (schedule in schedules) {
            val today = Date.today()

            val dayOfWeek = when (schedule) {
                is SingleSchedule -> schedule.date.dayOfWeek
                is WeeklySchedule -> schedule.dayOfWeek
                is MonthlyDaySchedule -> schedule.getDateInMonth(today.year, today.month).dayOfWeek
                is MonthlyWeekSchedule -> schedule.dayOfWeek
                is YearlySchedule -> schedule.getDateInYear(today.year).dayOfWeek
                else -> throw UnsupportedOperationException()
            }

            val copiedTime = getOrCopyTime(
                    deviceDbInfo.key,
                    dayOfWeek,
                    schedule.time,
                    customTimeMigrationHelper,
                    now,
            )

            val assignedTo = schedule.takeIf { it.topLevelTask.project == project } // todo task convert
                    ?.assignedTo
                    .orEmpty()
                    .map { it.key }
                    .toSet()

            when (schedule) {
                is SingleSchedule -> {
                    val date = schedule.date

                    val singleScheduleRecord = taskRecord.newSingleScheduleRecord(
                            copyScheduleHelper.newSingle(
                                    now.long,
                                    now.offset,
                                    schedule.endExactTimeStamp?.long,
                                    schedule.endExactTimeStamp?.offset,
                                    date.year,
                                    date.month,
                                    date.day,
                                    copiedTime,
                                    assignedTo,
                            )
                    )

                    _schedules += SingleSchedule(this, singleScheduleRecord)
                }
                is WeeklySchedule -> {
                    val weeklyScheduleRecord = taskRecord.newWeeklyScheduleRecord(
                            copyScheduleHelper.newWeekly(
                                    now.long,
                                    now.offset,
                                    schedule.endExactTimeStamp?.long,
                                    schedule.endExactTimeStamp?.offset,
                                    schedule.dayOfWeek.ordinal,
                                    copiedTime,
                                    schedule.from?.toJson(),
                                    schedule.until?.toJson(),
                                    schedule.interval,
                                    assignedTo,
                            )
                    )

                    _schedules += WeeklySchedule(this, weeklyScheduleRecord)
                }
                is MonthlyDaySchedule -> {
                    val monthlyDayScheduleRecord = taskRecord.newMonthlyDayScheduleRecord(
                            copyScheduleHelper.newMonthlyDay(
                                    now.long,
                                    now.offset,
                                    schedule.endExactTimeStamp?.long,
                                    schedule.endExactTimeStamp?.offset,
                                    schedule.dayOfMonth,
                                    schedule.beginningOfMonth,
                                    copiedTime,
                                    schedule.from?.toJson(),
                                    schedule.until?.toJson(),
                                    assignedTo,
                            )
                    )

                    _schedules += MonthlyDaySchedule(this, monthlyDayScheduleRecord)
                }
                is MonthlyWeekSchedule -> {
                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(
                            copyScheduleHelper.newMonthlyWeek(
                                    now.long,
                                    now.offset,
                                    schedule.endExactTimeStamp?.long,
                                    schedule.endExactTimeStamp?.offset,
                                    schedule.weekOfMonth,
                                    schedule.dayOfWeek.ordinal,
                                    schedule.beginningOfMonth,
                                    copiedTime,
                                    schedule.from?.toJson(),
                                    schedule.until?.toJson(),
                                    assignedTo,
                            )
                    )

                    _schedules += MonthlyWeekSchedule(this, monthlyWeekScheduleRecord)
                }
                is YearlySchedule -> {
                    val yearlyScheduleRecord = taskRecord.newYearlyScheduleRecord(
                            copyScheduleHelper.newYearly(
                                    now.long,
                                    now.offset,
                                    schedule.endExactTimeStamp?.long,
                                    schedule.endExactTimeStamp?.offset,
                                    schedule.month,
                                    schedule.day,
                                    copiedTime,
                                    schedule.from?.toJson(),
                                    schedule.until?.toJson(),
                                    assignedTo,
                            )
                    )

                    _schedules += YearlySchedule(this, yearlyScheduleRecord)
                }
            }
        }

        intervalsProperty.invalidate()
    }

    abstract fun invalidateProjectParentTaskHierarchies()

    fun invalidateChildTaskHierarchies() = childHierarchyIntervalsProperty.invalidate()

    fun invalidateIntervals() = intervalsProperty.invalidate()

    abstract fun updateProject(
            projectUpdater: ProjectUpdater,
            now: ExactTimeStamp.Local,
            projectId: ProjectKey<*>,
    ): Task

    fun getScheduleTextMultiline(
            scheduleTextFactory: ScheduleTextFactory,
            exactTimeStamp: ExactTimeStamp,
    ): String {
        requireCurrentOffset(exactTimeStamp)

        val currentScheduleIntervals = getCurrentScheduleIntervals(exactTimeStamp)
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
                ?: throw InstanceKeyNotFoundException("instanceKey: $instanceKey; \nmap keys: " + generatedInstances.keys.joinToString(", \n") + "; \n log: " + generatedInstancesLog.joinToString(", \n"))
    }

    fun getScheduleText(
            scheduleTextFactory: ScheduleTextFactory,
            exactTimeStamp: ExactTimeStamp,
            showParent: Boolean = false,
    ): String? {
        requireCurrentOffset(exactTimeStamp)

        val currentScheduleIntervals = getCurrentScheduleIntervals(exactTimeStamp)
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

    fun isUnscheduled(now: ExactTimeStamp.Local) = getInterval(now).type is Type.NoSchedule

    private fun getInterval(exactTimeStamp: ExactTimeStamp): Interval {
        val intervals = intervals

        try {
            return intervals.single {
                it.containsExactTimeStamp(exactTimeStamp)
            }
        } catch (throwable: Throwable) {
            throw IntervalException(
                    "error getting interval for task $name. exactTimeStamp: $exactTimeStamp, intervals:\n"
                            + intervals.joinToString("\n") {
                        "${it.startExactTimeStampOffset} - ${it.endExactTimeStampOffset}"
                    },
                    throwable
            )
        }
    }

    private class IntervalException(message: String, cause: Throwable) : Exception(message, cause)

    fun setNoScheduleOrParent(now: ExactTimeStamp.Local) {
        val noScheduleOrParentRecord =
                taskRecord.newNoScheduleOrParentRecord(NoScheduleOrParentJson(now.long, now.offset))
        check(!noScheduleOrParentsMap.containsKey(noScheduleOrParentRecord.id))

        noScheduleOrParentsMap[noScheduleOrParentRecord.id] =
                NoScheduleOrParent(this, noScheduleOrParentRecord)

        invalidateIntervals()
    }

    fun correctIntervalEndExactTimeStamps() = intervals.asSequence()
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
        val currentScheduleIntervals = getCurrentScheduleIntervals(getHierarchyExactTimeStamp(now))

        return if (currentScheduleIntervals.isEmpty()) {
            listOf()
        } else {
            currentScheduleIntervals.map { it.schedule.assignedTo }
                    .distinct()
                    .single()
                    .let(project::getAssignedTo) // todo task before create
                    .map { it.value }
        }
    }

    fun deleteNestedTaskHierarchy(nestedTaskHierarchy: NestedTaskHierarchy) {
        check(nestedParentTaskHierarchies.containsKey(nestedTaskHierarchy.childTaskId))

        nestedParentTaskHierarchies.remove(nestedTaskHierarchy.childTaskId)

        nestedTaskHierarchy.invalidateTasks()
    }

    fun createParentNestedTaskHierarchy(parentTask: Task, now: ExactTimeStamp.Local): TaskHierarchyKey.Nested {
        val taskHierarchyJson = NestedTaskHierarchyJson(parentTask.id, now.long, now.offset)

        return createParentNestedTaskHierarchy(taskHierarchyJson).taskHierarchyKey
    }

    private fun createParentNestedTaskHierarchy(
            nestedTaskHierarchyJson: NestedTaskHierarchyJson,
    ): NestedTaskHierarchy {
        val taskHierarchyRecord = taskRecord.newTaskHierarchyRecord(nestedTaskHierarchyJson)
        val taskHierarchy = NestedTaskHierarchy(this, taskHierarchyRecord, parentTaskDelegate)

        nestedParentTaskHierarchies[taskHierarchy.id] = taskHierarchy

        taskHierarchy.invalidateTasks()

        return taskHierarchy
    }

    fun <V : TaskHierarchy> copyParentNestedTaskHierarchy(
            now: ExactTimeStamp.Local,
            startTaskHierarchy: V,
            parentTaskId: String,
    ) {
        check(parentTaskId.isNotEmpty())

        val taskHierarchyJson = NestedTaskHierarchyJson(
                parentTaskId,
                now.long,
                now.offset,
                startTaskHierarchy.endExactTimeStampOffset?.long,
                startTaskHierarchy.endExactTimeStampOffset?.offset,
        )

        createParentNestedTaskHierarchy(taskHierarchyJson)
    }

    interface ScheduleTextFactory {

        fun getScheduleText(scheduleGroup: ScheduleGroup, customTimeProvider: JsonTime.CustomTimeProvider): String
    }

    interface ProjectUpdater {

        fun convert(now: ExactTimeStamp.Local, startingTask: Task, projectId: ProjectKey<*>): Task
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

        fun getInstance(instanceKey: InstanceKey) = getTask(instanceKey.taskKey).getInstance(instanceKey.scheduleKey)
    }
}
