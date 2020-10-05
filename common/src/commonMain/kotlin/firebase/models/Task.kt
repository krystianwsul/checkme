package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.models.interval.IntervalBuilder
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.*
import firebase.models.interval.HierarchyInterval
import firebase.models.interval.Interval
import firebase.models.interval.ScheduleInterval
import firebase.models.interval.Type

class Task<T : ProjectType>(
        val project: Project<T>,
        private val taskRecord: TaskRecord<T>,
        val rootInstanceManager: RootInstanceManager<T>
) : Current, QueryMatch {

    companion object {

        var USE_ROOT_INSTANCES = false

        val permutations = mutableMapOf<Int, Int>()
    }

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

    override val startExactTimeStamp = ExactTimeStamp(taskRecord.startTime)

    val note get() = taskRecord.note

    val taskKey get() = TaskKey(project.projectKey, taskRecord.id)

    val id get() = taskRecord.id

    val existingInstances: Map<ScheduleKey, Instance<T>> get() = _existingInstances

    val imageJson get() = taskRecord.image

    override val endExactTimeStamp get() = getEndData()?.exactTimeStamp

    private val parentTaskHierarchiesProperty =
            invalidatableLazy { project.getTaskHierarchiesByChildTaskKey(taskKey) }
    val parentTaskHierarchies by parentTaskHierarchiesProperty

    private val intervalsProperty = invalidatableLazy { IntervalBuilder.build(this) }
    private val intervals by intervalsProperty

    val scheduleIntervals
        get() = intervals.mapNotNull {
            (it.type as? Type.Schedule)?.getScheduleIntervals(it)
        }.flatten()

    val parentHierarchyIntervals
        get() = intervals.mapNotNull {
            (it.type as? Type.Child)?.getHierarchyInterval(it)
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

    override val normalizedName by invalidatableLazy { name.normalized() }
    override val normalizedNote by invalidatableLazy { note?.normalized() }

    fun getParentName(now: ExactTimeStamp) = getParentTask(now)?.name ?: project.name

    fun isVisible(now: ExactTimeStamp, hack24: Boolean): Boolean {
        if (!current(now))
            return false

        val rootTask = getRootTask(now)
        val schedules = rootTask.getCurrentSchedules(now)

        if (schedules.isEmpty())
            return true

        if (schedules.any { it.isVisible(this, now, hack24) })
            return true

        return false
    }// bo inheritance i testy

    private fun getRootTask(
            exactTimeStamp: ExactTimeStamp
    ): Task<T> = getParentTask(exactTimeStamp)?.getRootTask(exactTimeStamp) ?: this

    fun getCurrentSchedules(exactTimeStamp: ExactTimeStamp): List<ScheduleInterval<T>> {
        requireCurrent(exactTimeStamp)

        return getInterval(exactTimeStamp).let {
            (it.type as? Type.Schedule)?.getScheduleIntervals(it)
                    ?.filter { it.schedule.current(exactTimeStamp) }
                    ?: listOf()
        }
    }

    fun getCurrentNoScheduleOrParent(exactTimeStamp: ExactTimeStamp) =
            getInterval(exactTimeStamp).let {
                (it.type as? Type.NoSchedule)?.getNoScheduleOrParentInterval(it)
            }?.also {
                check(it.current(exactTimeStamp))
                check(it.noScheduleOrParent.current(exactTimeStamp))
            }

    fun isRootTask(exactTimeStamp: ExactTimeStamp): Boolean {
        requireCurrent(exactTimeStamp)

        return getParentTask(exactTimeStamp) == null
    }

    fun setEndData( // this is not recursive on children.  Get the whole tree beforehand.
            endData: EndData,
            taskUndoData: TaskUndoData? = null,
            recursive: Boolean = false
    ) {
        val now = endData.exactTimeStamp

        requireCurrent(now)

        taskUndoData?.taskKeys?.add(taskKey)

        val group = isGroupTask(now)

        getCurrentSchedules(now).forEach {
            it.requireCurrent(now)

            taskUndoData?.scheduleIds?.add(it.schedule.scheduleId)

            it.schedule.setEndExactTimeStamp(now)
        }

        if (group) {
            val remainingTaskHierarchies =
                    project.getTaskHierarchiesByParentTaskKey(taskKey).filter { it.notDeleted(now) }

            taskUndoData?.taskHierarchyKeys?.addAll(remainingTaskHierarchies.map { it.taskHierarchyKey })

            remainingTaskHierarchies.forEach { it.setEndExactTimeStamp(now) }
        }

        if (!recursive) {
            getParentTaskHierarchy(now)?.let {
                it.requireCurrent(now)
                it.taskHierarchy.requireCurrent(now)

                taskUndoData?.taskHierarchyKeys?.add(it.taskHierarchy.taskHierarchyKey)

                it.taskHierarchy.setEndExactTimeStamp(now)
            }
        }

        setMyEndExactTimeStamp(endData)
    }

    fun getGroupScheduleDateTime(exactTimeStamp: ExactTimeStamp): DateTime? {
        val hierarchyTimeStamp = getHierarchyExactTimeStamp(exactTimeStamp)

        val groupSingleSchedules = getCurrentSchedules(hierarchyTimeStamp)
                .asSequence()
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

    fun isGroupTask(exactTimeStamp: ExactTimeStamp) =
            getGroupScheduleDateTime(exactTimeStamp) != null

    fun endAllCurrentTaskHierarchies(now: ExactTimeStamp) =
            parentTaskHierarchies.filter { it.current(now) }.forEach { it.setEndExactTimeStamp(now) }

    fun endAllCurrentSchedules(now: ExactTimeStamp) =
            schedules.filter { it.current(now) }.forEach { it.setEndExactTimeStamp(now) }

    fun endAllCurrentNoScheduleOrParents(now: ExactTimeStamp) =
            noScheduleOrParents.filter { it.current(now) }.forEach { it.setEndExactTimeStamp(now) }

    private fun getParentTaskHierarchy(exactTimeStamp: ExactTimeStamp): HierarchyInterval<T>? {
        requireCurrent(exactTimeStamp)

        return getInterval(exactTimeStamp).let { (it.type as? Type.Child)?.getHierarchyInterval(it) }
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        requireNotCurrent(now)

        setMyEndExactTimeStamp(null)
    }

    fun getParentTask(exactTimeStamp: ExactTimeStamp): Task<T>? {
        requireNotDeleted(exactTimeStamp)

        return getParentTaskHierarchy(exactTimeStamp)?.run {
            requireNotDeleted(exactTimeStamp)
            taskHierarchy.requireNotDeleted(exactTimeStamp)

            taskHierarchy.parentTask.apply { requireNotDeleted(exactTimeStamp) }
        }
    }

    fun getPastRootInstances(now: ExactTimeStamp) = getInstances(
            null,
            now.plusOne(),
            now
    ).instances.filter { it.isRootInstance(now) }

    data class InstanceResult<out T : ProjectType>(val instances: List<Instance<out T>>, val hasMore: Boolean)

    /*
     todo to actually return a sequence from this, then the individual sequences from both parents
      and schedules would need to go through a mechanism that would somehow grab the next element
      from each one and yield them in order
     */
    fun getInstances(
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenEndExactTimeStamp: ExactTimeStamp,
            now: ExactTimeStamp
    ): InstanceResult<T> {
        val hash = hashCode() + (givenStartExactTimeStamp?.hashCode() ?: 0) + givenEndExactTimeStamp.hashCode() + now
                .hashCode()

        permutations[hash] = 1 + (permutations[hash] ?: 0)

        val startExactTimeStamp = listOfNotNull(
                givenStartExactTimeStamp,
                startExactTimeStamp
        ).maxOrNull()!!

        val endExactTimeStamp = listOfNotNull(
                endExactTimeStamp,
                givenEndExactTimeStamp
        ).minOrNull()!!

        val existingInstances = _existingInstances.values.filter {
            val scheduleExactTimeStamp = it.scheduleDateTime.toExactTimeStamp()

            if (scheduleExactTimeStamp < startExactTimeStamp)
                return@filter false

            if (scheduleExactTimeStamp >= endExactTimeStamp)
                return@filter false

            true
        }

        val existingHaveMore = _existingInstances.size > existingInstances.size

        val (scheduleInstances, schedulesHaveMore) = if (startExactTimeStamp >= endExactTimeStamp) {
            listOf<Instance<T>>() to false
        } else {
            val scheduleResults = scheduleIntervals.map {
                it.getDateTimesInRange(startExactTimeStamp, endExactTimeStamp)
            }

            Pair(
                    scheduleResults.flatMap { it.dateTimes.map(::getInstance).toList() },
                    scheduleResults.any { it.hasMore!! }
            )
        }

        val parentDatas = parentHierarchyIntervals.map {
            it.taskHierarchy
                    .parentTask
                    .getInstances(givenStartExactTimeStamp, givenEndExactTimeStamp, now)
        }

        val parentInstances = parentDatas.flatMap { it.instances }
                .flatMap { it.getChildInstances(now) }
                .asSequence()
                .map { it.first }
                .filter { it.taskKey == taskKey }
                .toList()

        val parentsHaveMore = parentDatas.any { it.hasMore }

        return InstanceResult(
                listOf(
                        existingInstances,
                        scheduleInstances,
                        parentInstances
                ).flatten()
                        .associateBy { it.scheduleKey }
                        .values
                        .toList(),
                existingHaveMore || schedulesHaveMore || parentsHaveMore
        )
    }

    fun getNextAlarm(now: ExactTimeStamp): TimeStamp? {
        val existingInstances = existingInstances.values
        val scheduleNextInstances = getCurrentSchedules(now).mapNotNull {
            it.getDateTimesInRange(now, null)
                    .dateTimes
                    .firstOrNull()
                    ?.let(::getInstance)
        }

        return (existingInstances + scheduleNextInstances)
                .map { it.instanceDateTime.timeStamp }
                .filter { it.toExactTimeStamp() > now }
                .minOrNull()
    }

    fun updateSchedules(
            ownerKey: UserKey,
            shownFactory: Instance.ShownFactory,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            now: ExactTimeStamp
    ) {
        val removeSchedules = mutableListOf<Schedule<T>>()
        val addScheduleDatas = scheduleDatas.toMutableList()

        val oldSchedules = getCurrentSchedules(now).map { it.schedule }
        val oldScheduleDatas =
                ScheduleGroup.getGroups(oldSchedules).map { it.scheduleData to it.schedules }
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

        val oldMockPair = oldSchedules.filterIsInstance<SingleSchedule<T>>()
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
            removeSchedules.forEach { it.setEndExactTimeStamp(now) }
            addSchedules(ownerKey, addScheduleDatas, now)
        }
    }

    fun getHierarchyExactTimeStamp(now: ExactTimeStamp) =
            listOfNotNull(now, endExactTimeStamp?.minusOne()).minOrNull()!!

    fun getChildTaskHierarchies(
            exactTimeStamp: ExactTimeStamp,
            groups: Boolean = false
    ): List<TaskHierarchy<T>> {
        val taskHierarchies = childHierarchyIntervals.filter {
            it.current(exactTimeStamp) &&
                    it.taskHierarchy.current(exactTimeStamp) &&
                    it.taskHierarchy.childTask.current(exactTimeStamp)
        }
                .map { it.taskHierarchy }
                .toMutableSet()

        if (groups && isGroupTask(exactTimeStamp))
            taskHierarchies += project.getTaskHierarchiesByParentTaskKey(taskKey)
                    .filter { it.current(exactTimeStamp) }

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

    fun getEndData() =
            taskRecord.endData?.let { EndData(ExactTimeStamp(it.time), it.deleteInstances) }

    private fun setMyEndExactTimeStamp(endData: EndData?) {
        taskRecord.endData =
                endData?.let { TaskJson.EndData(it.exactTimeStamp.long, it.deleteInstances) }
    }

    fun createChildTask(
            now: ExactTimeStamp,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double? = null
    ): Task<T> {
        val taskJson = TaskJson(name, now.long, null, note, image = image, ordinal = ordinal)

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

        (this::normalizedName.getDelegate() as InvalidatableLazyImpl<*>).invalidate()
        (this::normalizedNote.getDelegate() as InvalidatableLazyImpl<*>).invalidate()
    }

    private fun addSchedules(
            ownerKey: UserKey,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            now: ExactTimeStamp
    ) = createSchedules(ownerKey, now, scheduleDatas)

    fun addChild(childTask: Task<*>, now: ExactTimeStamp) {
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
            now: ExactTimeStamp,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            allReminders: Boolean = true
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
                    val (dayOfMonth, dayOfWeek, beginningOfMonth) = scheduleData

                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(
                            MonthlyWeekScheduleJson(
                                    now.long,
                                    null,
                                    dayOfMonth,
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
            now: ExactTimeStamp,
            schedules: Collection<Schedule<*>>
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
                                    schedule.endTime,
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
                                    schedule.endTime,
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
                                    schedule.endTime,
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
                                    schedule.endTime,
                                    schedule.dayOfMonth,
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
                                    schedule.endTime,
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
            now: ExactTimeStamp,
            projectId: ProjectKey<*>
    ): Task<*> {
        return if (projectId == project.projectKey)
            this
        else
            projectUpdater.convert(now, this, projectId)
    }

    fun getScheduleTextMultiline(
            scheduleTextFactory: ScheduleTextFactory,
            exactTimeStamp: ExactTimeStamp
    ): String {
        requireCurrent(exactTimeStamp)

        val currentSchedules = getCurrentSchedules(exactTimeStamp)
        currentSchedules.forEach { it.requireCurrent(exactTimeStamp) }

        return ScheduleGroup.getGroups(currentSchedules.map { it.schedule }).joinToString("\n") {
            scheduleTextFactory.getScheduleText(it, project)
        }
    }

    fun generateInstance(scheduleDateTime: DateTime) = Instance(project, this, scheduleDateTime)

    fun getScheduleText(
            scheduleTextFactory: ScheduleTextFactory,
            exactTimeStamp: ExactTimeStamp,
            showParent: Boolean = false
    ): String? {
        requireCurrent(exactTimeStamp)

        val currentSchedules = getCurrentSchedules(exactTimeStamp)
        val parentTask = getParentTask(exactTimeStamp)

        return if (parentTask == null) {
            currentSchedules.forEach { it.requireCurrent(exactTimeStamp) }

            ScheduleGroup.getGroups(currentSchedules.map { it.schedule }).joinToString(", ") {
                scheduleTextFactory.getScheduleText(it, project)
            }
        } else {
            check(currentSchedules.isEmpty())

            parentTask.name.takeIf { showParent }
        }
    }

    fun getInterval(exactTimeStamp: ExactTimeStamp) = intervals.single {
        it.containsExactTimeStamp(exactTimeStamp)
    }

    fun setNoScheduleOrParent(now: ExactTimeStamp) {
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
    fun hasFutureReminders(now: ExactTimeStamp) =
            current(now) && getRootTask(now).getCurrentSchedules(now)
                    .any { it.schedule is RepeatingSchedule<*> }

    override fun toString() = super.toString() + ", name: $name, taskKey: $taskKey"

    interface ScheduleTextFactory {

        fun getScheduleText(scheduleGroup: ScheduleGroup<*>, project: Project<*>): String
    }

    interface ProjectUpdater {

        fun <T : ProjectType> convert(
                now: ExactTimeStamp,
                startingTask: Task<T>,
                projectId: ProjectKey<*>
        ): Task<*>
    }

    data class EndData(
            val exactTimeStamp: ExactTimeStamp,
            val deleteInstances: Boolean
    )
}
