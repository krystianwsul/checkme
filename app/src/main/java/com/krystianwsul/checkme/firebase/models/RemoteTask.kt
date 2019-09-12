package com.krystianwsul.checkme.firebase.models

import com.krystianwsul.checkme.domain.Instance
import com.krystianwsul.checkme.domain.Task
import com.krystianwsul.checkme.domain.TaskHierarchy
import com.krystianwsul.checkme.domain.schedules.*
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.records.RemoteInstanceRecord
import com.krystianwsul.common.firebase.records.RemoteTaskRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.ScheduleKey
import com.krystianwsul.common.utils.TaskKey

class RemoteTask<T : RemoteCustomTimeId>(
        private val shownFactory: Instance.ShownFactory,
        val remoteProject: RemoteProject<T>,
        private val remoteTaskRecord: RemoteTaskRecord<T>,
        now: ExactTimeStamp) : Task() {

    private val existingRemoteInstances = remoteTaskRecord.remoteInstanceRecords
            .values
            .map { RemoteInstance(remoteProject, this, it, shownFactory.getShown(remoteProject.id, it.taskId, it.scheduleYear, it.scheduleMonth, it.scheduleDay, it.scheduleCustomTimeId, it.scheduleHour, it.scheduleMinute), now) }
            .associateBy { it.scheduleKey }
            .toMutableMap()

    private val remoteSchedules = ArrayList<Schedule>()

    override val name get() = remoteTaskRecord.name

    override val schedules get() = remoteSchedules

    override val startExactTimeStamp get() = ExactTimeStamp(remoteTaskRecord.startTime)

    override val note get() = remoteTaskRecord.note

    override val taskKey get() = TaskKey(remoteProject.id, remoteTaskRecord.id)

    val id get() = remoteTaskRecord.id

    override val existingInstances get() = existingRemoteInstances

    override val project get() = remoteProject

    private val uuid get() = remoteProject.uuid

    override val imageJson get() = remoteTaskRecord.image

    override var image: ImageState?
        get() {
            val image = remoteTaskRecord.image ?: return null

            return if (image.uploaderUuid != null) {
                if (image.uploaderUuid == uuid)
                    ImageState.Local(image.imageUuid)
                else
                    ImageState.Uploading
            } else {
                ImageState.Remote(image.imageUuid)
            }
        }
        set(value) {
            remoteTaskRecord.image = when (value) {
                null -> null
                is ImageState.Remote -> TaskJson.Image(value.uuid)
                is ImageState.Local -> TaskJson.Image(value.uuid, uuid)
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

    override fun getEndData() = remoteTaskRecord.endData?.let { EndData(ExactTimeStamp(it.time), it.deleteInstances) }

    override fun setMyEndExactTimeStamp(endData: EndData?) {
        remoteTaskRecord.endData = endData?.let { TaskJson.EndData(it.exactTimeStamp.long, it.deleteInstances) }
    }

    override fun createChildTask(now: ExactTimeStamp, name: String, note: String?, image: TaskJson.Image?): Task {
        val taskJson = TaskJson(name, now.long, null, null, null, null, note, image = image)

        val childTask = remoteProject.newRemoteTask(taskJson, now)

        remoteProject.createTaskHierarchy(this, childTask, now)

        return childTask
    }

    override fun getOldestVisible() = remoteTaskRecord.oldestVisible

    override fun setOldestVisible(date: Date) = remoteTaskRecord.setOldestVisible(OldestVisibleJson.fromDate(date))

    override fun delete() {
        schedules.toMutableList().forEach { it.delete() }

        remoteProject.deleteTask(this)
        remoteTaskRecord.delete()
    }

    override fun setName(name: String, note: String?) {
        check(name.isNotEmpty())

        remoteTaskRecord.name = name
        remoteTaskRecord.note = note
    }

    override fun addSchedules(ownerKey: String, scheduleDatas: List<Pair<ScheduleData, Time>>, now: ExactTimeStamp) = createSchedules(ownerKey, now, scheduleDatas)

    override fun addChild(childTask: Task, now: ExactTimeStamp) {
        check(childTask is RemoteTask<*>)

        @Suppress("UNCHECKED_CAST")
        remoteProject.createTaskHierarchy(this, childTask as RemoteTask<T>, now)
    }

    override fun deleteSchedule(schedule: Schedule) {
        check(remoteSchedules.contains(schedule))

        remoteSchedules.remove(schedule)
    }

    fun createRemoteInstanceRecord(remoteInstance: RemoteInstance<T>, scheduleDateTime: DateTime): RemoteInstanceRecord<T> {
        val instanceJson = InstanceJson(null, null, null, null, null, null, null, null)

        val scheduleKey = ScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        @Suppress("UNCHECKED_CAST")
        val remoteCustomTimeId = scheduleDateTime.time
                .timePair
                .customTimeKey
                ?.let { it.remoteCustomTimeId as T }

        val remoteInstanceRecord = remoteTaskRecord.newRemoteInstanceRecord(instanceJson, scheduleKey, remoteCustomTimeId)

        existingRemoteInstances[remoteInstance.scheduleKey] = remoteInstance

        return remoteInstanceRecord
    }

    fun deleteInstance(remoteInstance: RemoteInstance<T>) {
        val scheduleKey = remoteInstance.scheduleKey

        check(existingRemoteInstances.containsKey(scheduleKey))
        check(remoteInstance == existingRemoteInstances[scheduleKey])

        existingRemoteInstances.remove(scheduleKey)
    }

    fun getExistingInstanceIfPresent(scheduleKey: ScheduleKey) = existingRemoteInstances[scheduleKey]

    override fun getInstance(scheduleDateTime: DateTime): Instance {
        val scheduleKey = ScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        val existingInstance = getExistingInstanceIfPresent(scheduleKey)

        return existingInstance
                ?: generateInstance(scheduleDateTime, shownFactory.getShown(taskKey, scheduleDateTime))
    }

    fun createSchedules(ownerKey: String, now: ExactTimeStamp, scheduleDatas: List<Pair<ScheduleData, Time>>) {
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
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(now.long, null, dayOfWeek.ordinal, remoteCustomTimeId?.value, hour, minute)))

                        remoteSchedules.add(WeeklySchedule(this, RemoteWeeklyScheduleBridge(remoteProject.remoteProjectRecord, remoteWeeklyScheduleRecord)))
                    }
                }
                is ScheduleData.MonthlyDay -> {
                    val (dayOfMonth, beginningOfMonth, _) = scheduleData

                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(now.long, null, dayOfMonth, beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyDaySchedule(this, RemoteMonthlyDayScheduleBridge(remoteProject.remoteProjectRecord, remoteMonthlyDayScheduleRecord)))
                }
                is ScheduleData.MonthlyWeek -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth, _) = scheduleData

                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(now.long, null, dayOfMonth, dayOfWeek.ordinal, beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyWeekSchedule(this, RemoteMonthlyWeekScheduleBridge(remoteProject.remoteProjectRecord, remoteMonthlyWeekScheduleRecord)))
                }
            }
        }
    }

    fun copySchedules(ownerKey: String, now: ExactTimeStamp, schedules: Collection<Schedule>) {
        for (schedule in schedules) {
            val (remoteCustomTimeId, hour, minute) = remoteProject.getOrCopyAndDestructureTime(ownerKey, schedule.time)

            when (schedule) {
                is SingleSchedule -> {
                    val date = schedule.date

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(now.long, schedule.endTime, date.year, date.month, date.day, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(SingleSchedule(this, RemoteSingleScheduleBridge(remoteProject.remoteProjectRecord, remoteSingleScheduleRecord)))
                }
                is WeeklySchedule -> {
                    for (dayOfWeek in schedule.daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(now.long, schedule.endTime, dayOfWeek.ordinal, remoteCustomTimeId?.value, hour, minute)))

                        remoteSchedules.add(WeeklySchedule(this, RemoteWeeklyScheduleBridge(remoteProject.remoteProjectRecord, remoteWeeklyScheduleRecord)))
                    }
                }
                is MonthlyDaySchedule -> {
                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(now.long, schedule.endTime, schedule.dayOfMonth, schedule.beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyDaySchedule(this, RemoteMonthlyDayScheduleBridge(remoteProject.remoteProjectRecord, remoteMonthlyDayScheduleRecord)))
                }
                is MonthlyWeekSchedule -> {
                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(now.long, schedule.endTime, schedule.dayOfMonth, schedule.dayOfWeek.ordinal, schedule.beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyWeekSchedule(this, RemoteMonthlyWeekScheduleBridge(remoteProject.remoteProjectRecord, remoteMonthlyWeekScheduleRecord)))
                }
                else -> throw UnsupportedOperationException()
            }
        }
    }

    override fun getParentTaskHierarchies(): Set<TaskHierarchy> = remoteProject.getTaskHierarchiesByChildTaskKey(taskKey)

    override fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> = remoteProject.getTaskHierarchiesByParentTaskKey(parentTaskKey)

    override fun belongsToRemoteProject() = true

    override fun updateProject(projectUpdater: ProjectUpdater, now: ExactTimeStamp, projectId: String): RemoteTask<*> {
        return if (projectId == remoteProject.id)
            this
        else
            projectUpdater.convertRemoteToRemote(now, this, projectId)
    }

    override fun getScheduleTextMultiline(scheduleTextFactory: ScheduleTextFactory, exactTimeStamp: ExactTimeStamp): String? {
        check(current(exactTimeStamp))

        val currentSchedules = getCurrentSchedules(exactTimeStamp)

        check(currentSchedules.all { it.current(exactTimeStamp) })

        return ScheduleGroup.getGroups(currentSchedules).joinToString("\n") { scheduleTextFactory.getScheduleText(it, remoteProject) }
    }

    fun generateInstance(scheduleDateTime: DateTime, shown: Instance.Shown?) = RemoteInstance(remoteProject, this, scheduleDateTime, shown)

    override fun getScheduleText(scheduleTextFactory: ScheduleTextFactory, exactTimeStamp: ExactTimeStamp, showParent: Boolean): String? {
        check(current(exactTimeStamp))

        val currentSchedules = getCurrentSchedules(exactTimeStamp)
        val parentTask = getParentTask(exactTimeStamp)

        return if (parentTask == null) {
            check(currentSchedules.all { it.current(exactTimeStamp) })

            ScheduleGroup.getGroups(currentSchedules).joinToString(", ") { scheduleTextFactory.getScheduleText(it, remoteProject) }
        } else {
            check(currentSchedules.isEmpty())

            parentTask.name.takeIf { showParent }
        }
    }

    interface ScheduleTextFactory {

        fun getScheduleText(scheduleGroup: ScheduleGroup, remoteProject: RemoteProject<*>): String
    }

    interface ProjectUpdater {

        fun <T : RemoteCustomTimeId> convertRemoteToRemote(now: ExactTimeStamp, startingRemoteTask: RemoteTask<T>, projectId: String): RemoteTask<*>
    }
}
