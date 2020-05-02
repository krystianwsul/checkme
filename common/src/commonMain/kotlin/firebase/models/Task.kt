package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.domain.schedules.*
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.SingleScheduleRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

class Task<T : ProjectType>(
        val project: Project<T>,
        private val taskRecord: TaskRecord<T>,
        val rootInstanceManager: RootInstanceManager<T>
) : Current {

    companion object {

        var USE_ROOT_INSTANCES = false // todo instances May 23rd 2020
    }

    private val _existingInstances = taskRecord.instanceRecords
            .values
            .toMutableList<InstanceRecord<T>>()
            .apply { addAll(rootInstanceManager.rootInstanceRecords.values) }
            .map { Instance(project, this, it) }
            .toMutableList()
            .associateBy { it.scheduleKey }
            .toMutableMap()

    private val _schedules = mutableListOf<Schedule<T>>()

    val name get() = taskRecord.name

    val schedules: List<Schedule<T>> get() = _schedules

    override val startExactTimeStamp = ExactTimeStamp(taskRecord.startTime)

    val note get() = taskRecord.note

    val taskKey get() = TaskKey(project.projectKey, taskRecord.id)

    val id get() = taskRecord.id

    val existingInstances: Map<ScheduleKey, Instance<T>> get() = _existingInstances

    val imageJson get() = taskRecord.image

    override val endExactTimeStamp get() = getEndData()?.exactTimeStamp

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

    fun getCurrentSchedules(exactTimeStamp: ExactTimeStamp): List<Schedule<T>> {
        requireCurrent(exactTimeStamp)

        val currentSchedules = schedules.filter { it.current(exactTimeStamp) }

        currentSchedules.getSingleSchedule()?.let { singleSchedule ->
            val instance = singleSchedule.getInstance(this)

            @Suppress("UNCHECKED_CAST")
            if (instance.scheduleDate != instance.instanceDate || instance.scheduleDateTime.time.timePair != instance.instanceTimePair)
                return listOf(SingleSchedule(
                        this,
                        MockSingleScheduleRecord(singleSchedule.singleScheduleRecord, instance)
                ))
        }

        return currentSchedules
    }

    private fun List<Schedule<T>>.getSingleSchedule() = singleOrNull() as? SingleSchedule

    private class MockSingleScheduleRecord<T : ProjectType>(
            private val singleScheduleRecord: SingleScheduleRecord<T>,
            private val instance: Instance<T>
    ) : SingleScheduleRecord<T>(
            singleScheduleRecord.taskRecord,
            singleScheduleRecord.createObject,
            singleScheduleRecord.id
    ) {

        override val customTimeKey get() = instance.instanceCustomTimeKey

        override val year get() = instance.instanceDate.year

        override val month get() = instance.instanceDate.month

        override val day get() = instance.instanceDate.day

        override val hour
            get() = instance.instanceTime
                    .timePair
                    .hourMinute
                    ?.hour

        override val minute
            get() = instance.instanceTime
                    .timePair
                    .hourMinute
                    ?.minute

        override val timePair
            get() = customTimeKey?.let { TimePair(it) } ?: TimePair(HourMinute(hour!!, minute!!))

        override val originalTimePair get() = singleScheduleRecord.timePair
    }

    fun isRootTask(exactTimeStamp: ExactTimeStamp): Boolean {
        requireCurrent(exactTimeStamp)

        return getParentTask(exactTimeStamp) == null
    }

    fun setEndData(
            uuid: String,
            endData: EndData,
            taskUndoData: TaskUndoData? = null,
            recursive: Boolean = false
    ) {
        val now = endData.exactTimeStamp

        requireCurrent(now)

        taskUndoData?.taskKeys?.add(taskKey)

        val schedules = getCurrentSchedules(now)
        if (isRootTask(now)) {
            schedules.forEach {
                it.requireCurrent(now)

                taskUndoData?.scheduleIds?.add(it.scheduleId)

                it.setEndExactTimeStamp(now)
            }
        } else {
            check(schedules.isEmpty())
        }

        getChildTaskHierarchies(now).forEach {
            it.childTask.setEndData(uuid, endData, taskUndoData, true)

            taskUndoData?.taskHierarchyKeys?.add(it.taskHierarchyKey)

            it.setEndExactTimeStamp(now)
        }

        if (!recursive) {
            getParentTaskHierarchy(now)?.let {
                it.requireCurrent(now)

                taskUndoData?.taskHierarchyKeys?.add(it.taskHierarchyKey)

                it.setEndExactTimeStamp(now)
            }
        }

        setMyEndExactTimeStamp(uuid, now, endData)
    }

    fun getParentTaskHierarchy(exactTimeStamp: ExactTimeStamp): TaskHierarchy<T>? {
        val taskHierarchies = if (current(exactTimeStamp)) {
            requireNotDeleted(exactTimeStamp)

            getParentTaskHierarchies().filter { it.current(exactTimeStamp) }
        } else {
            // jeśli child task jeszcze nie istnieje, ale będzie utworzony jako child, zwróć ów przyszły hierarchy
            // żeby można było dodawać child instances do past parent instance

            requireNotDeleted(exactTimeStamp)

            getParentTaskHierarchies().filter { it.startExactTimeStamp == startExactTimeStamp }
        }

        return if (taskHierarchies.isEmpty()) {
            null
        } else {
            taskHierarchies.single()
        }
    }

    fun clearEndExactTimeStamp(uuid: String, now: ExactTimeStamp) {
        requireNotCurrent(now)

        setMyEndExactTimeStamp(uuid, now, null)
    }

    fun getParentTask(exactTimeStamp: ExactTimeStamp): Task<T>? {
        requireNotDeleted(exactTimeStamp)

        return getParentTaskHierarchy(exactTimeStamp)?.run {
            requireNotDeleted(exactTimeStamp)

            parentTask.apply { requireNotDeleted(exactTimeStamp) }
        }
    }

    fun getPastRootInstances(now: ExactTimeStamp) = getInstances(
            null,
            now.plusOne(),
            now
    ).first.filter { it.isRootInstance(now) }

    // there might be an issue here when moving task across projects
    fun updateOldestVisible(uuid: String, now: ExactTimeStamp) {
        // 24 hack
        val oldestVisible = listOfNotNull(
                getPastRootInstances(now).filter { it.isVisible(now, true) && !it.exists() }.map { it.scheduleDate },
                listOf(now.date)
        ).flatten().min()!!

        setOldestVisible(uuid, oldestVisible)
    }

    fun updateOldestVisibleServer(now: ExactTimeStamp) {
        // 24 hack
        val oldestVisible = listOfNotNull(
                getPastRootInstances(now).filter { it.isVisible(now, true) && !it.exists() }.map { it.scheduleDate },
                listOf(now.date)
        ).flatten().min()!!

        taskRecord.oldestVisibleServer = oldestVisible.toJson()
    }

    /*
     todo to actually return a sequence from this, then the individual sequences from both parents
     and schedules would need to go through a mechanism that would somehow grab the next element
     from each one and yield them in order
     */
    fun getInstances(
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenEndExactTimeStamp: ExactTimeStamp,
            now: ExactTimeStamp
    ): Pair<List<Instance<T>>, Boolean> { // boolean = has more
        val startExactTimeStamp = listOfNotNull(
                givenStartExactTimeStamp,
                startExactTimeStamp
        ).max()!!

        val scheduleStartExactTimeStamp = listOfNotNull(
                startExactTimeStamp,
                getOldestVisible()?.let { ExactTimeStamp(it, HourMilli(0, 0, 0, 0)) } // 24 hack
        ).max()!!

        val endExactTimeStamp = listOfNotNull(
                endExactTimeStamp,
                givenEndExactTimeStamp
        ).min()!!

        val existingInstances = _existingInstances.values.filter {
            val scheduleExactTimeStamp = it.scheduleDateTime.toExactTimeStamp()

            if (scheduleExactTimeStamp < startExactTimeStamp)
                return@filter false

            if (scheduleExactTimeStamp >= endExactTimeStamp)
                return@filter false

            true
        }

        val (scheduleInstances, schedulesHaveMore) = if (scheduleStartExactTimeStamp >= endExactTimeStamp) {
            listOf<Instance<T>>() to false
        } else {
            val scheduleResults = schedules.map { it.getInstances(this, scheduleStartExactTimeStamp, endExactTimeStamp) }

            scheduleResults.flatMap { it.first.toList() } to scheduleResults.any { it.second }
        }

        val parentDatas = getParentTaskHierarchies().map {
            it.parentTask.getInstances(givenStartExactTimeStamp, givenEndExactTimeStamp, now)
        }

        val parentInstances = parentDatas.flatMap { it.first }
                .flatMap { it.getChildInstances(now) }
                .asSequence()
                .map { it.first }
                .filter { it.taskKey == taskKey }
                .toList()

        val parentsHaveMore = parentDatas.any { it.second }

        return Pair(
                listOf(
                        existingInstances,
                        scheduleInstances,
                        parentInstances
                ).flatten()
                        .associateBy { it.scheduleKey }
                        .values
                        .toList(),
                schedulesHaveMore || parentsHaveMore
        )
    }

    fun updateSchedules(
            ownerKey: UserKey,
            shownFactory: Instance.ShownFactory,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            now: ExactTimeStamp
    ) {
        val removeSchedules = mutableListOf<Schedule<T>>()
        val addScheduleDatas = scheduleDatas.toMutableList()

        val oldSchedules = getCurrentSchedules(now)
        val oldScheduleDatas = ScheduleGroup.getGroups(oldSchedules).map { it.scheduleData to it.schedules }
        for ((key, value) in oldScheduleDatas) {
            val existing = addScheduleDatas.singleOrNull { it.first == key }
            if (existing != null)
                addScheduleDatas.remove(existing)
            else
                removeSchedules.addAll(value)
        }

        val singleRemoveSchedule = removeSchedules.singleOrNull() as? SingleSchedule
        val singleAddSchedulePair = addScheduleDatas.singleOrNull()?.takeIf { it.first is ScheduleData.Single }
        val oldSingleSchedule = oldSchedules.getSingleSchedule()

        if (singleRemoveSchedule != null &&
                singleAddSchedulePair != null &&
                singleRemoveSchedule.scheduleId == oldSingleSchedule?.scheduleId
        ) {
            oldSingleSchedule.getInstance(this).setInstanceDateTime(
                    shownFactory,
                    ownerKey,
                    singleAddSchedulePair.run { DateTime((first as ScheduleData.Single).date, second) },
                    now
            )
        } else {
            removeSchedules.forEach { it.setEndExactTimeStamp(now) }
            addSchedules(ownerKey, addScheduleDatas, now)
        }
    }

    fun getHierarchyExactTimeStamp(now: ExactTimeStamp) = listOfNotNull(now, endExactTimeStamp?.minusOne()).min()!!

    fun getChildTaskHierarchies(exactTimeStamp: ExactTimeStamp) = getChildTaskHierarchies().filter {
        it.current(exactTimeStamp) && it.childTask.current(exactTimeStamp)
    }.sortedBy { it.ordinal }

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

    fun getEndData() = taskRecord.endData?.let { EndData(ExactTimeStamp(it.time), it.deleteInstances) }

    private fun setMyEndExactTimeStamp(uuid: String, now: ExactTimeStamp, endData: EndData?) {
        taskRecord.endData = endData?.let { TaskJson.EndData(it.exactTimeStamp.long, it.deleteInstances) }

        updateOldestVisible(uuid, now)
    }

    fun createChildTask(
            now: ExactTimeStamp,
            name: String,
            note: String?,
            image: TaskJson.Image?
    ): Task<T> {
        val taskJson = TaskJson(name, now.long, null, note, image = image)

        val childTask = project.newTask(taskJson)

        project.createTaskHierarchy(this, childTask, now)

        return childTask
    }

    fun getOldestVisible() = taskRecord.oldestVisible

    private fun setOldestVisible(
            uuid: String,
            date: Date
    ) = taskRecord.setOldestVisible(uuid, OldestVisibleJson.fromDate(date))

    fun delete() {
        schedules.toMutableList().forEach { it.delete() }

        project.deleteTask(this)
        taskRecord.delete()
    }

    fun setName(name: String, note: String?) {
        check(name.isNotEmpty())

        taskRecord.name = name
        taskRecord.note = note
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

        return existingInstance ?: generateInstance(scheduleDateTime)
    }

    fun createSchedules(ownerKey: UserKey, now: ExactTimeStamp, scheduleDatas: List<Pair<ScheduleData, Time>>) {
        for ((scheduleData, time) in scheduleDatas) {
            val (customTimeId, hour, minute) = project.getOrCopyAndDestructureTime(ownerKey, time)

            when (scheduleData) {
                is ScheduleData.Single -> {
                    val date = scheduleData.date

                    val singleScheduleRecord = taskRecord.newSingleScheduleRecord(SingleScheduleJson(
                            now.long,
                            null,
                            date.year,
                            date.month,
                            date.day,
                            customTimeId?.value,
                            hour,
                            minute
                    ))

                    _schedules += SingleSchedule(this, singleScheduleRecord)
                }
                is ScheduleData.Weekly -> {
                    for (dayOfWeek in scheduleData.daysOfWeek) {
                        val weeklyScheduleRecord = taskRecord.newWeeklyScheduleRecord(WeeklyScheduleJson(
                                now.long,
                                null,
                                dayOfWeek.ordinal,
                                customTimeId?.value,
                                hour,
                                minute,
                                scheduleData.from?.toJson(),
                                scheduleData.until?.toJson()
                        ))

                        _schedules += WeeklySchedule(this, weeklyScheduleRecord)
                    }
                }
                is ScheduleData.MonthlyDay -> {
                    val (dayOfMonth, beginningOfMonth, _) = scheduleData

                    val monthlyDayScheduleRecord = taskRecord.newMonthlyDayScheduleRecord(MonthlyDayScheduleJson(
                            now.long,
                            null,
                            dayOfMonth,
                            beginningOfMonth,
                            customTimeId?.value,
                            hour,
                            minute,
                            scheduleData.from?.toJson(),
                            scheduleData.until?.toJson()
                    ))

                    _schedules += MonthlyDaySchedule(this, monthlyDayScheduleRecord)
                }
                is ScheduleData.MonthlyWeek -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth) = scheduleData

                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(MonthlyWeekScheduleJson(
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
                    ))

                    _schedules += MonthlyWeekSchedule(this, monthlyWeekScheduleRecord)
                }
                is ScheduleData.Yearly -> {
                    val yearlyScheduleRecord = taskRecord.newYearlyScheduleRecord(YearlyScheduleJson(
                            now.long,
                            null,
                            scheduleData.month,
                            scheduleData.day,
                            customTimeId?.value,
                            hour,
                            minute,
                            scheduleData.from?.toJson(),
                            scheduleData.until?.toJson()
                    ))

                    _schedules += YearlySchedule(this, yearlyScheduleRecord)
                }
            }
        }
    }

    fun copySchedules(deviceDbInfo: DeviceDbInfo, now: ExactTimeStamp, schedules: Collection<Schedule<*>>) {
        for (schedule in schedules) {
            val (customTimeId, hour, minute) = project.getOrCopyAndDestructureTime(deviceDbInfo.key, schedule.time)

            when (schedule) {
                is SingleSchedule<*> -> {
                    val date = schedule.date

                    val singleScheduleRecord = taskRecord.newSingleScheduleRecord(SingleScheduleJson(
                            now.long,
                            schedule.endTime,
                            date.year,
                            date.month,
                            date.day,
                            customTimeId?.value,
                            hour,
                            minute
                    ))

                    _schedules += SingleSchedule(this, singleScheduleRecord)
                }
                is WeeklySchedule<*> -> {
                    for (dayOfWeek in schedule.daysOfWeek) {
                        val weeklyScheduleRecord = taskRecord.newWeeklyScheduleRecord(WeeklyScheduleJson(
                                now.long,
                                schedule.endTime,
                                dayOfWeek.ordinal,
                                customTimeId?.value,
                                hour,
                                minute,
                                schedule.from?.toJson(),
                                schedule.until?.toJson()
                        ))

                        _schedules += WeeklySchedule(this, weeklyScheduleRecord)
                    }
                }
                is MonthlyDaySchedule<*> -> {
                    val monthlyDayScheduleRecord = taskRecord.newMonthlyDayScheduleRecord(MonthlyDayScheduleJson(
                            now.long,
                            schedule.endTime,
                            schedule.dayOfMonth,
                            schedule.beginningOfMonth,
                            customTimeId?.value,
                            hour,
                            minute,
                            schedule.from?.toJson(),
                            schedule.until?.toJson()
                    ))

                    _schedules += MonthlyDaySchedule(this, monthlyDayScheduleRecord)
                }
                is MonthlyWeekSchedule<*> -> {
                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(MonthlyWeekScheduleJson(
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
                    ))

                    _schedules += MonthlyWeekSchedule(this, monthlyWeekScheduleRecord)
                }
                is YearlySchedule<*> -> {
                    val yearlyScheduleRecord = taskRecord.newYearlyScheduleRecord(YearlyScheduleJson(
                            now.long,
                            schedule.endTime,
                            schedule.month,
                            schedule.day,
                            customTimeId?.value,
                            hour,
                            minute,
                            schedule.from?.toJson(),
                            schedule.until?.toJson()
                    ))

                    _schedules += YearlySchedule(this, yearlyScheduleRecord)
                }
            }
        }
    }

    fun getParentTaskHierarchies(): Set<TaskHierarchy<T>> = project.getTaskHierarchiesByChildTaskKey(taskKey)

    fun getChildTaskHierarchies() = project.getTaskHierarchiesByParentTaskKey(taskKey)

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

    fun getScheduleTextMultiline(scheduleTextFactory: ScheduleTextFactory, exactTimeStamp: ExactTimeStamp): String {
        requireCurrent(exactTimeStamp)

        val currentSchedules = getCurrentSchedules(exactTimeStamp)
        currentSchedules.forEach { it.requireCurrent(exactTimeStamp) }

        return ScheduleGroup.getGroups(currentSchedules).joinToString("\n") {
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

            ScheduleGroup.getGroups(currentSchedules).joinToString(", ") {
                scheduleTextFactory.getScheduleText(it, project)
            }
        } else {
            check(currentSchedules.isEmpty())

            parentTask.name.takeIf { showParent }
        }
    }

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
