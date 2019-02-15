package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.firebase.json.*
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
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

    override fun getEndExactTimeStamp() = remoteTaskRecord.endTime?.let { ExactTimeStamp(it) }

    override fun setMyEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp?) {
        remoteTaskRecord.endTime = endExactTimeStamp?.long
    }

    override fun createChildTask(now: ExactTimeStamp, name: String, note: String?): Task {
        val taskJson = TaskJson(name, now.long, null, null, null, null, note)

        val childTask = remoteProject.newRemoteTask(taskJson, now)

        remoteProject.createTaskHierarchy(this, childTask, now)

        return childTask
    }

    override fun getOldestVisible() = remoteTaskRecord.oldestVisible

    override fun setOldestVisible(date: Date) = remoteTaskRecord.setOldestVisible(OldestVisibleJson(date))

    override fun delete() {
        val taskKey = taskKey

        ArrayList(remoteProject.getTaskHierarchiesByChildTaskKey(taskKey)).forEach { it.delete() }

        ArrayList(schedules).forEach { it.delete() }

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

            when (scheduleData.scheduleType) {
                ScheduleType.SINGLE -> {
                    val date = (scheduleData as CreateTaskViewModel.ScheduleData.SingleScheduleData).date

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(now.long, null, date.year, date.month, date.day, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(SingleSchedule(domainFactory, RemoteSingleScheduleBridge(domainFactory, remoteSingleScheduleRecord)))
                }
                ScheduleType.DAILY -> throw UnsupportedOperationException()
                ScheduleType.WEEKLY -> {
                    val daysOfWeek = (scheduleData as CreateTaskViewModel.ScheduleData.WeeklyScheduleData).daysOfWeek

                    for (dayOfWeek in daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(now.long, null, dayOfWeek.ordinal, remoteCustomTimeId?.value, hour, minute)))

                        remoteSchedules.add(WeeklySchedule(domainFactory, RemoteWeeklyScheduleBridge(domainFactory, remoteWeeklyScheduleRecord)))
                    }
                }
                ScheduleType.MONTHLY_DAY -> {
                    val (dayOfMonth, beginningOfMonth, _) = scheduleData as CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData

                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(now.long, null, dayOfMonth, beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyDaySchedule(domainFactory, RemoteMonthlyDayScheduleBridge(domainFactory, remoteMonthlyDayScheduleRecord)))
                }
                ScheduleType.MONTHLY_WEEK -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth, _) = scheduleData as CreateTaskViewModel.ScheduleData.MonthlyWeekScheduleData

                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(now.long, null, dayOfMonth, dayOfWeek.ordinal, beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyWeekSchedule(domainFactory, RemoteMonthlyWeekScheduleBridge(domainFactory, remoteMonthlyWeekScheduleRecord)))
                }
            }
        }
    }

    fun copySchedules(schedules: Collection<Schedule>) {
        for (schedule in schedules) {
            val timePair = schedule.timePair
            val (remoteCustomTimeId, hour, minute) = timePair.destructureRemote(remoteProject)

            when (schedule) {
                is SingleSchedule -> {
                    val date = schedule.date

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(schedule.startTime, schedule.endTime, date.year, date.month, date.day, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(SingleSchedule(domainFactory, RemoteSingleScheduleBridge(domainFactory, remoteSingleScheduleRecord)))
                }
                is WeeklySchedule -> {
                    for (dayOfWeek in schedule.daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(schedule.startTime, schedule.endTime, dayOfWeek.ordinal, remoteCustomTimeId?.value, hour, minute)))

                        remoteSchedules.add(WeeklySchedule(domainFactory, RemoteWeeklyScheduleBridge(domainFactory, remoteWeeklyScheduleRecord)))
                    }
                }
                is MonthlyDaySchedule -> {
                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(schedule.startTime, schedule.endTime, schedule.dayOfMonth, schedule.beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

                    remoteSchedules.add(MonthlyDaySchedule(domainFactory, RemoteMonthlyDayScheduleBridge(domainFactory, remoteMonthlyDayScheduleRecord)))
                }
                is MonthlyWeekSchedule -> {
                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(schedule.startTime, schedule.endTime, schedule.dayOfMonth, schedule.dayOfWeek.ordinal, schedule.beginningOfMonth, remoteCustomTimeId?.value, hour, minute)))

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

    class MissingDayException(message: String) : Exception(message)

    fun generateInstance(scheduleDateTime: DateTime, instanceShownRecord: InstanceShownRecord?) = RemoteInstance(domainFactory, remoteProject, this, scheduleDateTime, instanceShownRecord)
}
