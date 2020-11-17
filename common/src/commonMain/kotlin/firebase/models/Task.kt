package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.models.interval.*
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.interrupt.throwIfInterrupted
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.*

class Task<T : ProjectType>(
        val project: Project<T>,
        private val taskRecord: TaskRecord<T>,
        val rootInstanceManager: RootInstanceManager<T>,
) : Current, CurrentOffset, QueryMatch {

    companion object {

        var USE_ROOT_INSTANCES = false
    }

    val endDataProperty = invalidatableLazyCallbacks {
        taskRecord.endData?.let {
            EndData(
                    ExactTimeStamp.Local(it.time),
                    it.deleteInstances,
                    ExactTimeStamp.Offset.fromOffset(it.time, it.offset)
            )
        }
    }
    val endData by endDataProperty

    private val _existingInstances = taskRecord.instanceRecords
            .values
            .toMutableList<InstanceRecord<T>>()
            .apply { addAll(rootInstanceManager.records) }
            .map { Instance(project, this, it) }
            .toMutableList()
            .associateBy { it.scheduleKey }
            .toMutableMap()

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

    private val parentTaskHierarchiesProperty = invalidatableLazy { project.getTaskHierarchiesByChildTaskKey(taskKey) }
    val parentTaskHierarchies by parentTaskHierarchiesProperty

    private val intervalsProperty = invalidatableLazy { IntervalBuilder.build(this) }
    private val intervals by intervalsProperty

    val scheduleIntervals
        get() = intervals.mapNotNull {
            (it.type as? Type.Schedule)?.getScheduleIntervals(it)
        }.flatten()

    val parentHierarchyIntervals
        get() = intervals.mapNotNull {
            (it.type as? Type.Child)?.getHierarchyInterval(it, ExactTimeStamp.Local.now)
        }

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

    var ordinal
        get() = taskRecord.ordinal ?: startExactTimeStamp.long.toDouble()
        set(value) {
            taskRecord.ordinal = value
        }

    private val normalizedNameDelegate = invalidatableLazy { name.normalized() }
    private val normalizedNoteDelegate = invalidatableLazy { note?.normalized() }

    override val normalizedName by normalizedNameDelegate
    override val normalizedNote by normalizedNoteDelegate

    fun getParentName(exactTimeStamp: ExactTimeStamp) = getParentTask(exactTimeStamp)?.name ?: project.name

    fun isVisible(now: ExactTimeStamp.Local, hack24: Boolean): Boolean {
        if (!current(now)) return false

        val rootTask = getRootTask(now)
        val schedules = rootTask.getCurrentScheduleIntervals(now)

        if (schedules.isEmpty()) return true

        if (schedules.any { it.isVisible(this, now, hack24) }) return true

        return false
    }// bo inheritance i testy

    private fun getRootTask(exactTimeStamp: ExactTimeStamp): Task<T> = getParentTask(exactTimeStamp)?.getRootTask(exactTimeStamp)
            ?: this

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

        val group = isGroupTask(now)

        getCurrentScheduleIntervals(now).forEach {
            it.requireCurrentOffset(now)

            taskUndoData?.scheduleIds?.add(it.schedule.scheduleId)

            it.schedule.setEndExactTimeStamp(now.toOffset())
        }

        if (group) {
            val remainingTaskHierarchies = project.getTaskHierarchiesByParentTaskKey(taskKey).filter {
                it.notDeleted(now)
            }

            taskUndoData?.taskHierarchyKeys?.addAll(remainingTaskHierarchies.map { it.taskHierarchyKey })

            remainingTaskHierarchies.forEach { it.setEndExactTimeStamp(now) }
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

    fun getGroupScheduleDateTime(exactTimeStamp: ExactTimeStamp): DateTime? {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(exactTimeStamp)

        val groupSingleSchedules = getCurrentScheduleIntervals(hierarchyExactTimeStamp).asSequence()
                .map { it.schedule }
                .filterIsInstance<SingleSchedule<*>>()
                .filter { it.group }
                .toList()

        return if (groupSingleSchedules.isEmpty()) {
            null
        } else {
            groupSingleSchedules.single().originalDateTime
        }
    }

    fun isGroupTask(exactTimeStamp: ExactTimeStamp) = getGroupScheduleDateTime(exactTimeStamp) != null

    fun endAllCurrentTaskHierarchies(now: ExactTimeStamp.Local) = parentTaskHierarchies.filter {
        it.currentOffset(now)
    }.forEach { it.setEndExactTimeStamp(now) }

    fun endAllCurrentSchedules(now: ExactTimeStamp.Local) =
            schedules.filter { it.currentOffset(now) }.forEach { it.setEndExactTimeStamp(now.toOffset()) }

    fun endAllCurrentNoScheduleOrParents(now: ExactTimeStamp.Local) = noScheduleOrParents.filter {
        it.currentOffset(now)
    }.forEach { it.setEndExactTimeStamp(now.toOffset()) }

    private fun getParentTaskHierarchy(exactTimeStamp: ExactTimeStamp): HierarchyInterval<T>? {
        requireCurrentOffset(exactTimeStamp)

        return getInterval(exactTimeStamp).let { (it.type as? Type.Child)?.getHierarchyInterval(it, exactTimeStamp) }
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

    fun getPastRootInstances(now: ExactTimeStamp.Local) = getInstances(
            null,
            now.toOffset().plusOne(),
            now,
            bySchedule = true,
            onlyRoot = true
    )

    private fun getExistingInstances(
            startExactTimeStamp: ExactTimeStamp.Offset,
            endExactTimeStamp: ExactTimeStamp.Offset?,
            now: ExactTimeStamp.Local,
            bySchedule: Boolean,
            onlyRoot: Boolean,
    ): Sequence<Instance<T>> {
        return _existingInstances.values
                .asSequence()
                .run { if (onlyRoot) filter { it.isRootInstance(now) } else this }
                .map { it to it.getSequenceDate(bySchedule) }
                .filter { (_, dateTime) ->
                    throwIfInterrupted()

                    val exactTimeStamp = dateTime.toLocalExactTimeStamp()

                    if (exactTimeStamp < startExactTimeStamp) return@filter false

                    if (endExactTimeStamp?.let { exactTimeStamp >= it } == true) return@filter false

                    true
                }
                .sortedBy { it.second }
                .map { it.first }
    }

    // contains only generated instances
    private fun getScheduleInstances(
            startExactTimeStamp: ExactTimeStamp.Offset,
            endExactTimeStamp: ExactTimeStamp.Offset?,
            now: ExactTimeStamp.Local,
            bySchedule: Boolean,
    ): Sequence<Instance<out T>> {
        val scheduleResults = scheduleIntervals.map {
            it.getDateTimesInRange(startExactTimeStamp, endExactTimeStamp)
        }

        val scheduleInstanceSequences = scheduleResults.map {
            throwIfInterrupted()

            it.mapNotNull {
                throwIfInterrupted()

                getInstance(it).takeIf { !it.exists() && it.isRootInstance(now) } // needed because of group tasks
            }
        }

        return combineInstanceSequences(scheduleInstanceSequences, bySchedule)
    }

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
                    .mapNotNull {
                        it.getChildInstances(now)
                                .map { it.first }
                                .singleOrNull { it.taskKey == taskKey }
                                ?.takeIf { !it.exists() }
                    }
        }

        return combineInstanceSequences(instanceSequences, bySchedule)
    }

    fun getInstances(
            givenStartExactTimeStamp: ExactTimeStamp.Offset?,
            givenEndExactTimeStamp: ExactTimeStamp.Offset?,
            now: ExactTimeStamp.Local,
            bySchedule: Boolean = false,
            onlyRoot: Boolean = false,
    ): Sequence<Instance<out T>> {
        throwIfInterrupted()

        val startExactTimeStamp = listOfNotNull(givenStartExactTimeStamp, startExactTimeStampOffset).maxOrNull()!!
        val endExactTimeStamp = listOfNotNull(givenEndExactTimeStamp, endExactTimeStampOffset).minOrNull()

        if (endExactTimeStamp?.let { startExactTimeStamp > it } == true) return sequenceOf()

        val instanceSequences = mutableListOf<Sequence<Instance<out T>>>()

        instanceSequences += getExistingInstances(startExactTimeStamp, endExactTimeStamp, now, bySchedule, onlyRoot)

        instanceSequences += getScheduleInstances(startExactTimeStamp, endExactTimeStamp, now, bySchedule)

        if (!onlyRoot) {
            instanceSequences += getParentInstances(
                    givenStartExactTimeStamp,
                    givenEndExactTimeStamp,
                    now,
                    bySchedule
            )
        }

        return combineInstanceSequences(instanceSequences, bySchedule)
    }

    fun getNextAlarm(now: ExactTimeStamp.Local): TimeStamp? {
        val existingInstances = existingInstances.values
        val scheduleNextInstances = getCurrentScheduleIntervals(now).mapNotNull {
            it.getDateTimesInRange(now.toOffset(), null)
                    .firstOrNull()
                    ?.let(::getInstance)
        }

        return (existingInstances + scheduleNextInstances)
                .map { it.instanceDateTime.timeStamp }
                .filter { it.toLocalExactTimeStamp() > now }
                .minOrNull()
    }

    fun updateSchedules(
            ownerKey: UserKey,
            shownFactory: Instance.ShownFactory,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            now: ExactTimeStamp.Local,
    ) {
        val removeSchedules = mutableListOf<Schedule<T>>()
        val addScheduleDatas = scheduleDatas.toMutableList()

        val oldScheduleIntervals = getCurrentScheduleIntervals(now).map { it.schedule }
        val oldScheduleDatas = ScheduleGroup.getGroups(oldScheduleIntervals).map { it.scheduleData to it.schedules }

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
        val singleAddSchedulePair =
                addScheduleDatas.singleOrNull()?.takeIf { it.first is ScheduleData.Single }

        val oldMockPair = oldScheduleIntervals.filterIsInstance<SingleSchedule<T>>()
                .singleOrNull()
                ?.let { singleSchedule ->
                    singleSchedule.mockInstance?.let { Pair(singleSchedule, it) }
                }

        if (singleRemoveSchedule != null &&
                singleAddSchedulePair != null &&
                oldMockPair != null
        ) {
            check(singleRemoveSchedule.scheduleId == oldMockPair.first.scheduleId)

            oldMockPair.second
                    .setInstanceDateTime(
                            shownFactory,
                            ownerKey,
                            singleAddSchedulePair.run {
                                DateTime(
                                        (first as ScheduleData.Single).date,
                                        second
                                )
                            },
                            now
                    )
        } else {
            removeSchedules.forEach { it.setEndExactTimeStamp(now.toOffset()) }
            addSchedules(ownerKey, addScheduleDatas, now)
        }
    }

    fun getHierarchyExactTimeStamp(exactTimeStamp: ExactTimeStamp) = listOfNotNull(
            exactTimeStamp,
            endExactTimeStampOffset?.minusOne()
    ).minOrNull()!!

    fun getChildTaskHierarchies(
            exactTimeStamp: ExactTimeStamp,
            groups: Boolean = false,
    ): List<TaskHierarchy<T>> {
        val taskHierarchies = childHierarchyIntervals.filter {
            it.currentOffset(exactTimeStamp)
                    && it.taskHierarchy.currentOffset(exactTimeStamp)
                    && it.taskHierarchy.childTask.currentOffset(exactTimeStamp)
        }
                .map { it.taskHierarchy }
                .toMutableSet()

        if (groups && isGroupTask(exactTimeStamp)) {
            taskHierarchies += project.getTaskHierarchiesByParentTaskKey(taskKey).filter {
                it.currentOffset(exactTimeStamp)
            }
        }

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

        (existingInstances + generatedInstances).values.forEach { it.onTaskEndChanged() }
    }

    fun createChildTask(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double? = null,
    ): Task<T> {
        val taskJson = PrivateTaskJson(
                name,
                now.long,
                now.offset,
                null,
                note,
                image = image,
                ordinal = ordinal
        )

        val childTask = project.newTask(taskJson)

        project.createTaskHierarchy(this, childTask, now)

        return childTask
    }

    fun delete() {
        schedules.toMutableList().forEach { it.delete() }

        project.deleteTask(this)
        taskRecord.delete()
    }

    fun setName(name: String, note: String?) {
        check(name.isNotEmpty())

        taskRecord.name = name
        taskRecord.note = note

        normalizedNameDelegate.invalidate()
        normalizedNoteDelegate.invalidate()
    }

    private fun addSchedules(
            ownerKey: UserKey,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            now: ExactTimeStamp.Local,
    ) = createSchedules(ownerKey, now, scheduleDatas)

    fun addChild(childTask: Task<*>, now: ExactTimeStamp.Local) {
        @Suppress("UNCHECKED_CAST")
        project.createTaskHierarchy(this, childTask as Task<T>, now)
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

        val newRecord = if (USE_ROOT_INSTANCES)
            rootInstanceManager::newRootInstanceRecord
        else
            taskRecord::newInstanceRecord

        @Suppress("UNCHECKED_CAST")
        val instanceRecord = newRecord(
                InstanceJson(),
                instance.scheduleKey,
                instance.scheduleDateTime
                        .time
                        .timePair
                        .customTimeKey
                        ?.let { it.customTimeId as CustomTimeId<T> }
        )

        _existingInstances[instance.scheduleKey] = instance

        return instanceRecord
    }

    fun deleteInstance(instance: Instance<T>) {
        val scheduleKey = instance.scheduleKey

        check(_existingInstances.containsKey(scheduleKey))
        check(instance == _existingInstances[scheduleKey])

        _existingInstances.remove(scheduleKey)
    }

    fun getExistingInstanceIfPresent(scheduleKey: ScheduleKey) = _existingInstances[scheduleKey]

    fun getInstance(scheduleDateTime: DateTime): Instance<T> {
        val scheduleKey = ScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        val existingInstance = getExistingInstanceIfPresent(scheduleKey)

        return (existingInstance ?: generateInstance(scheduleDateTime))
    }

    fun createSchedules(
            ownerKey: UserKey,
            now: ExactTimeStamp.Local,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            allReminders: Boolean = true,
    ) {
        if (!allReminders)
            check(scheduleDatas.single().first is ScheduleData.Single)

        for ((scheduleData, time) in scheduleDatas) {
            val (customTimeId, hour, minute) = project.getOrCopyAndDestructureTime(ownerKey, time)

            when (scheduleData) {
                is ScheduleData.Single -> {
                    val date = scheduleData.date

                    val singleScheduleRecord = taskRecord.newSingleScheduleRecord(
                            SingleScheduleJson(
                                    now.long,
                                    now.offset,
                                    null,
                                    null,
                                    date.year,
                                    date.month,
                                    date.day,
                                    customTimeId?.value,
                                    hour,
                                    minute,
                                    !allReminders
                            )
                    )

                    _schedules += SingleSchedule(this, singleScheduleRecord)
                }
                is ScheduleData.Weekly -> {
                    for (dayOfWeek in scheduleData.daysOfWeek) {
                        val weeklyScheduleRecord = taskRecord.newWeeklyScheduleRecord(
                                WeeklyScheduleJson(
                                        now.long,
                                        now.offset,
                                        null,
                                        null,
                                        dayOfWeek.ordinal,
                                        customTimeId?.value,
                                        hour,
                                        minute,
                                        scheduleData.from?.toJson(),
                                        scheduleData.until?.toJson(),
                                        scheduleData.interval
                                )
                        )

                        _schedules += WeeklySchedule(this, weeklyScheduleRecord)
                    }
                }
                is ScheduleData.MonthlyDay -> {
                    val (dayOfMonth, beginningOfMonth, _) = scheduleData

                    val monthlyDayScheduleRecord = taskRecord.newMonthlyDayScheduleRecord(
                            MonthlyDayScheduleJson(
                                    now.long,
                                    now.offset,
                                    null,
                                    null,
                                    dayOfMonth,
                                    beginningOfMonth,
                                    customTimeId?.value,
                                    hour,
                                    minute,
                                    scheduleData.from?.toJson(),
                                    scheduleData.until?.toJson()
                            )
                    )

                    _schedules += MonthlyDaySchedule(this, monthlyDayScheduleRecord)
                }
                is ScheduleData.MonthlyWeek -> {
                    val (weekOfMonth, dayOfWeek, beginningOfMonth) = scheduleData

                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(
                            MonthlyWeekScheduleJson(
                                    now.long,
                                    now.offset,
                                    null,
                                    null,
                                    weekOfMonth,
                                    dayOfWeek.ordinal,
                                    beginningOfMonth,
                                    customTimeId?.value,
                                    hour,
                                    minute,
                                    scheduleData.from?.toJson(),
                                    scheduleData.until?.toJson()
                            )
                    )

                    _schedules += MonthlyWeekSchedule(this, monthlyWeekScheduleRecord)
                }
                is ScheduleData.Yearly -> {
                    val yearlyScheduleRecord = taskRecord.newYearlyScheduleRecord(
                            YearlyScheduleJson(
                                    now.long,
                                    now.offset,
                                    null,
                                    null,
                                    scheduleData.month,
                                    scheduleData.day,
                                    customTimeId?.value,
                                    hour,
                                    minute,
                                    scheduleData.from?.toJson(),
                                    scheduleData.until?.toJson()
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
            schedules: Collection<Schedule<*>>,
    ) {
        val hasGroupSchedule = schedules.any { it is SingleSchedule<*> && it.group }
        if (hasGroupSchedule)
            check(schedules.size == 1)

        for (schedule in schedules) {
            val (customTimeId, hour, minute) = project.getOrCopyAndDestructureTime(
                    deviceDbInfo.key,
                    schedule.time
            )

            when (schedule) {
                is SingleSchedule<*> -> {
                    val date = schedule.date

                    val singleScheduleRecord = taskRecord.newSingleScheduleRecord(
                            SingleScheduleJson(
                                    now.long,
                                    now.offset,
                                    schedule.endExactTimeStamp?.long,
                                    schedule.endExactTimeStamp?.offset,
                                    date.year,
                                    date.month,
                                    date.day,
                                    customTimeId?.value,
                                    hour,
                                    minute,
                                    schedule.group
                            )
                    )

                    _schedules += SingleSchedule(this, singleScheduleRecord)
                }
                is WeeklySchedule<*> -> {
                    val weeklyScheduleRecord = taskRecord.newWeeklyScheduleRecord(
                            WeeklyScheduleJson(
                                    now.long,
                                    now.offset,
                                    schedule.endExactTimeStamp?.long,
                                    schedule.endExactTimeStamp?.offset,
                                    schedule.dayOfWeek.ordinal,
                                    customTimeId?.value,
                                    hour,
                                    minute,
                                    schedule.from?.toJson(),
                                    schedule.until?.toJson(),
                                    schedule.interval
                            )
                    )

                    _schedules += WeeklySchedule(this, weeklyScheduleRecord)
                }
                is MonthlyDaySchedule<*> -> {
                    val monthlyDayScheduleRecord = taskRecord.newMonthlyDayScheduleRecord(
                            MonthlyDayScheduleJson(
                                    now.long,
                                    now.offset,
                                    schedule.endExactTimeStamp?.long,
                                    schedule.endExactTimeStamp?.offset,
                                    schedule.dayOfMonth,
                                    schedule.beginningOfMonth,
                                    customTimeId?.value,
                                    hour,
                                    minute,
                                    schedule.from?.toJson(),
                                    schedule.until?.toJson()
                            )
                    )

                    _schedules += MonthlyDaySchedule(this, monthlyDayScheduleRecord)
                }
                is MonthlyWeekSchedule<*> -> {
                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(
                            MonthlyWeekScheduleJson(
                                    now.long,
                                    now.offset,
                                    schedule.endExactTimeStamp?.long,
                                    schedule.endExactTimeStamp?.offset,
                                    schedule.weekOfMonth,
                                    schedule.dayOfWeek.ordinal,
                                    schedule.beginningOfMonth,
                                    customTimeId?.value,
                                    hour,
                                    minute,
                                    schedule.from?.toJson(),
                                    schedule.until?.toJson()
                            )
                    )

                    _schedules += MonthlyWeekSchedule(this, monthlyWeekScheduleRecord)
                }
                is YearlySchedule<*> -> {
                    val yearlyScheduleRecord = taskRecord.newYearlyScheduleRecord(
                            YearlyScheduleJson(
                                    now.long,
                                    now.offset,
                                    schedule.endExactTimeStamp?.long,
                                    schedule.endExactTimeStamp?.offset,
                                    schedule.month,
                                    schedule.day,
                                    customTimeId?.value,
                                    hour,
                                    minute,
                                    schedule.from?.toJson(),
                                    schedule.until?.toJson()
                            )
                    )

                    _schedules += YearlySchedule(this, yearlyScheduleRecord)
                }
            }
        }

        intervalsProperty.invalidate()
    }

    fun invalidateParentTaskHierarchies() {
        parentTaskHierarchiesProperty.invalidate()
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

    fun generateInstance(scheduleDateTime: DateTime): Instance<T> {
        val instanceKey = InstanceKey(taskKey, scheduleDateTime.date, scheduleDateTime.time.timePair)

        if (!generatedInstances.containsKey(instanceKey))
            generatedInstances[instanceKey] = Instance(project, this, scheduleDateTime)

        return generatedInstances.getValue(instanceKey)
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

    fun getInterval(exactTimeStamp: ExactTimeStamp): Interval<T> {
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
                taskRecord.newNoScheduleOrParentRecord(NoScheduleOrParentJson(now.long))
        check(!noScheduleOrParentsMap.containsKey(noScheduleOrParentRecord.id))

        noScheduleOrParentsMap[noScheduleOrParentRecord.id] =
                NoScheduleOrParent(this, noScheduleOrParentRecord)

        invalidateIntervals()
    }

    fun correctIntervalEndExactTimeStamps() = intervals.asSequence()
            .filterIsInstance<Interval.Ended<T>>()
            .forEach { it.correctEndExactTimeStamps() }

    // maybe this should also handle multiple single schedules?
    fun hasFutureReminders(now: ExactTimeStamp.Local) = current(now)
            && getRootTask(now).getCurrentScheduleIntervals(now).any {
        it.schedule is RepeatingSchedule<*>
    }

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
}
