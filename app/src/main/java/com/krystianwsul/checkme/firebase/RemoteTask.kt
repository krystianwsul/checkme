package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Task
import com.krystianwsul.checkme.domainmodel.TaskHierarchy
import com.krystianwsul.checkme.domainmodel.schedules.*
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.fromDate
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.common.firebase.*
import java.util.*

class RemoteTask<T : RemoteCustomTimeId>(
        domainFactory: DomainFactory,
        val remoteProject: RemoteProject<T>,
        private val remoteTaskRecord: RemoteTaskRecord<T>,
        now: ExactTimeStamp) : Task(domainFactory) {

    private val existingRemoteInstances = remoteTaskRecord.remoteInstanceRecords
            .values
            .map { RemoteInstance(domainFactory, remoteProject, this, it, domainFactory.localFactory.getInstanceShownRecord(this.remoteProject.id, it.taskId, it.scheduleYear, it.scheduleMonth, it.scheduleDay, it.scheduleCustomTimeId, it.scheduleHour, it.scheduleMinute), now) }
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
                .map { SingleSchedule(domainFactory, RemoteSingleScheduleBridge(domainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteDailyScheduleRecords
                .values
                .map { WeeklySchedule(domainFactory, RemoteDailyScheduleBridge(domainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteWeeklyScheduleRecords
                .values
                .map { WeeklySchedule(domainFactory, RemoteWeeklyScheduleBridge(domainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteMonthlyDayScheduleRecords
                .values
                .map { MonthlyDaySchedule(domainFactory, RemoteMonthlyDayScheduleBridge(domainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteMonthlyWeekScheduleRecords
                .values
                .map { MonthlyWeekSchedule(domainFactory, RemoteMonthlyWeekScheduleBridge(domainFactory, it)) })
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
        check(!TextUtils.isEmpty(name))

        remoteTaskRecord.name = name
        remoteTaskRecord.note = note
    }

    override fun addSchedules(scheduleDatas: List<CreateTaskViewModel.ScheduleData>, now: ExactTimeStamp) = createSchedules(now, scheduleDatas)

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

        val remoteInstanceRecord = remoteTaskRecord.newRemoteInstanceRecord(remoteProject, instanceJson, scheduleKey)

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

    fun createSchedules(now: ExactTimeStamp, scheduleDatas: List<CreateTaskViewModel.ScheduleData>) {
        for (scheduleData in scheduleDatas) {
            val timePair = scheduleData.timePair
            val (remoteCustomTimeId, hour, minute) = timePair.destructureRemote(remoteProject)

            when (scheduleData) {
                is CreateTaskViewModel.ScheduleData.Single -> {
                    val date = scheduleData.date

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(now.long, null, date.year, date.month, date.day, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(SingleSchedule(domainFactory, RemoteSingleScheduleBridge(domainFactory, remoteSingleScheduleRecord)))
                }
                is CreateTaskViewModel.ScheduleData.Weekly -> {
                    for (dayOfWeek in scheduleData.daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(now.long, null, dayOfWeek.ordinal, remoteCustomTimeId?.value, hour, minute)))

                        remoteSchedules.add(WeeklySchedule(domainFactory, RemoteWeeklyScheduleBridge(domainFactory, remoteWeeklyScheduleRecord)))
                    }
                }
                is CreateTaskViewModel.ScheduleData.MonthlyDay -> {
                    val (dayOfMonth, beginningOfMonth, _) = scheduleData

                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(now.long, null, dayOfMonth, beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyDaySchedule(domainFactory, RemoteMonthlyDayScheduleBridge(domainFactory, remoteMonthlyDayScheduleRecord)))
                }
                is CreateTaskViewModel.ScheduleData.MonthlyWeek -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth, _) = scheduleData

                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(now.long, null, dayOfMonth, dayOfWeek.ordinal, beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyWeekSchedule(domainFactory, RemoteMonthlyWeekScheduleBridge(domainFactory, remoteMonthlyWeekScheduleRecord)))
                }
            }
        }
    }

    fun copySchedules(now: ExactTimeStamp, schedules: Collection<Schedule>) {
        for (schedule in schedules) {
            val timePair = schedule.timePair
            val (remoteCustomTimeId, hour, minute) = timePair.destructureRemote(remoteProject)

            when (schedule) {
                is SingleSchedule -> {
                    val date = schedule.date

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(now.long, schedule.endTime, date.year, date.month, date.day, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(SingleSchedule(domainFactory, RemoteSingleScheduleBridge(domainFactory, remoteSingleScheduleRecord)))
                }
                is WeeklySchedule -> {
                    for (dayOfWeek in schedule.daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(now.long, schedule.endTime, dayOfWeek.ordinal, remoteCustomTimeId?.value, hour, minute)))

                        remoteSchedules.add(WeeklySchedule(domainFactory, RemoteWeeklyScheduleBridge(domainFactory, remoteWeeklyScheduleRecord)))
                    }
                }
                is MonthlyDaySchedule -> {
                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(now.long, schedule.endTime, schedule.dayOfMonth, schedule.beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyDaySchedule(domainFactory, RemoteMonthlyDayScheduleBridge(domainFactory, remoteMonthlyDayScheduleRecord)))
                }
                is MonthlyWeekSchedule -> {
                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(now.long, schedule.endTime, schedule.dayOfMonth, schedule.dayOfWeek.ordinal, schedule.beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyWeekSchedule(domainFactory, RemoteMonthlyWeekScheduleBridge(domainFactory, remoteMonthlyWeekScheduleRecord)))
                }
                else -> throw UnsupportedOperationException()
            }
        }
    }

    override fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<TaskHierarchy> = remoteProject.getTaskHierarchiesByChildTaskKey(childTaskKey)

    override fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> = remoteProject.getTaskHierarchiesByParentTaskKey(parentTaskKey)

    override fun belongsToRemoteProject() = true

    override fun updateProject(now: ExactTimeStamp, projectId: String): RemoteTask<*> {
        return if (projectId == remoteProject.id)
            this
        else
            domainFactory.convertRemoteToRemote(now, this, projectId)
    }

    fun generateInstance(scheduleDateTime: DateTime, instanceShownRecord: InstanceShownRecord?) = RemoteInstance(domainFactory, remoteProject, this, scheduleDateTime, instanceShownRecord)

    class MissingDayException(message: String) : Exception(message)
}
