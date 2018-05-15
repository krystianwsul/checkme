package com.krystianwsul.checkme.firebase

import android.content.Context
import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.firebase.json.*
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord
import com.krystianwsul.checkme.loaders.CreateTaskLoader
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import junit.framework.Assert
import java.util.*

class RemoteTask(domainFactory: DomainFactory, val remoteProject: RemoteProject, private val remoteTaskRecord: RemoteTaskRecord, now: ExactTimeStamp) : Task(domainFactory) {

    private val existingRemoteInstances = remoteTaskRecord.remoteInstanceRecords
            .values
            .map { RemoteInstance(domainFactory, this.remoteProject, it, domainFactory.localFactory.getInstanceShownRecord(this.remoteProject.id, it.taskId, it.scheduleYear, it.scheduleMonth, it.scheduleDay, it.scheduleCustomTimeId, it.scheduleHour, it.scheduleMinute), now) }
            .associateBy { it.scheduleKey }
            .toMutableMap()

    private val remoteSchedules = ArrayList<Schedule>()

    override val name get() = remoteTaskRecord.name

    private val remoteFactory get() = domainFactory.remoteFactory!!

    override val schedules get() = remoteSchedules

    override val startExactTimeStamp get() = ExactTimeStamp(remoteTaskRecord.startTime)

    override val note get() = remoteTaskRecord.note

    override val taskKey get() = TaskKey(remoteProject.id, remoteTaskRecord.id)

    val id get() = remoteTaskRecord.id

    override val existingInstances get() = existingRemoteInstances

    override val remoteNullableProject get() = remoteProject

    override val remoteNonNullProject get() = remoteProject

