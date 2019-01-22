package com.krystianwsul.checkme.domainmodel.local

import android.annotation.SuppressLint
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.persistencemodel.*
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import java.util.*

@SuppressLint("UseSparseArrays")
class LocalFactory(private val persistenceManager: PersistenceManager = PersistenceManager.instance) {

    private val _localCustomTimes = HashMap<Int, LocalCustomTime>()

    private val localTasks = HashMap<Int, LocalTask>()

    private val localTaskHierarchies = TaskHierarchyContainer<Int, LocalTaskHierarchy>()

    private val existingLocalInstances = InstanceMap<LocalInstance>()

    val instanceShownRecords: Collection<InstanceShownRecord>
        get() = persistenceManager.instanceShownRecords

    val tasks: Collection<LocalTask>
        get() = localTasks.values

    val localCustomTimes: Collection<LocalCustomTime>
        get() = _localCustomTimes.values

    val currentCustomTimes get() = _localCustomTimes.values.filter { it.current }

    val instanceCount get() = existingLocalInstances.size()

    val existingInstances: List<LocalInstance>
        get() = existingLocalInstances.values()

    val taskIds get() = localTasks.keys

    val taskCount get() = localTasks.size

    val uuid get() = persistenceManager.uuid

    private lateinit var domainFactory: DomainFactory

    fun initialize(domainFactory: DomainFactory) {
        this.domainFactory = domainFactory
        
        _localCustomTimes.putAll(persistenceManager.customTimeRecords
                .map { LocalCustomTime(domainFactory, it) }
                .map { it.id to it })

        persistenceManager.taskRecords.forEach { taskRecord ->
            LocalTask(domainFactory, taskRecord).let {
                it.addSchedules(loadSchedules(taskRecord.id))

                localTasks[it.id] = it
            }
        }

        persistenceManager.taskHierarchyRecords
                .map { LocalTaskHierarchy(domainFactory, it) }
                .forEach { localTaskHierarchies.add(it.id, it) }

        persistenceManager.localInstanceRecords
                .map { LocalInstance(domainFactory, it) }
                .forEach { existingLocalInstances.add(it) }
    }

    private fun loadSchedules(localTaskId: Int) = persistenceManager.getScheduleRecords(localTaskId).map {
        check(it.type >= 0)
        check(it.type < ScheduleType.values().size)

        val scheduleType = ScheduleType.values()[it.type]

        when (scheduleType) {
            ScheduleType.SINGLE -> loadSingleSchedule(it)
            ScheduleType.DAILY -> loadDailySchedule(it)
            ScheduleType.WEEKLY -> loadWeeklySchedule(it)
            ScheduleType.MONTHLY_DAY -> loadMonthlyDaySchedule(it)
            ScheduleType.MONTHLY_WEEK -> loadMonthlyWeekSchedule(it)
        }
    }

    private fun loadSingleSchedule(scheduleRecord: ScheduleRecord): Schedule {
        val singleScheduleRecord = persistenceManager.getSingleScheduleRecord(scheduleRecord.id)

        return SingleSchedule(domainFactory, LocalSingleScheduleBridge(scheduleRecord, singleScheduleRecord))
    }

    private fun loadDailySchedule(scheduleRecord: ScheduleRecord): WeeklySchedule {
        val dailyScheduleRecord = persistenceManager.getDailyScheduleRecord(scheduleRecord.id)

        return WeeklySchedule(domainFactory, LocalDailyScheduleBridge(scheduleRecord, dailyScheduleRecord))
    }

    private fun loadWeeklySchedule(scheduleRecord: ScheduleRecord): WeeklySchedule {
        val weeklyScheduleRecord = persistenceManager.getWeeklyScheduleRecord(scheduleRecord.id)

        return WeeklySchedule(domainFactory, LocalWeeklyScheduleBridge(scheduleRecord, weeklyScheduleRecord))
    }

