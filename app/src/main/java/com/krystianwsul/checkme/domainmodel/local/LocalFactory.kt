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
class LocalFactory {

    companion object {

        private var sLocalFactory: LocalFactory? = null

        val instance: LocalFactory
            get() {
                if (sLocalFactory == null)
                    sLocalFactory = LocalFactory()
                return sLocalFactory!!
            }
    }

    private val persistenceManager: PersistenceManger

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

    private constructor() {
        persistenceManager = PersistenceManger.instance
    }

    constructor(persistenceManger: PersistenceManger) {
        persistenceManager = persistenceManger
    }

    fun reset() {
        sLocalFactory = null

        persistenceManager.reset()
    }

    fun initialize(kotlinDomainFactory: KotlinDomainFactory) {
        _localCustomTimes.putAll(persistenceManager.customTimeRecords
                .map { LocalCustomTime(kotlinDomainFactory, it) }
                .map { it.id to it })

        persistenceManager.taskRecords.forEach { taskRecord ->
            LocalTask(kotlinDomainFactory, taskRecord).let {
                it.addSchedules(loadSchedules(kotlinDomainFactory, taskRecord.id))

                localTasks[it.id] = it
            }
        }

        persistenceManager.taskHierarchyRecords
                .map { LocalTaskHierarchy(kotlinDomainFactory, it) }
                .forEach { localTaskHierarchies.add(it.id, it) }

        persistenceManager.instanceRecords
                .map { LocalInstance(kotlinDomainFactory, it) }
                .forEach { existingLocalInstances.add(it) }
    }

    private fun loadSchedules(kotlinDomainFactory: KotlinDomainFactory, localTaskId: Int) = persistenceManager.getScheduleRecords(localTaskId).map {
        check(it.type >= 0)
        check(it.type < ScheduleType.values().size)

        val scheduleType = ScheduleType.values()[it.type]

        when (scheduleType) {
            ScheduleType.SINGLE -> loadSingleSchedule(kotlinDomainFactory, it)
            ScheduleType.DAILY -> loadDailySchedule(kotlinDomainFactory, it)
            ScheduleType.WEEKLY -> loadWeeklySchedule(kotlinDomainFactory, it)
            ScheduleType.MONTHLY_DAY -> loadMonthlyDaySchedule(kotlinDomainFactory, it)
            ScheduleType.MONTHLY_WEEK -> loadMonthlyWeekSchedule(kotlinDomainFactory, it)
        }
    }

    private fun loadSingleSchedule(kotlinDomainFactory: KotlinDomainFactory, scheduleRecord: ScheduleRecord): Schedule {
        val singleScheduleRecord = persistenceManager.getSingleScheduleRecord(scheduleRecord.id)

        return SingleSchedule(kotlinDomainFactory, LocalSingleScheduleBridge(scheduleRecord, singleScheduleRecord))
    }

    private fun loadDailySchedule(kotlinDomainFactory: KotlinDomainFactory, scheduleRecord: ScheduleRecord): WeeklySchedule {
        val dailyScheduleRecord = persistenceManager.getDailyScheduleRecord(scheduleRecord.id)

        return WeeklySchedule(kotlinDomainFactory, LocalDailyScheduleBridge(scheduleRecord, dailyScheduleRecord))
    }

    private fun loadWeeklySchedule(kotlinDomainFactory: KotlinDomainFactory, scheduleRecord: ScheduleRecord): WeeklySchedule {
        val weeklyScheduleRecord = persistenceManager.getWeeklyScheduleRecord(scheduleRecord.id)

        return WeeklySchedule(kotlinDomainFactory, LocalWeeklyScheduleBridge(scheduleRecord, weeklyScheduleRecord))
    }

    private fun loadMonthlyDaySchedule(kotlinDomainFactory: KotlinDomainFactory, scheduleRecord: ScheduleRecord): MonthlyDaySchedule {
        val monthlyDayScheduleRecord = persistenceManager.getMonthlyDayScheduleRecord(scheduleRecord.id)

        return MonthlyDaySchedule(kotlinDomainFactory, LocalMonthlyDayScheduleBridge(scheduleRecord, monthlyDayScheduleRecord))
    }

    private fun loadMonthlyWeekSchedule(kotlinDomainFactory: KotlinDomainFactory, scheduleRecord: ScheduleRecord): MonthlyWeekSchedule {
        val monthlyWeekScheduleRecord = persistenceManager.getMonthlyWeekScheduleRecord(scheduleRecord.id)

        return MonthlyWeekSchedule(kotlinDomainFactory, LocalMonthlyWeekScheduleBridge(scheduleRecord, monthlyWeekScheduleRecord))
    }