    init {
        remoteSchedules.addAll(remoteTaskRecord.mRemoteSingleScheduleRecords
                .values
                .map { SingleSchedule(domainFactory, RemoteSingleScheduleBridge(domainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.mRemoteDailyScheduleRecords
                .values
                .map { WeeklySchedule(domainFactory, RemoteDailyScheduleBridge(domainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.mRemoteWeeklyScheduleRecords
                .values
                .map { WeeklySchedule(domainFactory, RemoteWeeklyScheduleBridge(domainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.mRemoteMonthlyDayScheduleRecords
                .values
                .map { MonthlyDaySchedule(domainFactory, RemoteMonthlyDayScheduleBridge(domainFactory, it)) })

        remoteSchedules.addAll(remoteTaskRecord.mRemoteMonthlyWeekScheduleRecords
                .values
                .map { MonthlyWeekSchedule(domainFactory, RemoteMonthlyWeekScheduleBridge(domainFactory, it)) })
    }

    override fun getEndExactTimeStamp() = remoteTaskRecord.endTime?.let { ExactTimeStamp(it) }

    override fun setMyEndExactTimeStamp(now: ExactTimeStamp) = remoteTaskRecord.setEndTime(now.long!!)

    override fun createChildTask(now: ExactTimeStamp, name: String, note: String?): Task {
        val taskJson = TaskJson(name, now.long!!, null, null, null, null, note, emptyMap())

        val childTask = remoteProject.newRemoteTask(taskJson, now)

        remoteProject.createTaskHierarchy(this, childTask, now)

        return childTask
    }

    override fun getOldestVisible(): Date? {
        return if (remoteTaskRecord.oldestVisibleYear != null) {
            Assert.assertTrue(remoteTaskRecord.oldestVisibleMonth != null)
            Assert.assertTrue(remoteTaskRecord.oldestVisibleDay != null)

            Date(remoteTaskRecord.oldestVisibleYear!!, remoteTaskRecord.oldestVisibleMonth!!, remoteTaskRecord.oldestVisibleDay!!)
        } else {
            Assert.assertTrue(remoteTaskRecord.oldestVisibleMonth == null)
            Assert.assertTrue(remoteTaskRecord.oldestVisibleDay == null)

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
        Assert.assertTrue(!TextUtils.isEmpty(name))

        remoteTaskRecord.name = name
        remoteTaskRecord.note = note
    }

    override fun addSchedules(scheduleDatas: List<CreateTaskLoader.ScheduleData>, now: ExactTimeStamp) = createSchedules(now, scheduleDatas)

    override fun addChild(childTask: Task, now: ExactTimeStamp) {
        Assert.assertTrue(childTask is RemoteTask)

        remoteProject.createTaskHierarchy(this, childTask as RemoteTask, now)
    }

    override fun deleteSchedule(schedule: Schedule) {
        Assert.assertTrue(remoteSchedules.contains(schedule))

        remoteSchedules.remove(schedule)
    }

    fun createRemoteInstanceRecord(remoteInstance: RemoteInstance, scheduleDateTime: DateTime, now: ExactTimeStamp): RemoteInstanceRecord {
        val instanceJson = InstanceJson(null, null, null, null, null, null, null, now.long!!, null)

        val scheduleKey = ScheduleKey(scheduleDateTime.date, scheduleDateTime.time.timePair)

        val remoteInstanceRecord = remoteTaskRecord.newRemoteInstanceRecord(domainFactory, instanceJson, scheduleKey)

        existingRemoteInstances[remoteInstance.scheduleKey] = remoteInstance

        return remoteInstanceRecord
    }

    fun deleteInstance(remoteInstance: RemoteInstance) {
        val scheduleKey = remoteInstance.scheduleKey

        Assert.assertTrue(existingRemoteInstances.containsKey(scheduleKey))
        Assert.assertTrue(remoteInstance == existingRemoteInstances[scheduleKey])

        existingRemoteInstances.remove(scheduleKey)
    }

    fun getExistingInstanceIfPresent(scheduleKey: ScheduleKey) = existingRemoteInstances[scheduleKey]

    fun createSchedules(now: ExactTimeStamp, scheduleDatas: List<CreateTaskLoader.ScheduleData>) {
        for (scheduleData in scheduleDatas) {
            when (scheduleData.scheduleType) {
                ScheduleType.SINGLE -> {
                    val (date, timePair) = scheduleData as CreateTaskLoader.ScheduleData.SingleScheduleData

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.mCustomTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null)

                        remoteCustomTimeId = null
                        hour = timePair.mHourMinute!!.hour
                        minute = timePair.mHourMinute.minute
                    }

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(now.long!!, null, date.year, date.month, date.day, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(SingleSchedule(domainFactory, RemoteSingleScheduleBridge(domainFactory, remoteSingleScheduleRecord)))
                }
                ScheduleType.DAILY -> throw UnsupportedOperationException()
                ScheduleType.WEEKLY -> {
                    val (daysOfWeek, timePair) = scheduleData as CreateTaskLoader.ScheduleData.WeeklyScheduleData

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.mCustomTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null)

                        remoteCustomTimeId = null
                        hour = timePair.mHourMinute!!.hour
                        minute = timePair.mHourMinute.minute
                    }

                    for (dayOfWeek in daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(WeeklyScheduleJson(now.long!!, null, dayOfWeek.ordinal, remoteCustomTimeId, hour, minute)))

                        remoteSchedules.add(WeeklySchedule(domainFactory, RemoteWeeklyScheduleBridge(domainFactory, remoteWeeklyScheduleRecord)))
                    }
                }
                ScheduleType.MONTHLY_DAY -> {
                    val (dayOfMonth, beginningOfMonth, timePair) = scheduleData as CreateTaskLoader.ScheduleData.MonthlyDayScheduleData

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.mCustomTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null)

                        remoteCustomTimeId = null
                        hour = timePair.mHourMinute!!.hour
                        minute = timePair.mHourMinute.minute
                    }

                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(MonthlyDayScheduleJson(now.long!!, null, dayOfMonth, beginningOfMonth, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(MonthlyDaySchedule(domainFactory, RemoteMonthlyDayScheduleBridge(domainFactory, remoteMonthlyDayScheduleRecord)))
                }
                ScheduleType.MONTHLY_WEEK -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth, TimePair) = scheduleData as CreateTaskLoader.ScheduleData.MonthlyWeekScheduleData

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?
                    if (TimePair.mCustomTimeKey != null) {
                        Assert.assertTrue(TimePair.mHourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(TimePair.mCustomTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        Assert.assertTrue(TimePair.mHourMinute != null)

                        remoteCustomTimeId = null
                        hour = TimePair.mHourMinute!!.hour
                        minute = TimePair.mHourMinute.minute
                    }

                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(MonthlyWeekScheduleJson(now.long!!, null, dayOfMonth, dayOfWeek.ordinal, beginningOfMonth, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(MonthlyWeekSchedule(domainFactory, RemoteMonthlyWeekScheduleBridge(domainFactory, remoteMonthlyWeekScheduleRecord)))
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
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.mCustomTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null)

                        remoteCustomTimeId = null
                        hour = timePair.mHourMinute!!.hour
                        minute = timePair.mHourMinute.minute
                    }

                    val remoteSingleScheduleRecord = remoteTaskRecord.newRemoteSingleScheduleRecord(ScheduleWrapper(SingleScheduleJson(singleSchedule.startTime, singleSchedule.endTime, date.year, date.month, date.day, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(SingleSchedule(domainFactory, RemoteSingleScheduleBridge(domainFactory, remoteSingleScheduleRecord)))
                }
                ScheduleType.DAILY -> throw UnsupportedOperationException()
                ScheduleType.WEEKLY -> {
                    val weeklySchedule = schedule as WeeklySchedule

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?

                    val timePair = weeklySchedule.timePair
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.mCustomTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null)

                        remoteCustomTimeId = null
                        hour = timePair.mHourMinute!!.hour
                        minute = timePair.mHourMinute.minute
                    }

                    for (dayOfWeek in weeklySchedule.daysOfWeek) {
                        val remoteWeeklyScheduleRecord = remoteTaskRecord.newRemoteWeeklyScheduleRecord(ScheduleWrapper(WeeklyScheduleJson(schedule.startTime, schedule.endTime, dayOfWeek.ordinal, remoteCustomTimeId, hour, minute)))

                        remoteSchedules.add(WeeklySchedule(domainFactory, RemoteWeeklyScheduleBridge(domainFactory, remoteWeeklyScheduleRecord)))
                    }
                }
                ScheduleType.MONTHLY_DAY -> {
                    val monthlyDaySchedule = schedule as MonthlyDaySchedule

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?

                    val timePair = monthlyDaySchedule.timePair
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.mCustomTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null)

                        remoteCustomTimeId = null
                        hour = timePair.mHourMinute!!.hour
                        minute = timePair.mHourMinute.minute
                    }

                    val remoteMonthlyDayScheduleRecord = remoteTaskRecord.newRemoteMonthlyDayScheduleRecord(ScheduleWrapper(MonthlyDayScheduleJson(schedule.startTime, schedule.endTime, monthlyDaySchedule.dayOfMonth, monthlyDaySchedule.beginningOfMonth, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(MonthlyDaySchedule(domainFactory, RemoteMonthlyDayScheduleBridge(domainFactory, remoteMonthlyDayScheduleRecord)))
                }
                ScheduleType.MONTHLY_WEEK -> {
                    val monthlyWeekScheduleData = schedule as MonthlyWeekSchedule

                    val remoteCustomTimeId: String?
                    val hour: Int?
                    val minute: Int?

                    val timePair = monthlyWeekScheduleData.timePair
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null)

                        remoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(timePair.mCustomTimeKey, remoteProject)
                        hour = null
                        minute = null
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null)

                        remoteCustomTimeId = null
                        hour = timePair.mHourMinute!!.hour
                        minute = timePair.mHourMinute.minute
                    }

                    val remoteMonthlyWeekScheduleRecord = remoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(ScheduleWrapper(MonthlyWeekScheduleJson(schedule.startTime, schedule.endTime, monthlyWeekScheduleData.dayOfMonth, monthlyWeekScheduleData.dayOfWeek.ordinal, monthlyWeekScheduleData.beginningOfMonth, remoteCustomTimeId, hour, minute)))

                    remoteSchedules.add(MonthlyWeekSchedule(domainFactory, RemoteMonthlyWeekScheduleBridge(domainFactory, remoteMonthlyWeekScheduleRecord)))
                }
            }
        }
    }

    override fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<TaskHierarchy> = remoteProject.getTaskHierarchiesByChildTaskKey(childTaskKey)

    override fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> = remoteProject.getTaskHierarchiesByParentTaskKey(parentTaskKey)

    override fun belongsToRemoteProject() = true

    override fun updateProject(context: Context, now: ExactTimeStamp, projectId: String?): RemoteTask {
        Assert.assertTrue(TextUtils.isEmpty(projectId))

        return this
    }
}