    private fun loadMonthlyDaySchedule(scheduleRecord: ScheduleRecord): MonthlyDaySchedule {
        val monthlyDayScheduleRecord = persistenceManager.getMonthlyDayScheduleRecord(scheduleRecord.id)

        return MonthlyDaySchedule(domainFactory, LocalMonthlyDayScheduleBridge(scheduleRecord, monthlyDayScheduleRecord))
    }

    private fun loadMonthlyWeekSchedule(scheduleRecord: ScheduleRecord): MonthlyWeekSchedule {
        val monthlyWeekScheduleRecord = persistenceManager.getMonthlyWeekScheduleRecord(scheduleRecord.id)

        return MonthlyWeekSchedule(domainFactory, LocalMonthlyWeekScheduleBridge(scheduleRecord, monthlyWeekScheduleRecord))
    }

    fun save(source: SaveService.Source): Boolean = persistenceManager.save(source)

    fun getInstanceShownRecord(projectId: String, taskId: String, scheduleYear: Int, scheduleMonth: Int, scheduleDay: Int, scheduleCustomTimeId: String?, scheduleHour: Int?, scheduleMinute: Int?): InstanceShownRecord? {
        val matches: List<InstanceShownRecord>
        if (scheduleCustomTimeId != null) {
            check(scheduleHour == null)
            check(scheduleMinute == null)

            matches = persistenceManager.instanceShownRecords
                    .filter { it.projectId == projectId }
                    .filter { it.taskId == taskId }
                    .filter { it.scheduleYear == scheduleYear }
                    .filter { it.scheduleMonth == scheduleMonth }
                    .filter { it.scheduleDay == scheduleDay }
                    .filter { it.scheduleCustomTimeId == scheduleCustomTimeId }
        } else {
            checkNotNull(scheduleHour)
            checkNotNull(scheduleMinute)

            matches = persistenceManager.instanceShownRecords
                    .filter { it.projectId == projectId }
                    .filter { it.taskId == taskId }
                    .filter { it.scheduleYear == scheduleYear }
                    .filter { it.scheduleMonth == scheduleMonth }
                    .filter { it.scheduleDay == scheduleDay }
                    .filter { it.scheduleHour == scheduleHour }
                    .filter { it.scheduleMinute == scheduleMinute }
        }

        return matches.singleOrNull()
    }

    fun createInstanceShownRecord(remoteTaskId: String, scheduleDateTime: DateTime, projectId: String): InstanceShownRecord {
        val (remoteCustomTimeId, hour, minute) = scheduleDateTime.time
                .timePair
                .destructureRemote(domainFactory, projectId)

        return persistenceManager.createInstanceShownRecord(remoteTaskId, scheduleDateTime.date, remoteCustomTimeId, hour, minute, projectId)
    }

    fun deleteTask(localTask: LocalTask) {
        check(localTasks.containsKey(localTask.id))

        localTasks.remove(localTask.id)
    }

    fun deleteTaskHierarchy(localTaskHierarchy: LocalTaskHierarchy) {
        localTaskHierarchies.removeForce(localTaskHierarchy.id)
    }

    fun deleteInstance(localInstance: LocalInstance) {
        existingLocalInstances.removeForce(localInstance)
    }

    fun deleteCustomTime(localCustomTime: LocalCustomTime) {
        check(_localCustomTimes.containsKey(localCustomTime.id))

        _localCustomTimes.remove(localCustomTime.id)
    }

    fun deleteInstanceShownRecords(taskKeys: Set<TaskKey>) {
        persistenceManager.deleteInstanceShownRecords(taskKeys)
    }

    fun createScheduleRootTask(now: ExactTimeStamp, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?): LocalTask {
        check(name.isNotEmpty())
        check(!scheduleDatas.isEmpty())

        val rootLocalTask = createLocalTaskHelper(name, now, note)

        val schedules = createSchedules(rootLocalTask, scheduleDatas, now)
        check(!schedules.isEmpty())

        rootLocalTask.addSchedules(schedules)

        return rootLocalTask
    }

