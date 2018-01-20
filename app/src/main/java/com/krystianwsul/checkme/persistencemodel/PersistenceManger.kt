package com.krystianwsul.checkme.persistencemodel

import android.annotation.SuppressLint
import android.content.Context
import com.krystianwsul.checkme.domainmodel.local.LocalTask
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*
import junit.framework.Assert

class PersistenceManger(
        private val _customTimeRecords: MutableList<LocalCustomTimeRecord>,
        private var customTimeMaxId: Int,
        private val _taskRecords: MutableList<TaskRecord>,
        private var taskMaxId: Int,
        private val _taskHierarchyRecords: MutableList<TaskHierarchyRecord>,
        private var taskHierarchyMaxId: Int,
        private val _scheduleRecords: MutableList<ScheduleRecord>,
        private var scheduleMaxId: Int,
        private val _singleScheduleRecords: MutableMap<Int, SingleScheduleRecord>,
        private val _dailyScheduleRecords: MutableMap<Int, DailyScheduleRecord>,
        private val _weeklyScheduleRecords: MutableMap<Int, WeeklyScheduleRecord>,
        private val _monthlyDayScheduleRecords: MutableMap<Int, MonthlyDayScheduleRecord>,
        private val _monthlyWeekScheduleRecords: MutableMap<Int, MonthlyWeekScheduleRecord>,
        private val _instanceRecords: MutableList<InstanceRecord>,
        private var instanceMaxId: Int,
        private val _instanceShownRecords: MutableList<InstanceShownRecord>,
        private var instanceShownMaxId: Int,
        private val uuidRecord: UuidRecord
) {

    companion object {

        private var sInstance: PersistenceManger? = null

        val instance: PersistenceManger
            get() {
                if (sInstance == null) {
                    val sqLiteDatabase = MySQLiteHelper.database

                    val customTimeRecords = LocalCustomTimeRecord.getCustomTimeRecords(sqLiteDatabase)
                    val customTimeMaxId = LocalCustomTimeRecord.getMaxId(sqLiteDatabase)

                    val taskRecords = TaskRecord.getTaskRecords(sqLiteDatabase)
                    val taskMaxId = TaskRecord.getMaxId(sqLiteDatabase)

                    val taskHierarchyRecords = if (taskRecords.isEmpty())
                        mutableListOf()
                    else
                        TaskHierarchyRecord.getTaskHierarchyRecords(sqLiteDatabase)

                    val taskHierarchyMaxId = TaskHierarchyRecord.getMaxId(sqLiteDatabase)

                    val scheduleRecords = ScheduleRecord.getScheduleRecords(sqLiteDatabase)
                    val scheduleMaxId = ScheduleRecord.getMaxId(sqLiteDatabase)

                    val singleScheduleRecords = SingleScheduleRecord.getSingleScheduleRecords(sqLiteDatabase)
                            .associateBy { it.scheduleId }
                            .toMutableMap()

                    val dailyScheduleRecords = DailyScheduleRecord.getDailyScheduleRecords(sqLiteDatabase)
                            .associateBy { it.scheduleId }
                            .toMutableMap()

                    val weeklyScheduleRecords = WeeklyScheduleRecord.getWeeklyScheduleRecords(sqLiteDatabase)
                            .associateBy { it.scheduleId }
                            .toMutableMap()

                    val monthlyDayScheduleRecords = MonthlyDayScheduleRecord.getMonthlyDayScheduleRecords(sqLiteDatabase)
                            .associateBy { it.scheduleId }
                            .toMutableMap()

                    val monthlyWeekScheduleRecords = MonthlyWeekScheduleRecord.getMonthlyWeekScheduleRecords(sqLiteDatabase)
                            .associateBy { it.scheduleId }
                            .toMutableMap()

                    val instanceRecords = InstanceRecord.getInstanceRecords(sqLiteDatabase)
                    val instanceMaxId = InstanceRecord.getMaxId(sqLiteDatabase)

                    val instanceShownRecords = InstanceShownRecord.getInstancesShownRecords(sqLiteDatabase)
                    val instanceShownMaxId = InstanceShownRecord.getMaxId(sqLiteDatabase)

                    val uuidRecord = UuidRecord.getUuidRecord(sqLiteDatabase)

                    sInstance = PersistenceManger(customTimeRecords, customTimeMaxId, taskRecords, taskMaxId, taskHierarchyRecords, taskHierarchyMaxId, scheduleRecords, scheduleMaxId, singleScheduleRecords, dailyScheduleRecords, weeklyScheduleRecords, monthlyDayScheduleRecords, monthlyWeekScheduleRecords, instanceRecords, instanceMaxId, instanceShownRecords, instanceShownMaxId, uuidRecord)
                }
            return sInstance!!
        }
    }


    val customTimeRecords: MutableCollection<LocalCustomTimeRecord>
        get() = _customTimeRecords

    val taskRecords: MutableCollection<TaskRecord>
        get() = _taskRecords

    val taskHierarchyRecords: MutableCollection<TaskHierarchyRecord>
        get() = _taskHierarchyRecords

    val scheduleRecords: MutableCollection<ScheduleRecord>
        get() = _scheduleRecords

    val singleScheduleRecords: MutableCollection<SingleScheduleRecord>
        get() = _singleScheduleRecords.values

    val dailyScheduleRecords: MutableCollection<DailyScheduleRecord>
        get() = _dailyScheduleRecords.values

    val weeklyScheduleRecords: MutableCollection<WeeklyScheduleRecord>
        get() = _weeklyScheduleRecords.values

    val monthlyDayScheduleRecords: MutableCollection<MonthlyDayScheduleRecord>
        get() = _monthlyDayScheduleRecords.values

    val monthlyWeekScheduleRecords: MutableCollection<MonthlyWeekScheduleRecord>
        get() = _monthlyWeekScheduleRecords.values

    val instanceRecords: MutableCollection<InstanceRecord>
        get() = _instanceRecords

    val instanceShownRecords: MutableCollection<InstanceShownRecord>
        get() = _instanceShownRecords

    val uuid get() = uuidRecord.uuid

    @SuppressLint("UseSparseArrays")
    constructor() : this(mutableListOf(), 0, mutableListOf(), 0, mutableListOf(), 0, mutableListOf(), 0, mutableMapOf(), mutableMapOf(), mutableMapOf(), mutableMapOf(), mutableMapOf(), mutableListOf(), 0, mutableListOf(), 0, UuidRecord(true, UuidRecord.newUuid()))

    @Synchronized
    fun reset() {
        sInstance = null
    }

    fun getScheduleRecords(localTaskId: Int) = _scheduleRecords.filter { it.rootTaskId == localTaskId }

    fun getSingleScheduleRecord(scheduleId: Int) = _singleScheduleRecords[scheduleId]

    fun getDailyScheduleRecord(scheduleId: Int) = _dailyScheduleRecords[scheduleId]

    fun getWeeklyScheduleRecord(scheduleId: Int) = _weeklyScheduleRecords[scheduleId]

    fun getMonthlyDayScheduleRecord(scheduleId: Int) = _monthlyDayScheduleRecords[scheduleId]

    fun getMonthlyWeekScheduleRecord(scheduleId: Int) = _monthlyWeekScheduleRecords[scheduleId]

    fun createCustomTimeRecord(name: String, hourMinutes: Map<DayOfWeek, HourMinute>): LocalCustomTimeRecord {
        Assert.assertTrue(name.isNotEmpty())

        val sunday = hourMinutes[DayOfWeek.SUNDAY]!!
        val monday = hourMinutes[DayOfWeek.MONDAY]!!
        val tuesday = hourMinutes[DayOfWeek.TUESDAY]!!
        val wednesday = hourMinutes[DayOfWeek.WEDNESDAY]!!
        val thursday = hourMinutes[DayOfWeek.THURSDAY]!!
        val friday = hourMinutes[DayOfWeek.FRIDAY]!!
        val saturday = hourMinutes[DayOfWeek.SATURDAY]!!

        val id = ++customTimeMaxId

        return LocalCustomTimeRecord(false, id, name, sunday.hour, sunday.minute, monday.hour, monday.minute, tuesday.hour, tuesday.minute, wednesday.hour, wednesday.minute, thursday.hour, thursday.minute, friday.hour, friday.minute, saturday.hour, saturday.minute, true).also {
            _customTimeRecords.add(it)
        }
    }

    fun createTaskRecord(name: String, startExactTimeStamp: ExactTimeStamp, note: String?): TaskRecord {
        Assert.assertTrue(name.isNotEmpty())

        val id = ++taskMaxId

        return TaskRecord(false, id, name, startExactTimeStamp.long!!, null, null, null, null, note).also {
            _taskRecords.add(it)
        }
    }

    fun createTaskHierarchyRecord(parentLocalTask: LocalTask, childLocalTask: LocalTask, startExactTimeStamp: ExactTimeStamp): TaskHierarchyRecord {
        Assert.assertTrue(parentLocalTask.current(startExactTimeStamp))
        Assert.assertTrue(childLocalTask.current(startExactTimeStamp))

        val id = ++taskHierarchyMaxId

        return TaskHierarchyRecord(false, id, parentLocalTask.id, childLocalTask.id, startExactTimeStamp.long!!, null).also {
            _taskHierarchyRecords.add(it)
        }
    }

    fun createScheduleRecord(rootLocalTask: LocalTask, scheduleType: ScheduleType, startExactTimeStamp: ExactTimeStamp): ScheduleRecord {
        Assert.assertTrue(rootLocalTask.current(startExactTimeStamp))

        val id = ++scheduleMaxId

        return ScheduleRecord(false, id, rootLocalTask.id, startExactTimeStamp.long!!, null, scheduleType.ordinal).also {
            _scheduleRecords.add(it)
        }
    }

    fun createSingleScheduleRecord(scheduleId: Int, date: Date, time: Time): SingleScheduleRecord {
        Assert.assertTrue(!_singleScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_dailyScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_weeklyScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_monthlyDayScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_monthlyWeekScheduleRecords.containsKey(scheduleId))

        val customTimeId: Int?
        val hour: Int?
        val minute: Int?

        if (time.timePair.mCustomTimeKey != null) {
            Assert.assertTrue(time.timePair.mHourMinute == null)

            customTimeId = time.timePair.mCustomTimeKey!!.mLocalCustomTimeId!!

            hour = null
            minute = null
        } else {
            Assert.assertTrue(time.timePair.mHourMinute != null)

            customTimeId = null

            hour = time.timePair.mHourMinute!!.hour
            minute = time.timePair.mHourMinute!!.minute
        }

        return SingleScheduleRecord(false, scheduleId, date.year, date.month, date.day, customTimeId, hour, minute).also {
            _singleScheduleRecords[it.scheduleId] = it
        }
    }

    fun createWeeklyScheduleRecord(scheduleId: Int, dayOfWeek: DayOfWeek, time: Time): WeeklyScheduleRecord {
        Assert.assertTrue(!_singleScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_dailyScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_weeklyScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_monthlyDayScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_monthlyWeekScheduleRecords.containsKey(scheduleId))

        val customTimeId: Int?
        val hour: Int?
        val minute: Int?

        if (time.timePair.mCustomTimeKey != null) {
            Assert.assertTrue(time.timePair.mHourMinute == null)

            customTimeId = time.timePair.mCustomTimeKey!!.mLocalCustomTimeId!!

            hour = null
            minute = null
        } else {
            Assert.assertTrue(time.timePair.mHourMinute != null)

            customTimeId = null

            hour = time.timePair.mHourMinute!!.hour
            minute = time.timePair.mHourMinute!!.minute
        }

        return WeeklyScheduleRecord(false, scheduleId, dayOfWeek.ordinal, customTimeId, hour, minute).also {
            _weeklyScheduleRecords[scheduleId] = it
        }
    }

    fun createMonthlyDayScheduleRecord(scheduleId: Int, dayOfMonth: Int, beginningOfMonth: Boolean, time: Time): MonthlyDayScheduleRecord {
        Assert.assertTrue(!_singleScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_dailyScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_weeklyScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_monthlyDayScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_monthlyWeekScheduleRecords.containsKey(scheduleId))

        val customTimeId: Int?
        val hour: Int?
        val minute: Int?

        if (time.timePair.mCustomTimeKey != null) {
            Assert.assertTrue(time.timePair.mHourMinute == null)

            customTimeId = time.timePair.mCustomTimeKey!!.mLocalCustomTimeId!!

            hour = null
            minute = null
        } else {
            Assert.assertTrue(time.timePair.mHourMinute != null)

            customTimeId = null

            hour = time.timePair.mHourMinute!!.hour
            minute = time.timePair.mHourMinute!!.minute
        }

        return MonthlyDayScheduleRecord(false, scheduleId, dayOfMonth, beginningOfMonth, customTimeId, hour, minute).also {
            _monthlyDayScheduleRecords[scheduleId] = it
        }
    }

    fun createMonthlyWeekScheduleRecord(scheduleId: Int, dayOfMonth: Int, dayOfWeek: DayOfWeek, beginningOfMonth: Boolean, time: Time): MonthlyWeekScheduleRecord {
        Assert.assertTrue(!_singleScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_dailyScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_weeklyScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_monthlyDayScheduleRecords.containsKey(scheduleId))
        Assert.assertTrue(!_monthlyWeekScheduleRecords.containsKey(scheduleId))

        val customTimeId: Int?
        val hour: Int?
        val minute: Int?

        if (time.timePair.mCustomTimeKey != null) {
            Assert.assertTrue(time.timePair.mHourMinute == null)

            customTimeId = time.timePair.mCustomTimeKey!!.mLocalCustomTimeId!!

            hour = null
            minute = null
        } else {
            Assert.assertTrue(time.timePair.mHourMinute != null)

            customTimeId = null

            hour = time.timePair.mHourMinute!!.hour
            minute = time.timePair.mHourMinute!!.minute
        }

        return MonthlyWeekScheduleRecord(false, scheduleId, dayOfMonth, dayOfWeek.ordinal, beginningOfMonth, customTimeId, hour, minute).also {
            _monthlyWeekScheduleRecords[scheduleId] = it
        }
    }

    fun createInstanceRecord(localTask: LocalTask, scheduleDate: Date, scheduleTimePair: TimePair, now: ExactTimeStamp): InstanceRecord {
        val scheduleCustomTimeId: Int?
        val scheduleHour: Int?
        val scheduleMinute: Int?

        if (scheduleTimePair.mCustomTimeKey != null) {
            Assert.assertTrue(scheduleTimePair.mHourMinute == null)

            scheduleCustomTimeId = scheduleTimePair.mCustomTimeKey.mLocalCustomTimeId!!

            scheduleHour = null
            scheduleMinute = null
        } else {
            Assert.assertTrue(scheduleTimePair.mHourMinute != null)

            scheduleCustomTimeId = null

            scheduleHour = scheduleTimePair.mHourMinute!!.hour
            scheduleMinute = scheduleTimePair.mHourMinute.minute
        }

        val id = ++instanceMaxId

        return InstanceRecord(false, id, localTask.id, null, scheduleDate.year, scheduleDate.month, scheduleDate.day, scheduleCustomTimeId, scheduleHour, scheduleMinute, null, null, null, null, null, null, now.long!!, false, false).also {
            _instanceRecords.add(it)
        }
    }

    fun save(context: Context, source: SaveService.Source) {
        SaveService.Factory.instance.startService(context, this, source)
    }

    fun createInstanceShownRecord(remoteTaskId: String, scheduleDate: Date, remoteCustomTimeId: String?, hour: Int?, minute: Int?, projectId: String): InstanceShownRecord {
        Assert.assertTrue(remoteTaskId.isNotEmpty())
        Assert.assertTrue(projectId.isNotEmpty())

        val id = ++instanceShownMaxId

        return InstanceShownRecord(false, id, remoteTaskId, scheduleDate.year, scheduleDate.month, scheduleDate.day, remoteCustomTimeId, hour, minute, false, false, projectId).also {
            _instanceShownRecords.add(it)
        }
    }

    fun deleteInstanceShownRecords(taskKeys: Set<TaskKey>) {
        val remove = _instanceShownRecords.filterNot { instanceShownRecord -> taskKeys.any { taskKey -> instanceShownRecord.projectId == taskKey.mRemoteProjectId && instanceShownRecord.taskId == taskKey.mRemoteTaskId } }

        remove.forEach { it.delete() }
    }
}