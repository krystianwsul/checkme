package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.DeviceDbInfo

import com.krystianwsul.common.domain.TaskHierarchy
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.domain.schedules.*
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.records.RemoteInstanceRecord
import com.krystianwsul.common.firebase.records.RemoteTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

class Task<T : RemoteCustomTimeId, U : ProjectKey>(
        val remoteProject: Project<T, U>,
        private val remoteTaskRecord: RemoteTaskRecord<T, *>
) {

    private val existingRemoteInstances = remoteTaskRecord.remoteInstanceRecords
            .values
            .map { Instance(remoteProject, this, it) }
            .associateBy { it.scheduleKey }
            .toMutableMap()

    private val remoteSchedules = ArrayList<Schedule>()

    val name get() = remoteTaskRecord.name

    val schedules get() = remoteSchedules

    val startExactTimeStamp get() = ExactTimeStamp(remoteTaskRecord.startTime)

    val note get() = remoteTaskRecord.note

    val taskKey get() = TaskKey(remoteProject.id, remoteTaskRecord.id)

    val id get() = remoteTaskRecord.id

    val existingInstances get() = existingRemoteInstances

    val project get() = remoteProject

    val imageJson get() = remoteTaskRecord.image

    fun getEndExactTimeStamp() = getEndData()?.exactTimeStamp

    fun current(exactTimeStamp: ExactTimeStamp) = startExactTimeStamp <= exactTimeStamp && notDeleted(exactTimeStamp)

    fun getParentName(now: ExactTimeStamp) = getParentTask(now)?.name ?: project.name

    fun notDeleted(exactTimeStamp: ExactTimeStamp) = getEndExactTimeStamp()?.let { it > exactTimeStamp } != false

    fun isVisible(now: ExactTimeStamp, hack24: Boolean): Boolean {
        if (!current(now))
            return false

        val rootTask = getRootTask(now)
        val schedules = rootTask.getCurrentSchedules(now)

        if (schedules.isEmpty())
            return true

        if (schedules.any { it.isVisible(this as Task<*, *>, now, hack24) })
            return true

        return false
    }// bo inheritance i testy

    private fun getRootTask(
            exactTimeStamp: ExactTimeStamp
    ): Task<*, *> = getParentTask(exactTimeStamp)?.getRootTask(exactTimeStamp)
            ?: (this as Task<*, *>)

    fun getCurrentSchedules(exactTimeStamp: ExactTimeStamp): List<Schedule> {
        check(current(exactTimeStamp))

        val currentSchedules = schedules.filter { it.current(exactTimeStamp) }

        getSingleSchedule(exactTimeStamp)?.let { singleSchedule ->
            val instance = singleSchedule.getInstance(this as Task<*, *>)

            if (instance.scheduleDate != instance.instanceDate || instance.scheduleDateTime.time.timePair != instance.instanceTimePair)
                return listOf(SingleSchedule(this, MockSingleScheduleBridge(singleSchedule, instance)))
        }

        return currentSchedules
    }

    private fun getSingleSchedule(exactTimeStamp: ExactTimeStamp): SingleSchedule? {
        return schedules.singleOrNull { it.current(exactTimeStamp) } as? SingleSchedule
    }

    private class MockSingleScheduleBridge(
            private val singleSchedule: SingleSchedule,
            private val instance: Instance<*, *>
    ) : SingleScheduleBridge by singleSchedule.singleScheduleBridge {

        override val customTimeKey get() = instance.instanceTimePair.customTimeKey

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

        override val remoteCustomTimeKey
            get() = instance.instanceTime
                    .timePair
                    .customTimeKey
                    ?.let { Pair(it.remoteProjectId, it.remoteCustomTimeId) }

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

    fun getParentTaskHierarchy(exactTimeStamp: ExactTimeStamp): TaskHierarchy? {
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

    fun getParentTask(exactTimeStamp: ExactTimeStamp): Task<*, *>? {
        check(notDeleted(exactTimeStamp))

        return getParentTaskHierarchy(exactTimeStamp)?.let {
            check(it.notDeleted(exactTimeStamp))

            it.parentTask.also {
                check(it.notDeleted(exactTimeStamp))
            }
        }
    }

    fun getPastRootInstances(now: ExactTimeStamp): List<Instance<*, *>> {
        val allInstances = mutableMapOf<InstanceKey, Instance<*, *>>()

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
    ): List<Instance<*, *>> {
        val startExactTimeStamp = listOfNotNull(
                givenStartExactTimeStamp,
                startExactTimeStamp,
                getOldestVisible()?.let { ExactTimeStamp(it, HourMilli(0, 0, 0, 0)) } // 24 hack
        ).max()!!

        val endExactTimeStamp = listOfNotNull(
                getEndExactTimeStamp(),
                givenEndExactTimeStamp
        ).min()!!

        val scheduleInstances = if (startExactTimeStamp >= endExactTimeStamp)
            listOf()
        else
            schedules.flatMap { it.getInstances(this as Task<*, *>, startExactTimeStamp, endExactTimeStamp).toList() }

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
        val removeSchedules = mutableListOf<Schedule>()
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
            oldSingleSchedule.getInstance(this as Task<*, *>).setInstanceDateTime(
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

    fun getHierarchyExactTimeStamp(now: ExactTimeStamp) = listOfNotNull(now, getEndExactTimeStamp()?.minusOne()).min()!!

    fun getChildTaskHierarchies(exactTimeStamp: ExactTimeStamp) = getChildTaskHierarchies().filter {
        it.current(exactTimeStamp) && it.childTask.current(exactTimeStamp)
    }.sortedBy { it.ordinal }

    fun getImage(deviceDbInfo: DeviceDbInfo): ImageState? {
        val image = remoteTaskRecord.image ?: return null

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
        remoteTaskRecord.image = when (imageState) {
            null -> null
            is ImageState.Remote -> TaskJson.Image(imageState.uuid)
            is ImageState.Local -> TaskJson.Image(imageState.uuid, deviceDbInfo.uuid)
            is ImageState.Uploading -> throw IllegalArgumentException()
        }
    }

    init {
        remoteSchedules.addAll(remoteTaskRecord.remoteSingleScheduleRecords
                .values
                .map { SingleSchedule(this, RemoteSingleScheduleBridge(remoteProject.remoteProjectRecord, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteDailyScheduleRecords
                .values
                .map { WeeklySchedule(this, RemoteDailyScheduleBridge(remoteProject.remoteProjectRecord, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteWeeklyScheduleRecords
                .values
                .map { WeeklySchedule(this, RemoteWeeklyScheduleBridge(remoteProject.remoteProjectRecord, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteMonthlyDayScheduleRecords
                .values
                .map { MonthlyDaySchedule(this, RemoteMonthlyDayScheduleBridge(remoteProject.remoteProjectRecord, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteMonthlyWeekScheduleRecords
                .values
                .map { MonthlyWeekSchedule(this, RemoteMonthlyWeekScheduleBridge(remoteProject.remoteProjectRecord, it)) })
    }

    fun getEndData() = remoteTaskRecord.endData?.let { EndData(ExactTimeStamp(it.time), it.deleteInstances) }

    private fun setMyEndExactTimeStamp(uuid: String, now: ExactTimeStamp, endData: EndData?) {
        remoteTaskRecord.endData = endData?.let { TaskJson.EndData(it.exactTimeStamp.long, it.deleteInstances) }

        updateOldestVisible(uuid, now)
    }

    fun createChildTask(
            now: ExactTimeStamp,
            name: String,
            note: String?,
            image: TaskJson.Image?
    ): Task<T, U> {
        val taskJson = TaskJson(name, now.long, null, note, image = image)

        val childTask = remoteProject.newRemoteTask(taskJson)

        remoteProject.createTaskHierarchy(this, childTask, now)

        return childTask
    }

    fun getOldestVisible() = remoteTaskRecord.oldestVisible

    private fun setOldestVisible(uuid: String, date: Date) = remoteTaskRecord.setOldestVisible(uuid, OldestVisibleJson.fromDate(date))

    fun delete() {
        schedules.toMutableList().forEach { it.delete() }

        remoteProject.deleteTask(this)
        remoteTaskRecord.delete()
    }

    fun setName(name: String, note: String?) {
        check(name.isNotEmpty())

        remoteTaskRecord.name = name
        remoteTaskRecord.note = note
    }

    private fun addSchedules(
            ownerKey: UserKey,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            now: ExactTimeStamp
    ) = createSchedules(ownerKey, now, scheduleDatas)

    fun addChild(childTask: Task<*, *>, now: ExactTimeStamp) {
        @Suppress("UNCHECKED_CAST")
        remoteProject.createTaskHierarchy(this, childTask as Task<T, U>, now)
    }

    fun deleteSchedule(schedule: Schedule) {
        check(remoteSchedules.contains(schedule))

        remoteSchedules.remove(schedule)
    }

    fun createRemoteInstanceRecord(instance: Instance<T, U>, scheduleDateTime: DateTime): RemoteInstanceRecord<T> {
        val instanceJson = InstanceJson(null, null, null, null, null, null, null, null)

        val scheduleKey = ScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        @Suppress("UNCHECKED_CAST")
        val remoteCustomTimeId = scheduleDateTime.time
                .timePair
                .customTimeKey
                ?.let { it.remoteCustomTimeId as T }

        val remoteInstanceRecord = remoteTaskRecord.newRemoteInstanceRecord(instanceJson, scheduleKey, remoteCustomTimeId)

        existingRemoteInstances[instance.scheduleKey] = instance

        return remoteInstanceRecord
    }

    fun deleteInstance(instance: Instance<T, U>) {
        val scheduleKey = instance.scheduleKey

        check(existingRemoteInstances.containsKey(scheduleKey))
        check(instance == existingRemoteInstances[scheduleKey])

        existingRemoteInstances.remove(scheduleKey)
    }

    fun getExistingInstanceIfPresent(scheduleKey: ScheduleKey) = existingRemoteInstances[scheduleKey]

    fun getInstance(scheduleDateTime: DateTime): Instance<*, *> {
        val scheduleKey = ScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        val existingInstance = getExistingInstanceIfPresent(scheduleKey)

        return existingInstance ?: generateInstance(scheduleDateTime)
    }

    fun createSchedules(ownerKey: UserKey, now: ExactTimeStamp, scheduleDatas: List<Pair<ScheduleData, Time>>) {
        for ((scheduleData, time) in scheduleDatas) {
            val (remoteCustomTimeId, hour, minute) = remoteProject.getOrCopyAndDestructureTime(ownerKey, time)

            when (scheduleData) {
                is ScheduleData.Single -> {
                    val date = scheduleData.date

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(now.long, null, date.year, date.month, date.day, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(SingleSchedule(this, RemoteSingleScheduleBridge(remoteProject.remoteProjectRecord, remoteSingleScheduleRecord)))
                }
                is ScheduleData.Weekly -> {
                    for (dayOfWeek in scheduleData.daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(
                                now.long,
                                null,
                                dayOfWeek.ordinal,
                                remoteCustomTimeId?.value,
                                hour,
                                minute,
                                scheduleData.from?.toJson(),
                                scheduleData.until?.toJson()
                        )))

                        remoteSchedules.add(WeeklySchedule(this, RemoteWeeklyScheduleBridge(remoteProject.remoteProjectRecord, remoteWeeklyScheduleRecord)))
                    }
                }
                is ScheduleData.MonthlyDay -> {
                    val (dayOfMonth, beginningOfMonth, _) = scheduleData

                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(
                            now.long,
                            null,
                            dayOfMonth,
                            beginningOfMonth,
                            remoteCustomTimeId?.value,
                            hour,
                            minute,
                            scheduleData.from?.toJson(),
                            scheduleData.until?.toJson()
                    )))

                    remoteSchedules.add(MonthlyDaySchedule(this, RemoteMonthlyDayScheduleBridge(remoteProject.remoteProjectRecord, remoteMonthlyDayScheduleRecord)))
                }
                is ScheduleData.MonthlyWeek -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth, _) = scheduleData

                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(
                            now.long,
                            null,
                            dayOfMonth,
                            dayOfWeek.ordinal,
                            beginningOfMonth,
                            remoteCustomTimeId?.value,
                            hour,
                            minute,
                            scheduleData.from?.toJson(),
                            scheduleData.until?.toJson()
                    )))

                    remoteSchedules.add(MonthlyWeekSchedule(this, RemoteMonthlyWeekScheduleBridge(remoteProject.remoteProjectRecord, remoteMonthlyWeekScheduleRecord)))
                }
            }
        }
    }

    fun copySchedules(deviceDbInfo: DeviceDbInfo, now: ExactTimeStamp, schedules: Collection<Schedule>) {
        for (schedule in schedules) {
            val (remoteCustomTimeId, hour, minute) = remoteProject.getOrCopyAndDestructureTime(deviceDbInfo.key, schedule.time)

            when (schedule) {
                is SingleSchedule -> {
                    val date = schedule.date

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(now.long, schedule.endTime, date.year, date.month, date.day, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(SingleSchedule(this, RemoteSingleScheduleBridge(remoteProject.remoteProjectRecord, remoteSingleScheduleRecord)))
                }
                is WeeklySchedule -> {
                    for (dayOfWeek in schedule.daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(
                                now.long,
                                schedule.endTime,
                                dayOfWeek.ordinal,
                                remoteCustomTimeId?.value,
                                hour,
                                minute,
                                schedule.from?.toJson(),
                                schedule.until?.toJson()
                        )))

                        remoteSchedules.add(WeeklySchedule(this, RemoteWeeklyScheduleBridge(remoteProject.remoteProjectRecord, remoteWeeklyScheduleRecord)))
                    }
                }
                is MonthlyDaySchedule -> {
                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(
                            now.long,
                            schedule.endTime,
                            schedule.dayOfMonth,
                            schedule.beginningOfMonth,
                            remoteCustomTimeId?.value,
                            hour,
                            minute,
                            schedule.from?.toJson(),
                            schedule.until?.toJson()
                    )))

                    remoteSchedules.add(MonthlyDaySchedule(this, RemoteMonthlyDayScheduleBridge(remoteProject.remoteProjectRecord, remoteMonthlyDayScheduleRecord)))
                }
                is MonthlyWeekSchedule -> {
                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(
                            now.long,
                            schedule.endTime,
                            schedule.dayOfMonth,
                            schedule.dayOfWeek.ordinal,
                            schedule.beginningOfMonth,
                            remoteCustomTimeId?.value,
                            hour,
                            minute,
                            schedule.from?.toJson(),
                            schedule.until?.toJson()
                    )))

                    remoteSchedules.add(MonthlyWeekSchedule(this, RemoteMonthlyWeekScheduleBridge(remoteProject.remoteProjectRecord, remoteMonthlyWeekScheduleRecord)))
                }
                else -> throw UnsupportedOperationException()
            }
        }
    }

    fun getParentTaskHierarchies(): Set<TaskHierarchy> = remoteProject.getTaskHierarchiesByChildTaskKey(taskKey)

    fun getChildTaskHierarchies() = remoteProject.getTaskHierarchiesByParentTaskKey(taskKey)

    fun updateProject(
            projectUpdater: ProjectUpdater,
            now: ExactTimeStamp,
            projectId: ProjectKey
    ): Task<*, *> {
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

        fun getScheduleText(scheduleGroup: ScheduleGroup, project: Project<*, *>): String
    }

    interface ProjectUpdater {

        fun <T : RemoteCustomTimeId, U : ProjectKey> convertRemoteToRemote(
                now: ExactTimeStamp,
                startingTask: Task<T, U>,
                projectId: ProjectKey
        ): Task<*, *>
    }

    data class EndData(
            val exactTimeStamp: ExactTimeStamp,
            val deleteInstances: Boolean
    )
}