    fun createLocalTaskHelper(name: String, now: ExactTimeStamp, note: String?): LocalTask {
        check(name.isNotEmpty())

        val taskRecord = persistenceManager.createTaskRecord(name, now, note)

        val rootLocalTask = LocalTask(domainFactory, taskRecord)

        check(!localTasks.containsKey(rootLocalTask.id))
        localTasks[rootLocalTask.id] = rootLocalTask

        return rootLocalTask
    }

    fun createSchedules(rootLocalTask: LocalTask, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, startExactTimeStamp: ExactTimeStamp): List<Schedule> {
        check(!scheduleDatas.isEmpty())
        check(rootLocalTask.current(startExactTimeStamp))

        return scheduleDatas.map { scheduleData ->
            when (scheduleData.scheduleType) {
                ScheduleType.SINGLE -> {
                    val (date, timePair) = scheduleData as CreateTaskViewModel.ScheduleData.SingleScheduleData

                    val time = domainFactory.getTime(timePair)

                    val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.SINGLE, startExactTimeStamp)

                    val singleScheduleRecord = persistenceManager.createSingleScheduleRecord(scheduleRecord.id, date, time)

                    listOf(SingleSchedule(domainFactory, LocalSingleScheduleBridge(scheduleRecord, singleScheduleRecord)))
                }
                ScheduleType.DAILY -> throw UnsupportedOperationException()
                ScheduleType.WEEKLY -> {
                    val (daysOfWeek, timePair) = scheduleData as CreateTaskViewModel.ScheduleData.WeeklyScheduleData

                    val time = domainFactory.getTime(timePair)

                    daysOfWeek.map { dayOfWeek ->
                        val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.WEEKLY, startExactTimeStamp)

                        val weeklyScheduleRecord = persistenceManager.createWeeklyScheduleRecord(scheduleRecord.id, dayOfWeek, time)

                        WeeklySchedule(domainFactory, LocalWeeklyScheduleBridge(scheduleRecord, weeklyScheduleRecord))
                    }
                }
                ScheduleType.MONTHLY_DAY -> {
                    val (dayOfMonth, beginningOfMonth, timePair) = scheduleData as CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData

                    val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.MONTHLY_DAY, startExactTimeStamp)

                    val monthlyDayScheduleRecord = persistenceManager.createMonthlyDayScheduleRecord(scheduleRecord.id, dayOfMonth, beginningOfMonth, domainFactory.getTime(timePair))

                    listOf(MonthlyDaySchedule(domainFactory, LocalMonthlyDayScheduleBridge(scheduleRecord, monthlyDayScheduleRecord)))
                }
                ScheduleType.MONTHLY_WEEK -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth, TimePair) = scheduleData as CreateTaskViewModel.ScheduleData.MonthlyWeekScheduleData

                    val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.MONTHLY_WEEK, startExactTimeStamp)

                    val monthlyWeekScheduleRecord = persistenceManager.createMonthlyWeekScheduleRecord(scheduleRecord.id, dayOfMonth, dayOfWeek, beginningOfMonth, domainFactory.getTime(TimePair))

                    listOf(MonthlyWeekSchedule(domainFactory, LocalMonthlyWeekScheduleBridge(scheduleRecord, monthlyWeekScheduleRecord)))
                }
            }
        }.flatten()
    }

    fun createTaskHierarchy(parentLocalTask: LocalTask, childLocalTask: LocalTask, startExactTimeStamp: ExactTimeStamp) {
        check(parentLocalTask.current(startExactTimeStamp))
        check(childLocalTask.current(startExactTimeStamp))

        val taskHierarchyRecord = persistenceManager.createTaskHierarchyRecord(parentLocalTask, childLocalTask, startExactTimeStamp)

        val localTaskHierarchy = LocalTaskHierarchy(domainFactory, taskHierarchyRecord)
        localTaskHierarchies.add(localTaskHierarchy.id, localTaskHierarchy)
    }

    fun createChildTask(now: ExactTimeStamp, parentTask: LocalTask, name: String, note: String?): LocalTask {
        check(name.isNotEmpty())
        check(parentTask.current(now))

        return createLocalTaskHelper(name, now, note).also {
            createTaskHierarchy(parentTask, it, now)
        }
    }

    fun createInstanceRecord(localTask: LocalTask, localInstance: LocalInstance, scheduleDate: Date, scheduleTimePair: TimePair, now: ExactTimeStamp): LocalInstanceRecord {
        existingLocalInstances.add(localInstance)

        return persistenceManager.createInstanceRecord(localTask, scheduleDate, scheduleTimePair, now)
    }

    fun convertLocalToRemoteHelper(localToRemoteConversion: LocalToRemoteConversion, localTask: LocalTask) {
        if (localToRemoteConversion.localTasks.containsKey(localTask.id))
            return

        val taskKey = localTask.taskKey

        localToRemoteConversion.localTasks[localTask.id] = Pair(localTask, ArrayList(existingLocalInstances[taskKey].values))

        val parentLocalTaskHierarchies = localTaskHierarchies.getByChildTaskKey(taskKey)

        localToRemoteConversion.localTaskHierarchies.addAll(parentLocalTaskHierarchies)

        localTaskHierarchies.getByParentTaskKey(taskKey)
                .map { it.childTask }
                .forEach { convertLocalToRemoteHelper(localToRemoteConversion, it) }

        parentLocalTaskHierarchies.map { it.parentTask }.forEach { convertLocalToRemoteHelper(localToRemoteConversion, it) }
    }

    fun createLocalCustomTime(name: String, hourMinutes: Map<DayOfWeek, HourMinute>): LocalCustomTime {
        check(name.isNotEmpty())

        checkNotNull(hourMinutes[DayOfWeek.SUNDAY])
        checkNotNull(hourMinutes[DayOfWeek.MONDAY])
        checkNotNull(hourMinutes[DayOfWeek.TUESDAY])
        checkNotNull(hourMinutes[DayOfWeek.WEDNESDAY])
        checkNotNull(hourMinutes[DayOfWeek.THURSDAY])
        checkNotNull(hourMinutes[DayOfWeek.FRIDAY])
        checkNotNull(hourMinutes[DayOfWeek.SATURDAY])

        val localCustomTimeRecord = persistenceManager.createCustomTimeRecord(name, hourMinutes)

        val localCustomTime = LocalCustomTime(domainFactory, localCustomTimeRecord)
        check(!_localCustomTimes.containsKey(localCustomTime.id))

        _localCustomTimes[localCustomTime.id] = localCustomTime

        return localCustomTime
    }

    fun getLocalCustomTime(localCustomTimeId: Int): LocalCustomTime {
        check(_localCustomTimes.containsKey(localCustomTimeId))

        return _localCustomTimes[localCustomTimeId]!!
    }

    fun removeRemoteCustomTimeRecords(projectId: String) = _localCustomTimes.values.forEach { it.removeRemoteRecord(projectId) }

    fun getLocalCustomTime(remoteProjectId: String, remoteCustomTimeId: String) = _localCustomTimes.values.singleOrNull { it.hasRemoteRecord(remoteProjectId) && it.getRemoteId(remoteProjectId) == remoteCustomTimeId }

    fun hasLocalCustomTime(localCustomTimeId: Int) = _localCustomTimes.containsKey(localCustomTimeId)

    fun getExistingInstances(taskKey: TaskKey) = existingLocalInstances[taskKey]

    fun getExistingInstanceIfPresent(instanceKey: InstanceKey) = existingLocalInstances.getIfPresent(instanceKey)

    fun getTaskForce(taskId: Int) = localTasks[taskId]!!

    fun getTaskHierarchy(localTaskHierarchyKey: TaskHierarchyKey.LocalTaskHierarchyKey) = localTaskHierarchies.getById(localTaskHierarchyKey.id)

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey) = localTaskHierarchies.getByChildTaskKey(childTaskKey)

    fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey) = localTaskHierarchies.getByParentTaskKey(parentTaskKey)

    fun getSchedule(scheduleId: ScheduleId.Local): Schedule {
        for (localTask in localTasks.values)
            for (schedule in localTask.schedules)
                if (schedule.scheduleId == scheduleId)
                    return schedule

        throw IllegalArgumentException()
    }
}