    fun save(source: SaveService.Source) = persistenceManager.save(source)

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

    fun createInstanceShownRecord(kotlinDomainFactory: KotlinDomainFactory, remoteTaskId: String, scheduleDateTime: DateTime, projectId: String): InstanceShownRecord {
        val timePair = scheduleDateTime.time.timePair

        val remoteCustomTimeId: String?
        val hour: Int?
        val minute: Int?
        if (timePair.hourMinute != null) {
            check(timePair.customTimeKey == null)

            remoteCustomTimeId = null

            hour = timePair.hourMinute.hour
            minute = timePair.hourMinute.minute
        } else {
            remoteCustomTimeId = kotlinDomainFactory.getRemoteCustomTimeId(projectId, timePair.customTimeKey!!)

            hour = null
            minute = null
        }

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

    // todo dont pass domainfactory
    fun createScheduleRootTask(kotlinDomainFactory: KotlinDomainFactory, now: ExactTimeStamp, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?): LocalTask {
        check(name.isNotEmpty())
        check(!scheduleDatas.isEmpty())

        val rootLocalTask = createLocalTaskHelper(kotlinDomainFactory, name, now, note)

        val schedules = createSchedules(kotlinDomainFactory, rootLocalTask, scheduleDatas, now)
        check(!schedules.isEmpty())

        rootLocalTask.addSchedules(schedules)

        return rootLocalTask
    }

    fun createLocalTaskHelper(kotlinDomainFactory: KotlinDomainFactory, name: String, now: ExactTimeStamp, note: String?): LocalTask {
        check(name.isNotEmpty())

        val taskRecord = persistenceManager.createTaskRecord(name, now, note)

        val rootLocalTask = LocalTask(kotlinDomainFactory, taskRecord)

        check(!localTasks.containsKey(rootLocalTask.id))
        localTasks[rootLocalTask.id] = rootLocalTask

        return rootLocalTask
    }

    fun createSchedules(kotlinDomainFactory: KotlinDomainFactory, rootLocalTask: LocalTask, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, startExactTimeStamp: ExactTimeStamp): List<Schedule> {
        check(!scheduleDatas.isEmpty())
        check(rootLocalTask.current(startExactTimeStamp))

        return scheduleDatas.map { scheduleData ->
            when (scheduleData.scheduleType) {
                ScheduleType.SINGLE -> {
                    val (date, timePair) = scheduleData as CreateTaskViewModel.ScheduleData.SingleScheduleData

                    val time = kotlinDomainFactory.getTime(timePair)

                    val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.SINGLE, startExactTimeStamp)

                    val singleScheduleRecord = persistenceManager.createSingleScheduleRecord(scheduleRecord.id, date, time)

                    listOf(SingleSchedule(kotlinDomainFactory, LocalSingleScheduleBridge(scheduleRecord, singleScheduleRecord)))
                }
                ScheduleType.DAILY -> throw UnsupportedOperationException()
                ScheduleType.WEEKLY -> {
                    val (daysOfWeek, timePair) = scheduleData as CreateTaskViewModel.ScheduleData.WeeklyScheduleData

                    val time = kotlinDomainFactory.getTime(timePair)

                    daysOfWeek.map { dayOfWeek ->
                        val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.WEEKLY, startExactTimeStamp)

                        val weeklyScheduleRecord = persistenceManager.createWeeklyScheduleRecord(scheduleRecord.id, dayOfWeek, time)

                        WeeklySchedule(kotlinDomainFactory, LocalWeeklyScheduleBridge(scheduleRecord, weeklyScheduleRecord))
                    }
                }
                ScheduleType.MONTHLY_DAY -> {
                    val (dayOfMonth, beginningOfMonth, timePair) = scheduleData as CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData

                    val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.MONTHLY_DAY, startExactTimeStamp)

                    val monthlyDayScheduleRecord = persistenceManager.createMonthlyDayScheduleRecord(scheduleRecord.id, dayOfMonth, beginningOfMonth, kotlinDomainFactory.getTime(timePair))

                    listOf(MonthlyDaySchedule(kotlinDomainFactory, LocalMonthlyDayScheduleBridge(scheduleRecord, monthlyDayScheduleRecord)))
                }
                ScheduleType.MONTHLY_WEEK -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth, TimePair) = scheduleData as CreateTaskViewModel.ScheduleData.MonthlyWeekScheduleData

