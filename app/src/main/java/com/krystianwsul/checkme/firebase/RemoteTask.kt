package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.firebase.json.*
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import java.util.*

class RemoteTask(
        kotlinDomainFactory: KotlinDomainFactory,
        val remoteProject: RemoteProject,
        private val remoteTaskRecord: RemoteTaskRecord,
        now: ExactTimeStamp) : Task(kotlinDomainFactory) {

    private val existingRemoteInstances = remoteTaskRecord.remoteInstanceRecords
            .values
            .map { RemoteInstance(kotlinDomainFactory, remoteProject, it, kotlinDomainFactory.localFactory.getInstanceShownRecord(this.remoteProject.id, it.taskId, it.scheduleYear, it.scheduleMonth, it.scheduleDay, it.scheduleCustomTimeId, it.scheduleHour, it.scheduleMinute), now) }
            .associateBy { it.scheduleKey }
            .toMutableMap()

    private val remoteSchedules = ArrayList<Schedule>()

    override val name get() = remoteTaskRecord.name

    private val remoteFactory get() = kotlinDomainFactory.remoteProjectFactory!!

    override val schedules get() = remoteSchedules

    override val startExactTimeStamp get() = ExactTimeStamp(remoteTaskRecord.startTime)

    override val note get() = remoteTaskRecord.note

    override val taskKey get() = TaskKey(remoteProject.id, remoteTaskRecord.id)

    val id get() = remoteTaskRecord.id

    override val existingInstances get() = existingRemoteInstances

    override val remoteNullableProject get() = remoteProject

    override val remoteNonNullProject get() = remoteProject

    init {
        remoteSchedules.addAll(remoteTaskRecord.remoteSingleScheduleRecords
                .values
                .map { SingleSchedule(kotlinDomainFactory, RemoteSingleScheduleBridge(kotlinDomainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteDailyScheduleRecords
                .values
                .map { WeeklySchedule(kotlinDomainFactory, RemoteDailyScheduleBridge(kotlinDomainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteWeeklyScheduleRecords
                .values
                .map { WeeklySchedule(kotlinDomainFactory, RemoteWeeklyScheduleBridge(kotlinDomainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteMonthlyDayScheduleRecords
                .values
                .map { MonthlyDaySchedule(kotlinDomainFactory, RemoteMonthlyDayScheduleBridge(kotlinDomainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.remoteMonthlyWeekScheduleRecords
                .values
                .map { MonthlyWeekSchedule(kotlinDomainFactory, RemoteMonthlyWeekScheduleBridge(kotlinDomainFactory, it)) })
    }

    override fun getEndExactTimeStamp() = remoteTaskRecord.endTime?.let { ExactTimeStamp(it) }

    override fun setMyEndExactTimeStamp(now: ExactTimeStamp) = remoteTaskRecord.setEndTime(now.long)

    override fun createChildTask(now: ExactTimeStamp, name: String, note: String?): Task {
        val taskJson = TaskJson(name, now.long, null, null, null, null, note)

        val childTask = remoteProject.newRemoteTask(taskJson, now)

        remoteProject.createTaskHierarchy(this, childTask, now)

        return childTask
    }

    override fun getOldestVisible(): Date? {
        return if (remoteTaskRecord.oldestVisibleYear != null && remoteTaskRecord.oldestVisibleMonth != null && remoteTaskRecord.oldestVisibleDay != null) {
            Date(remoteTaskRecord.oldestVisibleYear!!, remoteTaskRecord.oldestVisibleMonth!!, remoteTaskRecord.oldestVisibleDay!!)
        } else {
            if (remoteTaskRecord.oldestVisibleYear != null || remoteTaskRecord.oldestVisibleMonth != null || remoteTaskRecord.oldestVisibleDay != null)
                MyCrashlytics.logException(MissingDayException("projectId: ${remoteProject.id}, taskId: $id, oldestVisibleYear: ${remoteTaskRecord.oldestVisibleYear}, oldestVisibleMonth: ${remoteTaskRecord.oldestVisibleMonth}, oldestVisibleDay: ${remoteTaskRecord.oldestVisibleDay}"))

            null
        }
    }

    override fun setOldestVisible(date: Date) {
        remoteTaskRecord.setOldestVisibleYear(date.year)
        remoteTaskRecord.setOldestVisibleMonth(date.month)
        remoteTaskRecord.setOldestVisibleDay(date.day)
    }

    override fun delete() {
        val taskKey = taskKey

        ArrayList(remoteFactory.getTaskHierarchiesByChildTaskKey(taskKey)).forEach { it.delete() }

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
        check(childTask is RemoteTask)

        remoteProject.createTaskHierarchy(this, childTask, now)
    }

    override fun deleteSchedule(schedule: Schedule) {
        check(remoteSchedules.contains(schedule))

        remoteSchedules.remove(schedule)
    }

    fun createRemoteInstanceRecord(remoteInstance: RemoteInstance, scheduleDateTime: DateTime): RemoteInstanceRecord {
        val instanceJson = InstanceJson(null, null, null, null, null, null, null, null)

        val scheduleKey = ScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        val remoteInstanceRecord = remoteTaskRecord.newRemoteInstanceRecord(instanceJson, scheduleKey)

        existingRemoteInstances[remoteInstance.scheduleKey] = remoteInstance

        return remoteInstanceRecord
    }

    fun deleteInstance(remoteInstance: RemoteInstance) {
        val scheduleKey = remoteInstance.scheduleKey

        check(existingRemoteInstances.containsKey(scheduleKey))
        check(remoteInstance == existingRemoteInstances[scheduleKey])

        existingRemoteInstances.remove(scheduleKey)
    }

    fun getExistingInstanceIfPresent(scheduleKey: ScheduleKey) = existingRemoteInstances[scheduleKey]

    fun createSchedules(now: ExactTimeStamp, scheduleDatas: List<CreateTaskViewModel.ScheduleData>) {
        for (scheduleData in scheduleDatas) {
            when (scheduleData.scheduleType) {
                ScheduleType.SINGLE -> {
                    val (date, timePair) = scheduleData as CreateTaskViewModel.ScheduleData.SingleScheduleData

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?
                    if (timePair.customTimeKey != null) {
                        check(timePair.hourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.customTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        remoteCustomTimeId = null
                        hour = timePair.hourMinute!!.hour
                        minute = timePair.hourMinute.minute
                    }

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(now.long, null, date.year, date.month, date.day, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(SingleSchedule(kotlinDomainFactory, RemoteSingleScheduleBridge(kotlinDomainFactory, remoteSingleScheduleRecord)))
                }
                ScheduleType.DAILY -> throw UnsupportedOperationException()
                ScheduleType.WEEKLY -> {
                    val (daysOfWeek, timePair) = scheduleData as CreateTaskViewModel.ScheduleData.WeeklyScheduleData

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?
                    if (timePair.customTimeKey != null) {
                        check(timePair.hourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.customTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        remoteCustomTimeId = null
                        hour = timePair.hourMinute!!.hour
                        minute = timePair.hourMinute.minute
                    }

                    for (dayOfWeek in daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(now.long, null, dayOfWeek.ordinal, remoteCustomTimeId, hour, minute)))

                        remoteSchedules.add(WeeklySchedule(kotlinDomainFactory, RemoteWeeklyScheduleBridge(kotlinDomainFactory, remoteWeeklyScheduleRecord)))
                    }
                }
                ScheduleType.MONTHLY_DAY -> {
                    val (dayOfMonth, beginningOfMonth, timePair) = scheduleData as CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?
                    if (timePair.customTimeKey != null) {
                        check(timePair.hourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.customTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        remoteCustomTimeId = null
                        hour = timePair.hourMinute!!.hour
                        minute = timePair.hourMinute.minute
                    }

                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(now.long, null, dayOfMonth, beginningOfMonth, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(MonthlyDaySchedule(kotlinDomainFactory, RemoteMonthlyDayScheduleBridge(kotlinDomainFactory, remoteMonthlyDayScheduleRecord)))
                }
                ScheduleType.MONTHLY_WEEK -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth, TimePair) = scheduleData as CreateTaskViewModel.ScheduleData.MonthlyWeekScheduleData

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?
                    if (TimePair.customTimeKey != null) {
                        check(TimePair.hourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(TimePair.customTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        remoteCustomTimeId = null
                        hour = TimePair.hourMinute!!.hour
                        minute = TimePair.hourMinute.minute
                    }

                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(now.long, null, dayOfMonth, dayOfWeek.ordinal, beginningOfMonth, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(MonthlyWeekSchedule(kotlinDomainFactory, RemoteMonthlyWeekScheduleBridge(kotlinDomainFactory, remoteMonthlyWeekScheduleRecord)))
                }
            }
        }
    }

    fun copySchedules(schedules: Collection<Schedule>) {
        for (schedule in schedules) {
            when (schedule.scheduleType) {
                ScheduleType.SINGLE -> {
                    val singleSchedule = schedule as SingleSchedule

                    val date = singleSchedule.date

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?

                    val timePair = singleSchedule.timePair
                    if (timePair.customTimeKey != null) {
                        check(timePair.hourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.customTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        remoteCustomTimeId = null
                        hour = timePair.hourMinute!!.hour
                        minute = timePair.hourMinute.minute
                    }

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(singleSchedule.startTime, singleSchedule.endTime, date.year, date.month, date.day, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(SingleSchedule(kotlinDomainFactory, RemoteSingleScheduleBridge(kotlinDomainFactory, remoteSingleScheduleRecord)))
                }
                ScheduleType.DAILY -> throw UnsupportedOperationException()
                ScheduleType.WEEKLY -> {
                    val weeklySchedule = schedule as WeeklySchedule

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?

                    val timePair = weeklySchedule.timePair
                    if (timePair.customTimeKey != null) {
                        check(timePair.hourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.customTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        remoteCustomTimeId = null
                        hour = timePair.hourMinute!!.hour
                        minute = timePair.hourMinute.minute
                    }

                    for (dayOfWeek in weeklySchedule.daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(weeklyScheduleJson = WeeklyScheduleJson(schedule.startTime, schedule.endTime, dayOfWeek.ordinal, remoteCustomTimeId, hour, minute)))

                        remoteSchedules.add(WeeklySchedule(kotlinDomainFactory, RemoteWeeklyScheduleBridge(kotlinDomainFactory, remoteWeeklyScheduleRecord)))
                    }
                }
                ScheduleType.MONTHLY_DAY -> {
                    val monthlyDaySchedule = schedule as MonthlyDaySchedule

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?

                    val timePair = monthlyDaySchedule.timePair
                    if (timePair.customTimeKey != null) {
                        check(timePair.hourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.customTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        remoteCustomTimeId = null
                        hour = timePair.hourMinute!!.hour
                        minute = timePair.hourMinute.minute
                    }

                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(monthlyDayScheduleJson = MonthlyDayScheduleJson(schedule.startTime, schedule.endTime, monthlyDaySchedule.dayOfMonth, monthlyDaySchedule.beginningOfMonth, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(MonthlyDaySchedule(kotlinDomainFactory, RemoteMonthlyDayScheduleBridge(kotlinDomainFactory, remoteMonthlyDayScheduleRecord)))
                }
                ScheduleType.MONTHLY_WEEK -> {
                    val monthlyWeekScheduleData = schedule as MonthlyWeekSchedule

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?

                    val timePair = monthlyWeekScheduleData.timePair
                    if (timePair.customTimeKey != null) {
                        check(timePair.hourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.customTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        remoteCustomTimeId = null
                        hour = timePair.hourMinute!!.hour
                        minute = timePair.hourMinute.minute
                    }

                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(monthlyWeekScheduleJson = MonthlyWeekScheduleJson(schedule.startTime, schedule.endTime, monthlyWeekScheduleData.dayOfMonth, monthlyWeekScheduleData.dayOfWeek.ordinal, monthlyWeekScheduleData.beginningOfMonth, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(MonthlyWeekSchedule(kotlinDomainFactory, RemoteMonthlyWeekScheduleBridge(kotlinDomainFactory, remoteMonthlyWeekScheduleRecord)))
                }
            }
        }
    }

    override fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<TaskHierarchy> = remoteProject.getTaskHierarchiesByChildTaskKey(childTaskKey)

    override fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> = remoteProject.getTaskHierarchiesByParentTaskKey(parentTaskKey)

    override fun belongsToRemoteProject() = true

    override fun updateProject(now: ExactTimeStamp, projectId: String?): RemoteTask {
        check(TextUtils.isEmpty(projectId))

        return this
    }

    private class MissingDayException(message: String) : Exception(message)
}
