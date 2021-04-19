package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.criteria.Assignable
import com.krystianwsul.common.criteria.QueryMatchable
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.interval.*
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

class Task<T : ProjectType>(val project: Project<T>, private val taskRecord: TaskRecord<T>) :
        Current, CurrentOffset, QueryMatchable, Assignable {

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

    private val _schedules = mutableListOf<Schedule<T>>()

    private val noScheduleOrParentsMap = taskRecord.noScheduleOrParentRecords
            .mapValues { NoScheduleOrParent(this, it.value) }
            .toMutableMap()

    val noScheduleOrParents: Collection<NoScheduleOrParent<T>> get() = noScheduleOrParentsMap.values

    val name get() = taskRecord.name

    val schedules: List<Schedule<T>> get() = _schedules

    override val startExactTimeStamp = ExactTimeStamp.Local(taskRecord.startTime)

    override val startExactTimeStampOffset by lazy {
        taskRecord.run { ExactTimeStamp.Offset.fromOffset(startTime, startTimeOffset) }
    }

    override val endExactTimeStamp get() = endData?.exactTimeStampLocal
    override val endExactTimeStampOffset get() = endData?.exactTimeStampOffset

    val note get() = taskRecord.note

    val taskKey get() = TaskKey(project.projectKey, taskRecord.id)

    val id get() = taskRecord.id

    val existingInstances: Map<ScheduleKey, Instance<T>> get() = _existingInstances

    val imageJson get() = taskRecord.image

    private val parentProjectTaskHierarchiesProperty = invalidatableLazy { // todo taskhierarchies read
        project.getTaskHierarchiesByChildTaskKey(taskKey)
    }

    private val projectParentTaskHierarchies by parentProjectTaskHierarchiesProperty // todo taskhierarchies read

    private val nestedProjectTaskHierarchies = taskRecord.taskHierarchyRecords
            .mapValues { NestedTaskHierarchy(this, it.value) }
            .toMutableMap()

    val parentTaskHierarchies get() = projectParentTaskHierarchies + nestedProjectTaskHierarchies.values // todo taskhierarchies read

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
        project.getTaskHierarchiesByParentTaskKey(taskKey)
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
    override val normalizedFields by normalizedFieldsDelegate

    val instanceHierarchyContainer by lazy { InstanceHierarchyContainer(this) }

    fun getParentName(exactTimeStamp: ExactTimeStamp) = getParentTask(exactTimeStamp)?.name ?: project.name

    // hack24 = false -> basically, is it possible to add a subtask
    fun isVisible(now: ExactTimeStamp.Local, hack24: Boolean = false): Boolean {
        // can't add to deleted tasks
        if (!current(now)) return false

        // in general, we can add a subtask to any task that is either unscheduled, or has not done instances.  Checking
        // for that will be difficult, though.

        val rootTask = getRootTask(now)

        // if it's in the unscheduled tasks list, we can add a subtask
        if (rootTask.isUnscheduled(now)) return true

        // ... and if not, we can just use getInstances() and check all of them.
        return getInstances(null, null, now).any { it.canAddSubtask(now, hack24) }
    }

    private fun getRootTask(exactTimeStamp: ExactTimeStamp): Task<T> =
            getParentTask(exactTimeStamp)?.getRootTask(exactTimeStamp) ?: this

    fun getCurrentScheduleIntervals(exactTimeStamp: ExactTimeStamp): List<ScheduleInterval<T>> {
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

    fun isRootTask(exactTimeStamp: ExactTimeStamp): Boolean {
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

        taskUndoData?.taskKeys?.add(taskKey)

        getCurrentScheduleIntervals(now).forEach {
            it.requireCurrentOffset(now)

            taskUndoData?.scheduleIds?.add(it.schedule.scheduleId)

            it.schedule.setEndExactTimeStamp(now.toOffset())
        }

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
            .map { it.scheduleId }

    fun endAllCurrentNoScheduleOrParents(now: ExactTimeStamp.Local) = noScheduleOrParents.filter { it.currentOffset(now) }
            .onEach { it.setEndExactTimeStamp(now.toOffset()) }
            .map { it.id }

    private fun getParentTaskHierarchy(exactTimeStamp: ExactTimeStamp): HierarchyInterval<T>? {
        requireCurrentOffset(exactTimeStamp)

        return getInterval(exactTimeStamp).let { (it.type as? Type.Child)?.getHierarchyInterval(it) }
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp.Local) {
        requireNotCurrent(now)

        setMyEndExactTimeStamp(null)
    }

    fun getParentTask(exactTimeStamp: ExactTimeStamp): Task<T>? {
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
    ): Sequence<Instance<T>> {
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
    ): Sequence<Instance<out T>> {
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
    ): Sequence<Instance<out T>> {
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
    ): Sequence<List<Pair<DateTime, ScheduleInterval<T>>>> {
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
    ): Sequence<Instance<out T>> {
        InterruptionChecker.throwIfInterrupted()

        return if (filterVisible && !notDeleted(now) && endData!!.deleteInstances) {
            getExistingInstances(
                    startExactTimeStamp,
                    endExactTimeStamp,
                    bySchedule,
                    onlyRoot
            ).filter { it.done != null }
        } else {
            val instanceSequences = mutableListOf<Sequence<Instance<out T>>>()

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
    ) {
        val removeSchedules = mutableListOf<Schedule<T>>()
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
            check(singleRemoveSchedule.scheduleId == singleRemoveSchedule.scheduleId)

            if (assignedTo.isNotEmpty()) singleRemoveSchedule.setAssignedTo(assignedTo)

            singleRemoveSchedule.getInstance(this).setInstanceDateTime(
                    shownFactory,
                    ownerKey,
                    singleAddSchedulePair.second.run { DateTime((first as ScheduleData.Single).date, second) },
            )
        } else {
            removeSchedules.forEach { it.setEndExactTimeStamp(now.toOffset()) }
            addSchedules(ownerKey, addScheduleDatas.map { it.second }, now, assignedTo)
        }
    }

    fun getHierarchyExactTimeStamp(exactTimeStamp: ExactTimeStamp) =
            exactTimeStamp.coerceIn(startExactTimeStampOffset, endExactTimeStampOffset?.minusOne())

    fun getChildTaskHierarchies(
            exactTimeStamp: ExactTimeStamp,
            currentByHierarchy: Boolean = false,
    ): List<TaskHierarchy<T>> {
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

    private fun setMyEndExactTimeStamp(endData: EndData?) {
        taskRecord.endData = endData?.let {
            TaskJson.EndData(it.exactTimeStampLocal.long, it.exactTimeStampLocal.offset, it.deleteInstances)
        }

        endDataProperty.invalidate()
    }

    fun createChildTask(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double? = null,
    ) = project.createChildTask(this, now, name, note, image, ordinal)

    fun delete() {
        existingInstances.values
                .toMutableList()
                .forEach { it.delete() }

        schedules.toMutableList().forEach { it.delete() }

        project.deleteTask(this)
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
    ) = createSchedules(ownerKey, now, scheduleDatas, assignedTo)

    fun addChild(childTask: Task<*>, now: ExactTimeStamp.Local): TaskHierarchyKey {
        @Suppress("UNCHECKED_CAST")
        return project.createTaskHierarchy(this, childTask as Task<T>, now)
    }

    fun deleteSchedule(schedule: Schedule<T>) {
        check(_schedules.contains(schedule))

        _schedules.remove(schedule)
        invalidateIntervals()
    }

    fun deleteNoScheduleOrParent(noScheduleOrParent: NoScheduleOrParent<T>) {
        check(noScheduleOrParentsMap.containsKey(noScheduleOrParent.id))

        noScheduleOrParentsMap.remove(noScheduleOrParent.id)
        invalidateIntervals()
    }

    fun createRemoteInstanceRecord(instance: Instance<T>): InstanceRecord<T> {
        check(generatedInstances.containsKey(instance.instanceKey))

        generatedInstances.remove(instance.instanceKey)

        val instanceRecord = taskRecord.newInstanceRecord(InstanceJson(), instance.scheduleKey)

        _existingInstances[instance.scheduleKey] = instance

        return instanceRecord
    }

    fun deleteInstance(instance: Instance<T>) {
        val scheduleKey = instance.scheduleKey

        check(_existingInstances.containsKey(scheduleKey))
        check(instance == _existingInstances[scheduleKey])

        _existingInstances.remove(scheduleKey)
    }

    private fun getExistingInstanceIfPresent(scheduleKey: ScheduleKey) = _existingInstances[scheduleKey]

    fun getInstance(scheduleDateTime: DateTime): Instance<T> {
        val scheduleKey = ScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        val existingInstance = getExistingInstanceIfPresent(scheduleKey)

        return (existingInstance ?: generateInstance(scheduleDateTime))
    }

    fun getInstance(scheduleKey: ScheduleKey) = getInstance(project.getDateTime(scheduleKey))

    fun createSchedules(
            ownerKey: UserKey,
            now: ExactTimeStamp.Local,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            assignedTo: Set<UserKey>,
    ) {
        val assignedToKeys = assignedTo.map { it.key }.toSet()

        for ((scheduleData, time) in scheduleDatas) {
            when (scheduleData) {
                is ScheduleData.Single -> {
                    val date = scheduleData.date
                    val copiedTime = project.getOrCopyTime(ownerKey, date.dayOfWeek, time)

                    val singleScheduleRecord = taskRecord.newSingleScheduleRecord(
                            project.copyScheduleHelper.newSingle(
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
                        val copiedTime = project.getOrCopyTime(ownerKey, dayOfWeek, time)

                        val weeklyScheduleRecord = taskRecord.newWeeklyScheduleRecord(
                                project.copyScheduleHelper.newWeekly(
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

                    val copiedTime = project.getOrCopyTime(ownerKey, dayOfWeek, time)

                    val monthlyDayScheduleRecord = taskRecord.newMonthlyDayScheduleRecord(
                            project.copyScheduleHelper.newMonthlyDay(
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
                    val copiedTime = project.getOrCopyTime(ownerKey, dayOfWeek, time)

                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(
                            project.copyScheduleHelper.newMonthlyWeek(
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
                    val copiedTime = project.getOrCopyTime(
                            ownerKey,
                            Date(Date.today().year, scheduleData.month, scheduleData.day).dayOfWeek,
                            time,
                    )

                    val yearlyScheduleRecord = taskRecord.newYearlyScheduleRecord(
                            project.copyScheduleHelper.newYearly(
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

    fun copySchedules(deviceDbInfo: DeviceDbInfo, now: ExactTimeStamp.Local, schedules: List<Schedule<*>>) {
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

            val copiedTime = project.getOrCopyTime(deviceDbInfo.key, dayOfWeek, schedule.time)

            val assignedTo = schedule.takeIf { it.rootTask.project == project }
                    ?.assignedTo
                    .orEmpty()
                    .map { it.key }
                    .toSet()

            when (schedule) {
                is SingleSchedule<*> -> {
                    val date = schedule.date

                    val singleScheduleRecord = taskRecord.newSingleScheduleRecord(
                            project.copyScheduleHelper.newSingle(
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
                is WeeklySchedule<*> -> {
                    val weeklyScheduleRecord = taskRecord.newWeeklyScheduleRecord(
                            project.copyScheduleHelper.newWeekly(
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
                is MonthlyDaySchedule<*> -> {
                    val monthlyDayScheduleRecord = taskRecord.newMonthlyDayScheduleRecord(
                            project.copyScheduleHelper.newMonthlyDay(
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
                is MonthlyWeekSchedule<*> -> {
                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(
                            project.copyScheduleHelper.newMonthlyWeek(
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
                is YearlySchedule<*> -> {
                    val yearlyScheduleRecord = taskRecord.newYearlyScheduleRecord(
                            project.copyScheduleHelper.newYearly(
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

    fun invalidateParentTaskHierarchies() {
        parentProjectTaskHierarchiesProperty.invalidate()
        invalidateIntervals()
    }

    fun invalidateChildTaskHierarchies() = childHierarchyIntervalsProperty.invalidate()

    fun invalidateIntervals() = intervalsProperty.invalidate()

    fun updateProject(
            projectUpdater: ProjectUpdater,
            now: ExactTimeStamp.Local,
            projectId: ProjectKey<*>,
    ): Task<*> {
        return if (projectId == project.projectKey)
            this
        else
            projectUpdater.convert(now, this, projectId)
    }

    fun getScheduleTextMultiline(
            scheduleTextFactory: ScheduleTextFactory,
            exactTimeStamp: ExactTimeStamp,
    ): String {
        requireCurrentOffset(exactTimeStamp)

        val currentScheduleIntervals = getCurrentScheduleIntervals(exactTimeStamp)
        currentScheduleIntervals.forEach { it.requireCurrentOffset(exactTimeStamp) }

        return ScheduleGroup.getGroups(currentScheduleIntervals.map { it.schedule }).joinToString("\n") {
            scheduleTextFactory.getScheduleText(it, project)
        }
    }

    private val generatedInstances = mutableMapOf<InstanceKey, Instance<T>>()
    private val generatedInstancesLog = mutableListOf<String>().synchronized()

    private fun generateInstance(scheduleDateTime: DateTime): Instance<T> {
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
                scheduleTextFactory.getScheduleText(it, project)
            }
        } else {
            check(currentScheduleIntervals.isEmpty())

            parentTask.name.takeIf { showParent }
        }
    }

    fun isUnscheduled(now: ExactTimeStamp.Local) = getInterval(now).type is Type.NoSchedule

    private fun getInterval(exactTimeStamp: ExactTimeStamp): Interval<T> {
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
            .filterIsInstance<Interval.Ended<T>>()
            .forEach { it.correctEndExactTimeStamps() }

    fun hasOtherVisibleInstances(now: ExactTimeStamp.Local, instanceKey: InstanceKey?) = getInstances(
            null,
            null,
            now,
    ).filter { it.instanceKey != instanceKey }
            .filter { it.isVisible(now, Instance.VisibilityOptions()) }
            .any()

    override fun toString() = super.toString() + ", name: $name, taskKey: $taskKey"

    fun fixOffsets() {
        if (taskRecord.startTimeOffset == null) taskRecord.startTimeOffset = startExactTimeStamp.offset

        endData?.let {
            if (taskRecord.endData!!.offset == null) setMyEndExactTimeStamp(it)
        }

        scheduleIntervals.forEach { it.schedule.fixOffsets() }
        parentHierarchyIntervals.forEach { it.taskHierarchy.fixOffsets() }
        noScheduleOrParentIntervals.forEach { it.noScheduleOrParent.fixOffsets() }
        existingInstances.values.forEach { it.fixOffsets() }
    }

    override fun getAssignedTo(now: ExactTimeStamp.Local): List<ProjectUser> {
        val currentScheduleIntervals = getCurrentScheduleIntervals(getHierarchyExactTimeStamp(now))

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

    interface ScheduleTextFactory {

        fun getScheduleText(scheduleGroup: ScheduleGroup<*>, project: Project<*>): String
    }

    interface ProjectUpdater {

        fun <T : ProjectType> convert(
                now: ExactTimeStamp.Local,
                startingTask: Task<T>,
                projectId: ProjectKey<*>,
        ): Task<*>
    }

    data class EndData(
            val exactTimeStampLocal: ExactTimeStamp.Local,
            val deleteInstances: Boolean,
            val exactTimeStampOffset: ExactTimeStamp.Offset = exactTimeStampLocal.toOffset(),
    )

    private class InstanceKeyNotFoundException(message: String) : Exception(message)
}