                    val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.MONTHLY_WEEK, startExactTimeStamp)

                    val monthlyWeekScheduleRecord = persistenceManager.createMonthlyWeekScheduleRecord(scheduleRecord.id, dayOfMonth, dayOfWeek, beginningOfMonth, kotlinDomainFactory.getTime(TimePair))

                    listOf(MonthlyWeekSchedule(kotlinDomainFactory, LocalMonthlyWeekScheduleBridge(scheduleRecord, monthlyWeekScheduleRecord)))
                }
            }
        }.flatten()
    }

    fun createTaskHierarchy(kotlinDomainFactory: KotlinDomainFactory, parentLocalTask: LocalTask, childLocalTask: LocalTask, startExactTimeStamp: ExactTimeStamp) {
        check(parentLocalTask.current(startExactTimeStamp))
        check(childLocalTask.current(startExactTimeStamp))

        val taskHierarchyRecord = persistenceManager.createTaskHierarchyRecord(parentLocalTask, childLocalTask, startExactTimeStamp)

        val localTaskHierarchy = LocalTaskHierarchy(kotlinDomainFactory, taskHierarchyRecord)
        localTaskHierarchies.add(localTaskHierarchy.id, localTaskHierarchy)
    }

    fun createChildTask(kotlinDomainFactory: KotlinDomainFactory, now: ExactTimeStamp, parentTask: LocalTask, name: String, note: String?): LocalTask {
        check(name.isNotEmpty())
        check(parentTask.current(now))

        return createLocalTaskHelper(kotlinDomainFactory, name, now, note).also {
            createTaskHierarchy(kotlinDomainFactory, parentTask, it, now)
        }
    }

    fun createInstanceRecord(localTask: LocalTask, localInstance: LocalInstance, scheduleDate: Date, scheduleTimePair: TimePair, now: ExactTimeStamp): InstanceRecord {
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

    fun createLocalCustomTime(kotlinDomainFactory: KotlinDomainFactory, name: String, hourMinutes: Map<DayOfWeek, HourMinute>): LocalCustomTime {
        check(name.isNotEmpty())

        checkNotNull(hourMinutes[DayOfWeek.SUNDAY])
        checkNotNull(hourMinutes[DayOfWeek.MONDAY])
        checkNotNull(hourMinutes[DayOfWeek.TUESDAY])
        checkNotNull(hourMinutes[DayOfWeek.WEDNESDAY])
        checkNotNull(hourMinutes[DayOfWeek.THURSDAY])
        checkNotNull(hourMinutes[DayOfWeek.FRIDAY])
        checkNotNull(hourMinutes[DayOfWeek.SATURDAY])

        val localCustomTimeRecord = persistenceManager.createCustomTimeRecord(name, hourMinutes)

        val localCustomTime = LocalCustomTime(kotlinDomainFactory, localCustomTimeRecord)
        check(!_localCustomTimes.containsKey(localCustomTime.id))

        _localCustomTimes[localCustomTime.id] = localCustomTime

        return localCustomTime
    }

    fun getLocalCustomTime(localCustomTimeId: Int): LocalCustomTime {
        check(_localCustomTimes.containsKey(localCustomTimeId))

        return _localCustomTimes[localCustomTimeId]!!
    }

    fun clearRemoteCustomTimeRecords() = _localCustomTimes.values.forEach { it.clearRemoteRecords() }

    fun getLocalCustomTime(remoteProjectId: String, remoteCustomTimeId: String) = _localCustomTimes.values.singleOrNull { it.hasRemoteRecord(remoteProjectId) && it.getRemoteId(remoteProjectId) == remoteCustomTimeId }

    fun hasLocalCustomTime(localCustomTimeId: Int) = _localCustomTimes.containsKey(localCustomTimeId)

    fun getExistingInstances(taskKey: TaskKey) = existingLocalInstances[taskKey]

    fun getExistingInstanceIfPresent(instanceKey: InstanceKey) = existingLocalInstances.getIfPresent(instanceKey)

    fun getTaskForce(taskId: Int) = localTasks[taskId]!!

    fun getTaskIfPresent(taskId: Int) = localTasks[taskId]

    fun getTaskHierarchy(localTaskHierarchyKey: TaskHierarchyKey.LocalTaskHierarchyKey) = localTaskHierarchies.getById(localTaskHierarchyKey.id)

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey) = localTaskHierarchies.getByChildTaskKey(childTaskKey)

    fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey) = localTaskHierarchies.getByParentTaskKey(parentTaskKey)
}
