package com.krystianwsul.checkme.persistencemodel

import android.annotation.SuppressLint
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.local.LocalTask
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*


class PersistenceManager(
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
        private val _localInstanceRecords: MutableList<LocalInstanceRecord>,
        private var instanceMaxId: Int,
        private val _instanceShownRecords: MutableList<InstanceShownRecord>,
        private var instanceShownMaxId: Int,
        private val uuidRecord: UuidRecord) {

    companion object {

        val instance by lazy {
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

            val instanceRecords = LocalInstanceRecord.getInstanceRecords(sqLiteDatabase)
            val instanceMaxId = LocalInstanceRecord.getMaxId(sqLiteDatabase)

            val instanceShownRecords = InstanceShownRecord.getInstancesShownRecords(sqLiteDatabase)
            val instanceShownMaxId = InstanceShownRecord.getMaxId(sqLiteDatabase)

            val uuidRecord = UuidRecord.getUuidRecord(sqLiteDatabase)

            PersistenceManager(customTimeRecords, customTimeMaxId, taskRecords, taskMaxId, taskHierarchyRecords, taskHierarchyMaxId, scheduleRecords, scheduleMaxId, singleScheduleRecords, dailyScheduleRecords, weeklyScheduleRecords, monthlyDayScheduleRecords, monthlyWeekScheduleRecords, instanceRecords, instanceMaxId, instanceShownRecords, instanceShownMaxId, uuidRecord)
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

    val localInstanceRecords: MutableCollection<LocalInstanceRecord>
        get() = _localInstanceRecords

    val instanceShownRecords: MutableCollection<InstanceShownRecord>
        get() = _instanceShownRecords

    val uuid get() = uuidRecord.uuid

    @SuppressLint("UseSparseArrays")
    constructor() : this(mutableListOf(), 0, mutableListOf(), 0, mutableListOf(), 0, mutableListOf(), 0, mutableMapOf(), mutableMapOf(), mutableMapOf(), mutableMapOf(), mutableMapOf(), mutableListOf(), 0, mutableListOf(), 0, UuidRecord(true, UuidRecord.newUuid()))

    fun getScheduleRecords(localTaskId: Int) = _scheduleRecords.filter { it.rootTaskId == localTaskId }

    fun getSingleScheduleRecord(scheduleId: Int) = _singleScheduleRecords[scheduleId]!!

    fun getDailyScheduleRecord(scheduleId: Int) = _dailyScheduleRecords[scheduleId]!!

    fun getWeeklyScheduleRecord(scheduleId: Int) = _weeklyScheduleRecords[scheduleId]!!

    fun getMonthlyDayScheduleRecord(scheduleId: Int) = _monthlyDayScheduleRecords[scheduleId]!!

    fun getMonthlyWeekScheduleRecord(scheduleId: Int) = _monthlyWeekScheduleRecords[scheduleId]!!

    fun createCustomTimeRecord(name: String, hourMinutes: Map<DayOfWeek, HourMinute>): LocalCustomTimeRecord {
        check(name.isNotEmpty())

        val sunday = hourMinutes.getValue(DayOfWeek.SUNDAY)
        val monday = hourMinutes.getValue(DayOfWeek.MONDAY)
        val tuesday = hourMinutes.getValue(DayOfWeek.TUESDAY)
        val wednesday = hourMinutes.getValue(DayOfWeek.WEDNESDAY)
        val thursday = hourMinutes.getValue(DayOfWeek.THURSDAY)
        val friday = hourMinutes.getValue(DayOfWeek.FRIDAY)
        val saturday = hourMinutes.getValue(DayOfWeek.SATURDAY)

        val id = ++customTimeMaxId

        return LocalCustomTimeRecord(false, id, name, sunday.hour, sunday.minute, monday.hour, monday.minute, tuesday.hour, tuesday.minute, wednesday.hour, wednesday.minute, thursday.hour, thursday.minute, friday.hour, friday.minute, saturday.hour, saturday.minute, true).also {
            _customTimeRecords.add(it)
        }
    }

    fun createTaskRecord(name: String, startExactTimeStamp: ExactTimeStamp, note: String?): TaskRecord {
        check(name.isNotEmpty())

        val id = ++taskMaxId

        return TaskRecord(false, id, name, startExactTimeStamp.long, null, null, null, null, note).also {
            _taskRecords.add(it)
        }
    }

    fun createTaskHierarchyRecord(parentLocalTask: LocalTask, childLocalTask: LocalTask, startExactTimeStamp: ExactTimeStamp): TaskHierarchyRecord {
        check(parentLocalTask.current(startExactTimeStamp))
        check(childLocalTask.current(startExactTimeStamp))

        val id = ++taskHierarchyMaxId

        return TaskHierarchyRecord(false, id, parentLocalTask.id, childLocalTask.id, startExactTimeStamp.long, null, null).also {
            _taskHierarchyRecords.add(it)
        }
    }

    fun createScheduleRecord(rootLocalTask: LocalTask, scheduleType: ScheduleType, startExactTimeStamp: ExactTimeStamp): ScheduleRecord {
        check(rootLocalTask.current(startExactTimeStamp))

        val id = ++scheduleMaxId

        return ScheduleRecord(false, id, rootLocalTask.id, startExactTimeStamp.long, null, scheduleType.ordinal).also {
            _scheduleRecords.add(it)
        }
    }

    fun createSingleScheduleRecord(scheduleId: Int, date: Date, time: Time): SingleScheduleRecord {
        check(!_singleScheduleRecords.containsKey(scheduleId))
        check(!_dailyScheduleRecords.containsKey(scheduleId))
        check(!_weeklyScheduleRecords.containsKey(scheduleId))
        check(!_monthlyDayScheduleRecords.containsKey(scheduleId))
        check(!_monthlyWeekScheduleRecords.containsKey(scheduleId))

        val (customTimeId, hour, minute) = time.timePair.destructureLocal(DomainFactory.instance)

        return SingleScheduleRecord(false, scheduleId, date.year, date.month, date.day, customTimeId, hour, minute).also {
            _singleScheduleRecords[it.scheduleId] = it
        }
    }

    fun createWeeklyScheduleRecord(scheduleId: Int, dayOfWeek: DayOfWeek, time: Time): WeeklyScheduleRecord {
        check(!_singleScheduleRecords.containsKey(scheduleId))
        check(!_dailyScheduleRecords.containsKey(scheduleId))
        check(!_weeklyScheduleRecords.containsKey(scheduleId))
        check(!_monthlyDayScheduleRecords.containsKey(scheduleId))
        check(!_monthlyWeekScheduleRecords.containsKey(scheduleId))

        val (customTimeId, hour, minute) = time.timePair.destructureLocal(DomainFactory.instance)

        return WeeklyScheduleRecord(false, scheduleId, dayOfWeek.ordinal, customTimeId, hour, minute).also {
            _weeklyScheduleRecords[scheduleId] = it
        }
    }

    fun createMonthlyDayScheduleRecord(scheduleId: Int, dayOfMonth: Int, beginningOfMonth: Boolean, time: Time): MonthlyDayScheduleRecord {
        check(!_singleScheduleRecords.containsKey(scheduleId))
        check(!_dailyScheduleRecords.containsKey(scheduleId))
        check(!_weeklyScheduleRecords.containsKey(scheduleId))
        check(!_monthlyDayScheduleRecords.containsKey(scheduleId))
        check(!_monthlyWeekScheduleRecords.containsKey(scheduleId))

        val (customTimeId, hour, minute) = time.timePair.destructureLocal(DomainFactory.instance)

        return MonthlyDayScheduleRecord(false, scheduleId, dayOfMonth, beginningOfMonth, customTimeId, hour, minute).also {
            _monthlyDayScheduleRecords[scheduleId] = it
        }
    }

    fun createMonthlyWeekScheduleRecord(scheduleId: Int, dayOfMonth: Int, dayOfWeek: DayOfWeek, beginningOfMonth: Boolean, time: Time): MonthlyWeekScheduleRecord {
        check(!_singleScheduleRecords.containsKey(scheduleId))
        check(!_dailyScheduleRecords.containsKey(scheduleId))
        check(!_weeklyScheduleRecords.containsKey(scheduleId))
        check(!_monthlyDayScheduleRecords.containsKey(scheduleId))
        check(!_monthlyWeekScheduleRecords.containsKey(scheduleId))

        val (customTimeId, hour, minute) = time.timePair.destructureLocal(DomainFactory.instance)

        return MonthlyWeekScheduleRecord(false, scheduleId, dayOfMonth, dayOfWeek.ordinal, beginningOfMonth, customTimeId, hour, minute).also {
            _monthlyWeekScheduleRecords[scheduleId] = it
        }
    }

    fun createInstanceRecord(localTask: LocalTask, scheduleDate: Date, scheduleTimePair: TimePair, now: ExactTimeStamp): LocalInstanceRecord {
        val (customTimeId, hour, minute) = scheduleTimePair.destructureLocal(DomainFactory.instance)

        val id = ++instanceMaxId

        return LocalInstanceRecord(false, id, localTask.id, null, scheduleDate.year, scheduleDate.month, scheduleDate.day, customTimeId, hour, minute, null, null, null, null, null, null, now.long, false, false, null).also {
            _localInstanceRecords.add(it)
        }
    }

    fun save(source: SaveService.Source) = SaveService.Factory.instance.startService(this, source)

    fun createInstanceShownRecord(remoteTaskId: String, scheduleDate: Date, remoteCustomTimeId: RemoteCustomTimeId?, hour: Int?, minute: Int?, projectId: String): InstanceShownRecord {
        check(remoteTaskId.isNotEmpty())
        check(projectId.isNotEmpty())

        val id = ++instanceShownMaxId

        return InstanceShownRecord(false, id, remoteTaskId, scheduleDate.year, scheduleDate.month, scheduleDate.day, remoteCustomTimeId?.value, hour, minute, false, false, projectId).also {
            _instanceShownRecords.add(it)
        }
    }

    fun deleteInstanceShownRecords(taskKeys: Set<TaskKey>) {
        val remove = _instanceShownRecords.filterNot { instanceShownRecord -> taskKeys.any { taskKey -> instanceShownRecord.projectId == taskKey.remoteProjectId && instanceShownRecord.taskId == taskKey.remoteTaskId } }

        remove.forEach { it.delete() }
    }
}