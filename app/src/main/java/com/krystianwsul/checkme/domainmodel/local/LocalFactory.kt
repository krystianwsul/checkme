package com.krystianwsul.checkme.domainmodel.local

import android.annotation.SuppressLint
import android.content.Context
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.loaders.CreateTaskLoader
import com.krystianwsul.checkme.persistencemodel.*
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import junit.framework.Assert
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

    fun initialize(domainFactory: DomainFactory) {
        _localCustomTimes.putAll(persistenceManager.customTimeRecords
                .map { LocalCustomTime(domainFactory, it) }
                .map { it.id to it })

        persistenceManager.taskRecords.forEach { taskRecord ->
            LocalTask(domainFactory, taskRecord).let {
                it.addSchedules(loadSchedules(domainFactory, taskRecord.id))

                localTasks[it.id] = it
            }
        }

        persistenceManager.taskHierarchyRecords
                .map { LocalTaskHierarchy(domainFactory, it) }
                .forEach { localTaskHierarchies.add(it.id, it) }

        persistenceManager.instanceRecords
                .map { LocalInstance(domainFactory, it) }
                .forEach { existingLocalInstances.add(it) }
    }

    private fun loadSchedules(domainFactory: DomainFactory, localTaskId: Int): List<Schedule> {
        val scheduleRecords = persistenceManager.getScheduleRecords(localTaskId)

        val schedules = mutableListOf<Schedule>()

        for (scheduleRecord in scheduleRecords) {
            Assert.assertTrue(scheduleRecord.type >= 0)
            Assert.assertTrue(scheduleRecord.type < ScheduleType.values().size)

            val scheduleType = ScheduleType.values()[scheduleRecord.type]

            when (scheduleType) {
                ScheduleType.SINGLE -> schedules.add(loadSingleSchedule(domainFactory, scheduleRecord))
                ScheduleType.DAILY -> schedules.add(loadDailySchedule(domainFactory, scheduleRecord))
                ScheduleType.WEEKLY -> schedules.add(loadWeeklySchedule(domainFactory, scheduleRecord))
                ScheduleType.MONTHLY_DAY -> schedules.add(loadMonthlyDaySchedule(domainFactory, scheduleRecord))
                ScheduleType.MONTHLY_WEEK -> schedules.add(loadMonthlyWeekSchedule(domainFactory, scheduleRecord))
            }
        }

        return schedules
    }

    private fun loadSingleSchedule(domainFactory: DomainFactory, scheduleRecord: ScheduleRecord): Schedule {
        val singleScheduleRecord = persistenceManager.getSingleScheduleRecord(scheduleRecord.id)

        return SingleSchedule(domainFactory, LocalSingleScheduleBridge(scheduleRecord, singleScheduleRecord))
    }

    private fun loadDailySchedule(domainFactory: DomainFactory, scheduleRecord: ScheduleRecord): WeeklySchedule {
        val dailyScheduleRecord = persistenceManager.getDailyScheduleRecord(scheduleRecord.id)

        return WeeklySchedule(domainFactory, LocalDailyScheduleBridge(scheduleRecord, dailyScheduleRecord))
    }

    private fun loadWeeklySchedule(domainFactory: DomainFactory, scheduleRecord: ScheduleRecord): WeeklySchedule {
        val weeklyScheduleRecord = persistenceManager.getWeeklyScheduleRecord(scheduleRecord.id)

        return WeeklySchedule(domainFactory, LocalWeeklyScheduleBridge(scheduleRecord, weeklyScheduleRecord))
    }

    private fun loadMonthlyDaySchedule(domainFactory: DomainFactory, scheduleRecord: ScheduleRecord): MonthlyDaySchedule {
        val monthlyDayScheduleRecord = persistenceManager.getMonthlyDayScheduleRecord(scheduleRecord.id)

        return MonthlyDaySchedule(domainFactory, LocalMonthlyDayScheduleBridge(scheduleRecord, monthlyDayScheduleRecord))
    }

    private fun loadMonthlyWeekSchedule(domainFactory: DomainFactory, scheduleRecord: ScheduleRecord): MonthlyWeekSchedule {
        val monthlyWeekScheduleRecord = persistenceManager.getMonthlyWeekScheduleRecord(scheduleRecord.id)

        return MonthlyWeekSchedule(domainFactory, LocalMonthlyWeekScheduleBridge(scheduleRecord, monthlyWeekScheduleRecord))
    }

    fun save(context: Context, source: SaveService.Source) {
        persistenceManager.save(context, source)
    }

    fun getInstanceShownRecord(projectId: String, taskId: String, scheduleYear: Int, scheduleMonth: Int, scheduleDay: Int, scheduleCustomTimeId: String?, scheduleHour: Int?, scheduleMinute: Int?): InstanceShownRecord? {
        val matches: List<InstanceShownRecord>
        if (scheduleCustomTimeId != null) {
            Assert.assertTrue(scheduleHour == null)
            Assert.assertTrue(scheduleMinute == null)

            matches = persistenceManager.instanceShownRecords
                    .filter { it.projectId == projectId }
                    .filter { it.taskId == taskId }
                    .filter { it.scheduleYear == scheduleYear }
                    .filter { it.scheduleMonth == scheduleMonth }
                    .filter { it.scheduleDay == scheduleDay }
                    .filter { it.scheduleCustomTimeId == scheduleCustomTimeId }
        } else {
            Assert.assertTrue(scheduleHour != null)
            Assert.assertTrue(scheduleMinute != null)

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

    fun createInstanceShownRecord(domainFactory: DomainFactory, remoteTaskId: String, scheduleDateTime: DateTime, projectId: String): InstanceShownRecord {
        val timePair = scheduleDateTime.time.timePair

        val remoteCustomTimeId: String?
        val hour: Int?
        val minute: Int?
        if (timePair.mHourMinute != null) {
            Assert.assertTrue(timePair.mCustomTimeKey == null)

            remoteCustomTimeId = null

            hour = timePair.mHourMinute.hour
            minute = timePair.mHourMinute.minute
        } else {
            Assert.assertTrue(timePair.mCustomTimeKey != null)

            remoteCustomTimeId = domainFactory.getRemoteCustomTimeId(projectId, timePair.mCustomTimeKey!!)

            hour = null
            minute = null
        }

        return persistenceManager.createInstanceShownRecord(remoteTaskId, scheduleDateTime.date, remoteCustomTimeId, hour, minute, projectId)
    }

    fun deleteTask(localTask: LocalTask) {
        Assert.assertTrue(localTasks.containsKey(localTask.id))

        localTasks.remove(localTask.id)
    }

    fun deleteTaskHierarchy(localTaskHierarchy: LocalTaskHierarchy) {
        localTaskHierarchies.removeForce(localTaskHierarchy.id)
    }

    fun deleteInstance(localInstance: LocalInstance) {
        existingLocalInstances.removeForce(localInstance)
    }

    fun deleteCustomTime(localCustomTime: LocalCustomTime) {
        Assert.assertTrue(_localCustomTimes.containsKey(localCustomTime.id))

        _localCustomTimes.remove(localCustomTime.id)
    }

    fun deleteInstanceShownRecords(taskKeys: Set<TaskKey>) {
        persistenceManager.deleteInstanceShownRecords(taskKeys)
    }

    fun createScheduleRootTask(domainFactory: DomainFactory, now: ExactTimeStamp, name: String, scheduleDatas: List<CreateTaskLoader.ScheduleData>, note: String?): LocalTask {
        Assert.assertTrue(name.isNotEmpty())
        Assert.assertTrue(!scheduleDatas.isEmpty())

        val rootLocalTask = createLocalTaskHelper(domainFactory, name, now, note)

        val schedules = createSchedules(domainFactory, rootLocalTask, scheduleDatas, now)
        Assert.assertTrue(!schedules.isEmpty())

        rootLocalTask.addSchedules(schedules)

        return rootLocalTask
    }

    fun createLocalTaskHelper(domainFactory: DomainFactory, name: String, now: ExactTimeStamp, note: String?): LocalTask {
        Assert.assertTrue(name.isNotEmpty())

        val taskRecord = persistenceManager.createTaskRecord(name, now, note)

        val rootLocalTask = LocalTask(domainFactory, taskRecord)

        Assert.assertTrue(!localTasks.containsKey(rootLocalTask.id))
        localTasks[rootLocalTask.id] = rootLocalTask

        return rootLocalTask
    }

    fun createSchedules(domainFactory: DomainFactory, rootLocalTask: LocalTask, scheduleDatas: List<CreateTaskLoader.ScheduleData>, startExactTimeStamp: ExactTimeStamp): List<Schedule> {
        Assert.assertTrue(!scheduleDatas.isEmpty())
        Assert.assertTrue(rootLocalTask.current(startExactTimeStamp))

        return scheduleDatas.map { scheduleData ->
            when (scheduleData.scheduleType) {
                ScheduleType.SINGLE -> {
                    val (date, timePair) = scheduleData as CreateTaskLoader.ScheduleData.SingleScheduleData

                    val time = domainFactory.getTime(timePair)

                    val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.SINGLE, startExactTimeStamp)

                    val singleScheduleRecord = persistenceManager.createSingleScheduleRecord(scheduleRecord.id, date, time)

                    listOf(SingleSchedule(domainFactory, LocalSingleScheduleBridge(scheduleRecord, singleScheduleRecord)))
                }
                ScheduleType.DAILY -> throw UnsupportedOperationException()
                ScheduleType.WEEKLY -> {
                    val (daysOfWeek, timePair) = scheduleData as CreateTaskLoader.ScheduleData.WeeklyScheduleData

                    val time = domainFactory.getTime(timePair)

                    daysOfWeek.map { dayOfWeek ->
                        val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.WEEKLY, startExactTimeStamp)

                        val weeklyScheduleRecord = persistenceManager.createWeeklyScheduleRecord(scheduleRecord.id, dayOfWeek, time)

                        WeeklySchedule(domainFactory, LocalWeeklyScheduleBridge(scheduleRecord, weeklyScheduleRecord))
                    }
                }
                ScheduleType.MONTHLY_DAY -> {
                    val (dayOfMonth, beginningOfMonth, timePair) = scheduleData as CreateTaskLoader.ScheduleData.MonthlyDayScheduleData

                    val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.MONTHLY_DAY, startExactTimeStamp)

                    val monthlyDayScheduleRecord = persistenceManager.createMonthlyDayScheduleRecord(scheduleRecord.id, dayOfMonth, beginningOfMonth, domainFactory.getTime(timePair))

                    listOf(MonthlyDaySchedule(domainFactory, LocalMonthlyDayScheduleBridge(scheduleRecord, monthlyDayScheduleRecord)))
                }
                ScheduleType.MONTHLY_WEEK -> {
                    val (dayOfMonth, dayOfWeek, beginningOfMonth, TimePair) = scheduleData as CreateTaskLoader.ScheduleData.MonthlyWeekScheduleData

                    val scheduleRecord = persistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.MONTHLY_WEEK, startExactTimeStamp)

                    val monthlyWeekScheduleRecord = persistenceManager.createMonthlyWeekScheduleRecord(scheduleRecord.id, dayOfMonth, dayOfWeek, beginningOfMonth, domainFactory.getTime(TimePair))

                    listOf(MonthlyWeekSchedule(domainFactory, LocalMonthlyWeekScheduleBridge(scheduleRecord, monthlyWeekScheduleRecord)))
                }
            }
        }.flatten()
    }

    fun createTaskHierarchy(domainFactory: DomainFactory, parentLocalTask: LocalTask, childLocalTask: LocalTask, startExactTimeStamp: ExactTimeStamp) {
        Assert.assertTrue(parentLocalTask.current(startExactTimeStamp))
        Assert.assertTrue(childLocalTask.current(startExactTimeStamp))

        val taskHierarchyRecord = persistenceManager.createTaskHierarchyRecord(parentLocalTask, childLocalTask, startExactTimeStamp)

        val localTaskHierarchy = LocalTaskHierarchy(domainFactory, taskHierarchyRecord)
        localTaskHierarchies.add(localTaskHierarchy.id, localTaskHierarchy)
    }

    fun createChildTask(domainFactory: DomainFactory, now: ExactTimeStamp, parentTask: LocalTask, name: String, note: String?): LocalTask {
        Assert.assertTrue(name.isNotEmpty())
        Assert.assertTrue(parentTask.current(now))

        return createLocalTaskHelper(domainFactory, name, now, note).also {
            createTaskHierarchy(domainFactory, parentTask, it, now)
        }
    }

    fun createInstanceRecord(localTask: LocalTask, localInstance: LocalInstance, scheduleDate: Date, scheduleTimePair: TimePair, now: ExactTimeStamp): InstanceRecord {
        existingLocalInstances.add(localInstance)

        return persistenceManager.createInstanceRecord(localTask, scheduleDate, scheduleTimePair, now)
    }

    fun convertLocalToRemoteHelper(localToRemoteConversion: DomainFactory.LocalToRemoteConversion, localTask: LocalTask) {
        if (localToRemoteConversion.mLocalTasks.containsKey(localTask.id))
            return

        val taskKey = localTask.taskKey

        localToRemoteConversion.mLocalTasks[localTask.id] = Pair(localTask, ArrayList(existingLocalInstances.get(taskKey).values))

        val parentLocalTaskHierarchies = localTaskHierarchies.getByChildTaskKey(taskKey)

        localToRemoteConversion.mLocalTaskHierarchies.addAll(parentLocalTaskHierarchies)

        localTaskHierarchies.getByParentTaskKey(taskKey)
                .map { it.childTask }
                .forEach { convertLocalToRemoteHelper(localToRemoteConversion, it) }

        parentLocalTaskHierarchies.map { it.parentTask }.forEach { convertLocalToRemoteHelper(localToRemoteConversion, it) }
    }

    fun createLocalCustomTime(domainFactory: DomainFactory, name: String, hourMinutes: Map<DayOfWeek, HourMinute>): LocalCustomTime {
        Assert.assertTrue(name.isNotEmpty())

        Assert.assertTrue(hourMinutes[DayOfWeek.SUNDAY] != null)
        Assert.assertTrue(hourMinutes[DayOfWeek.MONDAY] != null)
        Assert.assertTrue(hourMinutes[DayOfWeek.TUESDAY] != null)
        Assert.assertTrue(hourMinutes[DayOfWeek.WEDNESDAY] != null)
        Assert.assertTrue(hourMinutes[DayOfWeek.THURSDAY] != null)
        Assert.assertTrue(hourMinutes[DayOfWeek.FRIDAY] != null)
        Assert.assertTrue(hourMinutes[DayOfWeek.SATURDAY] != null)

        val localCustomTimeRecord = persistenceManager.createCustomTimeRecord(name, hourMinutes)

        val localCustomTime = LocalCustomTime(domainFactory, localCustomTimeRecord)
        Assert.assertTrue(!_localCustomTimes.containsKey(localCustomTime.id))

        _localCustomTimes[localCustomTime.id] = localCustomTime

        return localCustomTime
    }

    fun getLocalCustomTime(localCustomTimeId: Int): LocalCustomTime {
        Assert.assertTrue(_localCustomTimes.containsKey(localCustomTimeId))

        return _localCustomTimes[localCustomTimeId]!!
    }

    fun clearRemoteCustomTimeRecords() {
        _localCustomTimes.values.forEach { it.clearRemoteRecords() }
    }

    fun getLocalCustomTime(remoteProjectId: String, remoteCustomTimeId: String) = _localCustomTimes.values.singleOrNull { it.hasRemoteRecord(remoteProjectId) && it.getRemoteId(remoteProjectId) == remoteCustomTimeId }

    fun hasLocalCustomTime(localCustomTimeId: Int) = _localCustomTimes.containsKey(localCustomTimeId)

    fun getExistingInstances(taskKey: TaskKey) = existingLocalInstances.get(taskKey)

    fun getExistingInstanceIfPresent(instanceKey: InstanceKey) = existingLocalInstances.getIfPresent(instanceKey)

    fun getTaskForce(taskId: Int) = localTasks[taskId]!!

    fun getTaskIfPresent(taskId: Int) = localTasks[taskId]

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey) = localTaskHierarchies.getByChildTaskKey(childTaskKey)

    fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey) = localTaskHierarchies.getByParentTaskKey(parentTaskKey)

}
