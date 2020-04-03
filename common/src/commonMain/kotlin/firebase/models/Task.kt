package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.domain.schedules.*
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

class Task<T : ProjectType>(
        val remoteProject: Project<T>,
        private val taskRecord: TaskRecord<T>
) : Current {

    private val existingRemoteInstances = taskRecord.remoteInstanceRecords
            .values
            .map { Instance(remoteProject, this, it) }
            .associateBy { it.scheduleKey }
            .toMutableMap()

    private val remoteSchedules = ArrayList<Schedule<T>>()

    val name get() = taskRecord.name

    val schedules get() = remoteSchedules

    override val startExactTimeStamp = ExactTimeStamp(taskRecord.startTime)

    val note get() = taskRecord.note

    val taskKey get() = TaskKey(remoteProject.id, taskRecord.id)

    val id get() = taskRecord.id

    val existingInstances get() = existingRemoteInstances

    val project get() = remoteProject

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
        check(current(exactTimeStamp))

        val currentSchedules = schedules.filter { it.current(exactTimeStamp) }

        getSingleSchedule(exactTimeStamp)?.let { singleSchedule ->
            val instance = singleSchedule.getInstance(this)

            @Suppress("UNCHECKED_CAST")
            if (instance.scheduleDate != instance.instanceDate || instance.scheduleDateTime.time.timePair != instance.instanceTimePair)
                return listOf(SingleSchedule(
                        this,
                        MockSingleScheduleBridge(singleSchedule.singleScheduleBridge, instance)
                ))
        }

        return currentSchedules
    }

    private fun getSingleSchedule(exactTimeStamp: ExactTimeStamp): SingleSchedule<T>? {
        return schedules.singleOrNull { it.current(exactTimeStamp) } as? SingleSchedule
    }

    private class MockSingleScheduleBridge<T : ProjectType>(
            private val singleScheduleBridge: SingleScheduleBridge<T>,
            private val instance: Instance<T>
    ) : SingleScheduleBridge<T> by singleScheduleBridge {

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
    }

    fun isRootTask(exactTimeStamp: ExactTimeStamp): Boolean {
        check(current(exactTimeStamp))

        return getParentTask(exactTimeStamp) == null
    }

    fun setEndData(
            uuid: String,
            endData: EndData,
            taskUndoData: TaskUndoData? = null,
            recursive: Boolean = false
    ) {
        val now = endData.exactTimeStamp

        check(current(now))

        taskUndoData?.taskKeys?.add(taskKey)

        val schedules = getCurrentSchedules(now)
        if (isRootTask(now)) {
            check(schedules.all { it.current(now) })

            schedules.forEach {
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
                check(it.current(now))

                taskUndoData?.taskHierarchyKeys?.add(it.taskHierarchyKey)

                it.setEndExactTimeStamp(now)
            }
        }

        setMyEndExactTimeStamp(uuid, now, endData)
    }

    fun getParentTaskHierarchy(exactTimeStamp: ExactTimeStamp): TaskHierarchy<T>? {
        val taskHierarchies = if (current(exactTimeStamp)) {
            check(notDeleted(exactTimeStamp))

            getParentTaskHierarchies().filter { it.current(exactTimeStamp) }
        } else {
            // jeśli child task jeszcze nie istnieje, ale będzie utworzony jako child, zwróć ów przyszły hierarchy
            // żeby można było dodawać child instances do past parent instance

            check(notDeleted(exactTimeStamp))

            getParentTaskHierarchies().filter { it.startExactTimeStamp == startExactTimeStamp }
        }

        return if (taskHierarchies.isEmpty()) {
            null
        } else {
            taskHierarchies.single()
        }
    }

    fun clearEndExactTimeStamp(uuid: String, now: ExactTimeStamp) {
        check(!current(now))

        setMyEndExactTimeStamp(uuid, now, null)
    }

    fun getParentTask(exactTimeStamp: ExactTimeStamp): Task<T>? {
        check(notDeleted(exactTimeStamp))

        return getParentTaskHierarchy(exactTimeStamp)?.let {
            check(it.notDeleted(exactTimeStamp))

            it.parentTask.also {
                check(it.notDeleted(exactTimeStamp))
            }
        }
    }

    fun getPastRootInstances(now: ExactTimeStamp): List<Instance<T>> {
        val allInstances = mutableMapOf<InstanceKey, Instance<T>>()

        allInstances.putAll(existingInstances
                .values
                .filter { it.scheduleDateTime.timeStamp.toExactTimeStamp() <= now }
                .associateBy { it.instanceKey })

        allInstances.putAll(getInstances(null, now.plusOne(), now).associateBy { it.instanceKey })

        return allInstances.values
                .toList()
                .filter { it.isRootInstance(now) }
    }

    // there might be an issue here when moving task across projects
    fun updateOldestVisible(uuid: String, now: ExactTimeStamp) {
        // 24 hack
        val optional = getPastRootInstances(now).filter { it.isVisible(now, true) }.minBy { it.scheduleDateTime }

        val oldestVisible = listOfNotNull(optional?.scheduleDate, now.date).min()!!

        setOldestVisible(uuid, oldestVisible)
    }

    fun correctOldestVisible(date: Date) {
        val oldestVisible = getOldestVisible()
        check(oldestVisible != null && date < oldestVisible)

        ErrorLogger.instance.logException(OldestVisibleException6("$name real oldest: $oldestVisible, correct oldest: $date"))
    }

    fun getInstances(
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenEndExactTimeStamp: ExactTimeStamp,
            now: ExactTimeStamp
    ): List<Instance<T>> {
        val startExactTimeStamp = listOfNotNull(
                givenStartExactTimeStamp,
                startExactTimeStamp,
                getOldestVisible()?.let { ExactTimeStamp(it, HourMilli(0, 0, 0, 0)) } // 24 hack
        ).max()!!

        val endExactTimeStamp = listOfNotNull(
                endExactTimeStamp,
                givenEndExactTimeStamp
        ).min()!!

        val scheduleInstances = if (startExactTimeStamp >= endExactTimeStamp)
            listOf()
        else
            schedules.flatMap { it.getInstances(this, startExactTimeStamp, endExactTimeStamp).toList() }

        val parentInstances = getParentTaskHierarchies().map { it.parentTask }
                .flatMap { it.getInstances(givenStartExactTimeStamp, givenEndExactTimeStamp, now) }
                .flatMap { it.getChildInstances(now) }
                .asSequence()
                .map { it.first }
                .filter { it.taskKey == taskKey }
                .toList()

        return scheduleInstances + parentInstances
    }

    fun hasInstances(now: ExactTimeStamp) = existingInstances.values.isNotEmpty() || getInstances(null, now, now).isNotEmpty()

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
        val oldSingleSchedule = getSingleSchedule(now)

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

    private class OldestVisibleException6(message: String) : Exception(message)

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
        remoteSchedules.addAll(taskRecord.remoteSingleScheduleRecords
                .values
                .map { SingleSchedule(this, RemoteSingleScheduleBridge(it)) })

        remoteSchedules.addAll(taskRecord.remoteDailyScheduleRecords
                .values
                .map { WeeklySchedule(this, RemoteDailyScheduleBridge(it)) })

        remoteSchedules.addAll(taskRecord.remoteWeeklyScheduleRecords
                .values
                .map { WeeklySchedule(this, RemoteWeeklyScheduleBridge(it)) })

        remoteSchedules.addAll(taskRecord.remoteMonthlyDayScheduleRecords
                .values
                .map { MonthlyDaySchedule(this, RemoteMonthlyDayScheduleBridge(it)) })

        remoteSchedules.addAll(taskRecord.remoteMonthlyWeekScheduleRecords
                .values
                .map { MonthlyWeekSchedule(this, RemoteMonthlyWeekScheduleBridge(it)) })
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

        val childTask = remoteProject.newRemoteTask(taskJson)

        remoteProject.createTaskHierarchy(this, childTask, now)

        return childTask
    }

    fun getOldestVisible() = taskRecord.oldestVisible

    private fun setOldestVisible(uuid: String, date: Date) = taskRecord.setOldestVisible(uuid, OldestVisibleJson.fromDate(date))

    fun delete() {
        schedules.toMutableList().forEach { it.delete() }

        remoteProject.deleteTask(this)
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
        remoteProject.createTaskHierarchy(this, childTask as Task<T>, now)
    }

    fun deleteSchedule(schedule: Schedule<T>) {
        check(remoteSchedules.contains(schedule))

        remoteSchedules.remove(schedule)
    }

    fun createRemoteInstanceRecord(instance: Instance<T>): InstanceRecord<T> {
        @Suppress("UNCHECKED_CAST")
        val remoteInstanceRecord = taskRecord.newRemoteInstanceRecord(
                InstanceJson(),
                instance.scheduleKey,
                instance.scheduleDateTime
                        .time
                        .timePair
                        .customTimeKey
                        ?.let { it.customTimeId as CustomTimeId<T> }
        )

        existingRemoteInstances[instance.scheduleKey] = instance

        return remoteInstanceRecord
    }

    fun deleteInstance(instance: Instance<T>) {
        val scheduleKey = instance.scheduleKey

        check(existingRemoteInstances.containsKey(scheduleKey))
        check(instance == existingRemoteInstances[scheduleKey])

        existingRemoteInstances.remove(scheduleKey)
    }

    fun getExistingInstanceIfPresent(scheduleKey: ScheduleKey) = existingRemoteInstances[scheduleKey]

    fun getInstance(scheduleDateTime: DateTime): Instance<T> {
        val scheduleKey = ScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        val existingInstance = getExistingInstanceIfPresent(scheduleKey)

        return existingInstance ?: generateInstance(scheduleDateTime)
    }

    fun createSchedules(ownerKey: UserKey, now: ExactTimeStamp, scheduleDatas: List<Pair<ScheduleData, Time>>) {
        for ((scheduleData, time) in scheduleDatas) {
            val (customTimeId, hour, minute) = remoteProject.getOrCopyAndDestructureTime(ownerKey, time)

            when (scheduleData) {
                is ScheduleData.Single -> {
                    val date = scheduleData.date

                    val remoteSingleScheduleRecord = taskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(now.long, null, date.year, date.month, date.day, customTimeId?.value, hour, minute)))

                    remoteSchedules.add(SingleSchedule(this, RemoteSingleScheduleBridge(remoteSingleScheduleRecord)))
                }
                is ScheduleData.Weekly -> {
                    for (dayOfWeek in scheduleData.daysOfWeek) {
                        val remoteWeeklyScheduleRecord = taskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(
                                now.long,
                                null,
                                dayOfWeek.ordinal,
                                customTimeId?.value,
                                hour,
                                minute,
                                scheduleData.from?.toJson(),
                                scheduleData.until?.toJson()
                        )))

                        remoteSchedules.add(WeeklySchedule(this, RemoteWeeklyScheduleBridge(remoteWeeklyScheduleRecord)))
                    }
                }
                is ScheduleData.MonthlyDay -> {
                    val (dayOfMonth, beginningOfMonth, _) = scheduleData

                    val remoteMonthlyDayScheduleRecord = taskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(
                            now.long,
                            null,
                            dayOfMonth,
                            beginningOfMonth,
                            customTimeId?.value,
                            hour,
                            minute,
                            scheduleData.from?.toJson(),
                            scheduleData.until?.toJson()
                    )))

                    remoteSchedules.add(MonthlyDaySchedule(this, RemoteMonthlyDayScheduleBridge(remoteMonthlyDayScheduleRecord)))
                }
                is ScheduleData.MonthlyWeek -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth, _) = scheduleData

                    val remoteMonthlyWeekScheduleRecord = taskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(
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
                    )))

                    remoteSchedules.add(MonthlyWeekSchedule(this, RemoteMonthlyWeekScheduleBridge(remoteMonthlyWeekScheduleRecord)))
                }
            }
        }
    }

    fun copySchedules(deviceDbInfo: DeviceDbInfo, now: ExactTimeStamp, schedules: Collection<Schedule<*>>) {
        for (schedule in schedules) {
            val (customTimeId, hour, minute) = remoteProject.getOrCopyAndDestructureTime(deviceDbInfo.key, schedule.time)

            when (schedule) {
                is SingleSchedule<*> -> {
                    val date = schedule.date

                    val remoteSingleScheduleRecord = taskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(now.long, schedule.endTime, date.year, date.month, date.day, customTimeId?.value, hour, minute)))

                    remoteSchedules.add(SingleSchedule(this, RemoteSingleScheduleBridge(remoteSingleScheduleRecord)))
                }
                is WeeklySchedule<*> -> {
                    for (dayOfWeek in schedule.daysOfWeek) {
                        val remoteWeeklyScheduleRecord = taskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(
                                now.long,
                                schedule.endTime,
                                dayOfWeek.ordinal,
                                customTimeId?.value,
                                hour,
                                minute,
                                schedule.from?.toJson(),
                                schedule.until?.toJson()
                        )))

                        remoteSchedules.add(WeeklySchedule(this, RemoteWeeklyScheduleBridge(remoteWeeklyScheduleRecord)))
                    }
                }
                is MonthlyDaySchedule<*> -> {
                    val remoteMonthlyDayScheduleRecord = taskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(
                            now.long,
                            schedule.endTime,
                            schedule.dayOfMonth,
                            schedule.beginningOfMonth,
                            customTimeId?.value,
                            hour,
                            minute,
                            schedule.from?.toJson(),
                            schedule.until?.toJson()
                    )))

                    remoteSchedules.add(MonthlyDaySchedule(this, RemoteMonthlyDayScheduleBridge(remoteMonthlyDayScheduleRecord)))
                }
                is MonthlyWeekSchedule<*> -> {
                    val remoteMonthlyWeekScheduleRecord = taskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(
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
                    )))

                    remoteSchedules.add(MonthlyWeekSchedule(this, RemoteMonthlyWeekScheduleBridge(remoteMonthlyWeekScheduleRecord)))
                }
                else -> throw UnsupportedOperationException()
            }
        }
    }

    fun getParentTaskHierarchies(): Set<TaskHierarchy<T>> = remoteProject.getTaskHierarchiesByChildTaskKey(taskKey)

    fun getChildTaskHierarchies() = remoteProject.getTaskHierarchiesByParentTaskKey(taskKey)

    fun updateProject(
            projectUpdater: ProjectUpdater,
            now: ExactTimeStamp,
            projectId: ProjectKey<*>
    ): Task<*> {
        return if (projectId == remoteProject.id)
            this
        else
            projectUpdater.convertRemoteToRemote(now, this, projectId)
    }

    fun getScheduleTextMultiline(scheduleTextFactory: ScheduleTextFactory, exactTimeStamp: ExactTimeStamp): String? {
        check(current(exactTimeStamp))

        val currentSchedules = getCurrentSchedules(exactTimeStamp)

        check(currentSchedules.all { it.current(exactTimeStamp) })

        return ScheduleGroup.getGroups(currentSchedules).joinToString("\n") { scheduleTextFactory.getScheduleText(it, remoteProject) }
    }

    fun generateInstance(scheduleDateTime: DateTime) = Instance(remoteProject, this, scheduleDateTime)

    fun getScheduleText(
            scheduleTextFactory: ScheduleTextFactory,
            exactTimeStamp: ExactTimeStamp,
            showParent: Boolean = false
    ): String? {
        check(current(exactTimeStamp))

        val currentSchedules = getCurrentSchedules(exactTimeStamp)
        val parentTask = getParentTask(exactTimeStamp)

        return if (parentTask == null) {
            check(currentSchedules.all { it.current(exactTimeStamp) })

            ScheduleGroup.getGroups(currentSchedules).joinToString(", ") {
                scheduleTextFactory.getScheduleText(it, remoteProject)
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

        fun <T : ProjectType> convertRemoteToRemote(
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
