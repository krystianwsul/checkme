package com.krystianwsul.checkme.domainmodel

import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.local.LocalInstance
import com.krystianwsul.checkme.domainmodel.local.LocalTask
import com.krystianwsul.checkme.domainmodel.relevance.*
import com.krystianwsul.checkme.firebase.*
import com.krystianwsul.checkme.firebase.json.UserWrapper
import com.krystianwsul.checkme.firebase.records.RemoteRootUserRecord
import com.krystianwsul.checkme.gui.HierarchyData
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.persistencemodel.PersistenceManger
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskHierarchyKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.*
import java.util.*

@Suppress("LeakingThis")
open class DomainFactory(persistenceManager: PersistenceManger?) {

    companion object {

        private var _domainFactory: DomainFactory? = null

        @Synchronized
        fun getInstance(persistenceManager: PersistenceManger? = null): DomainFactory {
            if (_domainFactory == null)
                _domainFactory = DomainFactory(persistenceManager)
            return _domainFactory!!
        }

        fun mergeTickDatas(oldTickData: TickData, newTickData: TickData): TickData {
            val silent = oldTickData.silent && newTickData.silent

            val source = "merged (${oldTickData.source}, ${newTickData.source})"

            oldTickData.releaseWakelock()
            newTickData.releaseWakelock()

            val listeners = oldTickData.listeners + newTickData.listeners

            return TickData(silent, source, listeners)
        }
    }

    private var start: ExactTimeStamp
    private var read: ExactTimeStamp
    private var stop: ExactTimeStamp

    val readMillis get() = read.long - start.long
    val instantiateMillis get() = stop.long - read.long

    var userInfo: UserInfo? = null
        private set

    private var recordQuery: Query? = null
    private var recordListener: ValueEventListener? = null

    private var userQuery: Query? = null
    private var userListener: ValueEventListener? = null

    @JvmField
    var localFactory: LocalFactory

    var remoteProjectFactory: RemoteProjectFactory? = null

    var remoteRootUser: RemoteRootUser? = null

    val notTickFirebaseListeners = mutableListOf<(DomainFactory) -> Unit>()

    var tickData: TickData? = null

    private var skipSave = false

    private val lastNotificationBeeps = mutableMapOf<InstanceKey, Long>()

    init {
        start = ExactTimeStamp.now

        localFactory = persistenceManager?.let { LocalFactory(it) } ?: LocalFactory.instance

        read = ExactTimeStamp.now

        localFactory.initialize(this)

        stop = ExactTimeStamp.now
    }

    // misc

    val isHoldingWakeLock get() = tickData?.wakelock?.isHeld == true

    val taskCount get() = localFactory.taskCount + (remoteProjectFactory?.taskCount ?: 0)

    val instanceCount
        get() = localFactory.instanceCount + (remoteProjectFactory?.instanceCount ?: 0)

    val customTimeCount get() = customTimes.size

    val instanceShownCount get() = localFactory.instanceShownRecords.size

    fun save(dataId: Int, source: SaveService.Source) = save(listOf(dataId), source)

    fun save(dataIds: List<Int>, source: SaveService.Source) {
        if (skipSave)
            return

        localFactory.save(source)
        remoteProjectFactory?.save()

        ObserverHolder.notifyDomainObservers(dataIds)
    }

    @Synchronized
    fun reset(source: SaveService.Source) {
        val userInfo = userInfo
        clearUserInfo()

        _domainFactory = null
        localFactory.reset()

        userInfo?.let { setUserInfo(source, it) }

        ObserverHolder.notifyDomainObservers(ArrayList())

        ObserverHolder.clear()
    }

    // firebase

    @Synchronized
    fun setUserInfo(source: SaveService.Source, newUserInfo: UserInfo) {
        if (userInfo != null) {
            checkNotNull(recordQuery)
            checkNotNull(userQuery)

            if (userInfo == newUserInfo)
                return

            clearUserInfo()
        }

        check(userInfo == null)

        check(recordQuery == null)
        check(recordListener == null)

        check(userQuery == null)
        check(userListener == null)

        userInfo = newUserInfo

        DatabaseWrapper.setUserInfo(newUserInfo, localFactory.uuid)

        recordQuery = DatabaseWrapper.getTaskRecordsQuery(newUserInfo)
        recordListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.e("asdf", "DomainFactory.getMRecordListener().onDataChange, dataSnapshot: $dataSnapshot")

                setRemoteTaskRecords(dataSnapshot, source)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("asdf", "DomainFactory.getMRecordListener().onCancelled", databaseError.toException())

                MyCrashlytics.logException(databaseError.toException())

                tickData?.release()
                tickData = null

                notTickFirebaseListeners.clear()
                RemoteFriendFactory.clearFriendListeners()
            }
        }
        recordQuery!!.addValueEventListener(recordListener!!)

        RemoteFriendFactory.setListener(userInfo!!)

        userQuery = DatabaseWrapper.getUserQuery(newUserInfo)
        userListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.e("asdf", "DomainFactory.getMUserListener().onDataChange, dataSnapshot: $dataSnapshot")

                setUserRecord(dataSnapshot)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("asdf", "DomainFactory.getMUserListener().onCancelled", databaseError.toException())

                MyCrashlytics.logException(databaseError.toException())
            }
        }

        userQuery!!.addValueEventListener(userListener!!)
    }

    @Synchronized
    fun clearUserInfo() {
        val now = ExactTimeStamp.now

        if (userInfo == null) {
            check(recordQuery == null)
            check(recordListener == null)
            check(userQuery == null)
            check(userListener == null)
        } else {
            check(recordQuery != null)
            check(recordListener != null)
            check(userQuery != null)
            check(userListener != null)

            localFactory.clearRemoteCustomTimeRecords()
            Log.e("asdf", "clearing getMRemoteProjectFactory()", Exception())

            remoteProjectFactory = null
            RemoteFriendFactory.setInstance(null)

            userInfo = null

            recordQuery!!.removeEventListener(recordListener!!)
            recordQuery = null
            recordListener = null

            RemoteFriendFactory.clearListener()

            userQuery!!.removeEventListener(userListener!!)
            userQuery = null
            userListener = null

            updateNotifications(now)

            ObserverHolder.notifyDomainObservers(ArrayList())
        }
    }

    @Synchronized
    fun setRemoteTaskRecords(dataSnapshot: DataSnapshot, source: SaveService.Source) {
        check(userInfo != null)

        val now = ExactTimeStamp.now

        localFactory.clearRemoteCustomTimeRecords()

        val firstThereforeSilent = remoteProjectFactory == null
        remoteProjectFactory = RemoteProjectFactory(this, dataSnapshot.children, userInfo!!, localFactory.uuid, now)

        RemoteFriendFactory.tryNotifyFriendListeners() // assuming they're all getters

        if (tickData == null && notTickFirebaseListeners.isEmpty()) {
            updateNotifications(firstThereforeSilent, ExactTimeStamp.now, listOf(), "DomainModel.setRemoteTaskRecords")

            save(0, source)
        } else {
            skipSave = true

            if (tickData == null) {
                updateNotifications(firstThereforeSilent, ExactTimeStamp.now, listOf(), "DomainModel.setRemoteTaskRecords")
            } else {
                updateNotificationsTick(source, tickData!!.silent, tickData!!.source)

                if (!firstThereforeSilent) {
                    Log.e("asdf", "not first, clearing getMTickData()")

                    tickData!!.release()
                    tickData = null
                } else {
                    Log.e("asdf", "first, keeping getMTickData()")
                }
            }

            notTickFirebaseListeners.forEach { it.invoke(this) }
            notTickFirebaseListeners.clear()

            skipSave = false

            save(0, source)
        }
    }

    @Synchronized
    fun setUserRecord(dataSnapshot: DataSnapshot) {
        val userWrapper = dataSnapshot.getValue(UserWrapper::class.java)!!

        val remoteRootUserRecord = RemoteRootUserRecord(false, userWrapper)
        remoteRootUser = RemoteRootUser(remoteRootUserRecord)
    }

    @Synchronized
    fun addFirebaseListener(firebaseListener: (DomainFactory) -> Unit) {
        check(remoteProjectFactory?.isSaved != false)

        notTickFirebaseListeners.add(firebaseListener)
    }

    @Synchronized
    fun removeFirebaseListener(firebaseListener: (DomainFactory) -> Unit) {
        notTickFirebaseListeners.remove(firebaseListener)
    }

    @Synchronized
    fun setFirebaseTickListener(source: SaveService.Source, newTickData: TickData) {
        check(FirebaseAuth.getInstance().currentUser != null)

        if (remoteProjectFactory != null && !remoteProjectFactory!!.isSaved && tickData == null) {
            updateNotificationsTick(source, newTickData.silent, newTickData.source)

            newTickData.release()
        } else {
            tickData = if (tickData != null) {
                mergeTickDatas(tickData!!, newTickData)
            } else {
                newTickData
            }
        }
    }

    @Synchronized
    fun getIsConnected(): Boolean = remoteProjectFactory != null

    @Synchronized
    fun getIsConnectedAndSaved() = remoteProjectFactory!!.isSaved

    // gets

    @Synchronized
    fun getEditInstanceData(instanceKey: InstanceKey): EditInstanceViewModel.Data {
        MyCrashlytics.log("DomainFactory.getEditInstanceData")

        val now = ExactTimeStamp.now

        val currentCustomTimes = getCurrentCustomTimes().associateBy { it.customTimeKey }.toMutableMap<CustomTimeKey, CustomTime>()

        val instance = getInstance(instanceKey)
        check(instance.isRootInstance(now))

        if (instance.instanceTimePair.customTimeKey != null) {
            val customTime = getCustomTime(instance.instanceTimePair.customTimeKey!!)

            currentCustomTimes[customTime.customTimeKey] = customTime
        }

        val customTimeDatas = currentCustomTimes.mapValues { it.value.let { EditInstanceViewModel.CustomTimeData(it.customTimeKey, it.name, it.hourMinutes) } }

        return EditInstanceViewModel.Data(instance.instanceKey, instance.instanceDate, instance.instanceTimePair, instance.name, customTimeDatas, instance.done != null, instance.instanceDateTime.timeStamp.toExactTimeStamp() <= now)
    }

    @Synchronized
    fun getEditInstancesData(instanceKeys: List<InstanceKey>): EditInstancesViewModel.Data {
        MyCrashlytics.log("DomainFactory.getEditInstancesData")

        check(instanceKeys.size > 1)

        val now = ExactTimeStamp.now

        val currentCustomTimes = getCurrentCustomTimes().associateBy { it.customTimeKey }.toMutableMap<CustomTimeKey, CustomTime>()

        val instanceDatas = mutableMapOf<InstanceKey, EditInstancesViewModel.InstanceData>()

        for (instanceKey in instanceKeys) {
            val instance = getInstance(instanceKey)
            check(instance.isRootInstance(now))
            check(instance.done == null)

            instanceDatas[instanceKey] = EditInstancesViewModel.InstanceData(instance.instanceDateTime, instance.name)

            if (instance.instanceTimePair.customTimeKey != null) {
                val customTime = getCustomTime(instance.instanceTimePair.customTimeKey!!)

                currentCustomTimes[customTime.customTimeKey] = customTime
            }
        }

        val customTimeDatas = currentCustomTimes.mapValues { it.value.let { EditInstancesViewModel.CustomTimeData(it.customTimeKey, it.name, it.hourMinutes) } }

        val showHour = instanceDatas.values.all { it.instanceDateTime.timeStamp.toExactTimeStamp() < now }

        return EditInstancesViewModel.Data(instanceDatas, customTimeDatas, showHour)
    }

    @Synchronized
    fun getShowCustomTimeData(localCustomTimeId: Int): ShowCustomTimeViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowCustomTimeData")

        val localCustomTime = localFactory.getLocalCustomTime(localCustomTimeId)

        val hourMinutes = DayOfWeek.values().associate { it to localCustomTime.getHourMinute(it) }

        return ShowCustomTimeViewModel.Data(localCustomTime.id, localCustomTime.name, hourMinutes)
    }

    @Synchronized
    fun getShowCustomTimesData(): ShowCustomTimesViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowCustomTimesData")

        val entries = getCurrentCustomTimes().map { ShowCustomTimesViewModel.CustomTimeData(it.id, it.name) }

        return ShowCustomTimesViewModel.Data(entries)
    }

    @Synchronized
    fun getGroupListData(now: ExactTimeStamp, position: Int, timeRange: MainActivity.TimeRange): DayViewModel.DayData {
        MyCrashlytics.log("DomainFactory.getShowNotificationGroupData")

        check(position >= 0)

        val startExactTimeStamp: ExactTimeStamp?
        val endExactTimeStamp: ExactTimeStamp

        if (position == 0) {
            startExactTimeStamp = null
        } else {
            val startCalendar = now.calendar

            when (timeRange) {
                MainActivity.TimeRange.DAY -> startCalendar.add(Calendar.DATE, position)
                MainActivity.TimeRange.WEEK -> {
                    startCalendar.add(Calendar.WEEK_OF_YEAR, position)
                    startCalendar.set(Calendar.DAY_OF_WEEK, startCalendar.firstDayOfWeek)
                }
                MainActivity.TimeRange.MONTH -> {
                    startCalendar.add(Calendar.MONTH, position)
                    startCalendar.set(Calendar.DAY_OF_MONTH, 1)
                }
            }

            startExactTimeStamp = ExactTimeStamp(Date(startCalendar), HourMilli(0, 0, 0, 0))
        }

        val endCalendar = now.calendar

        when (timeRange) {
            MainActivity.TimeRange.DAY -> endCalendar.add(Calendar.DATE, position + 1)
            MainActivity.TimeRange.WEEK -> {
                endCalendar.add(Calendar.WEEK_OF_YEAR, position + 1)
                endCalendar.set(Calendar.DAY_OF_WEEK, endCalendar.firstDayOfWeek)
            }
            MainActivity.TimeRange.MONTH -> {
                endCalendar.add(Calendar.MONTH, position + 1)
                endCalendar.set(Calendar.DAY_OF_MONTH, 1)
            }
        }

        endExactTimeStamp = ExactTimeStamp(Date(endCalendar), HourMilli(0, 0, 0, 0))

        val currentInstances = getRootInstances(startExactTimeStamp, endExactTimeStamp, now)

        val customTimeDatas = getCurrentCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val taskDatas = if (position == 0) {
            getTasks().filter { it.current(now) && it.isVisible(now) && it.isRootTask(now) && it.getCurrentSchedules(now).isEmpty() }
                    .map { GroupListFragment.TaskData(it.taskKey, it.name, getGroupListChildTaskDatas(it, now), it.startExactTimeStamp, it.note) }
                    .toList()
        } else {
            listOf()
        }

        val instanceDatas = HashMap<InstanceKey, GroupListFragment.InstanceData>()
        for (instance in currentInstances) {
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)
            val instanceData = GroupListFragment.InstanceData(instance.done, instance.instanceKey, instance.getDisplayText(now), instance.name, instance.instanceDateTime.timeStamp, task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.instanceDateTime.time.timePair, task.note, children, null, instance.ordinal)
            children.values.forEach { it.instanceDataParent = instanceData }
            instanceDatas[instanceData.InstanceKey] = instanceData
        }

        val dataWrapper = GroupListFragment.DataWrapper(customTimeDatas, null, taskDatas, null, instanceDatas)
        val data = DayViewModel.DayData(dataWrapper)

        instanceDatas.values.forEach { it.instanceDataParent = dataWrapper }

        Log.e("asdf", "getShowNotificationGroupData returning $data")
        return data
    }

    @Synchronized
    fun getShowGroupData(timeStamp: TimeStamp): ShowGroupViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowGroupData")

        val now = ExactTimeStamp.now

        val date = timeStamp.date
        val dayOfWeek = date.dayOfWeek
        val hourMinute = timeStamp.hourMinute

        val time = getCurrentCustomTimes().firstOrNull { it.getHourMinute(dayOfWeek) == hourMinute }
                ?: NormalTime(hourMinute)

        val displayText = DateTime(date, time).getDisplayText()

        return ShowGroupViewModel.Data(displayText, getGroupListData(timeStamp, now))
    }

    @Synchronized
    fun getShowTaskInstancesData(taskKey: TaskKey): ShowTaskInstancesViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowTaskInstancesData")

        val task = getTaskForce(taskKey)
        val now = ExactTimeStamp.now

        val customTimeDatas = getCurrentCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val isRootTask = if (task.current(now)) task.isRootTask(now) else null

        val existingInstances = task.existingInstances.values
        val pastInstances = task.getInstances(null, now, now)

        val allInstances = existingInstances.toMutableSet()
        allInstances.addAll(pastInstances)

        val instanceDatas = allInstances.associate {
            val children = getChildInstanceDatas(it, now)

            val hierarchyData = if (task.isRootTask(now)) {
                null
            } else {
                val taskHierarchy = getParentTaskHierarchy(task, now)!!

                HierarchyData(taskHierarchy.taskHierarchyKey, taskHierarchy.ordinal)
            }

            it.instanceKey to GroupListFragment.InstanceData(it.done, it.instanceKey, it.getDisplayText(now), it.name, it.instanceDateTime.timeStamp, task.current(now), it.isRootInstance(now), isRootTask, it.exists(), it.instanceDateTime.time.timePair, task.note, children, hierarchyData, it.ordinal)
        }.toMutableMap()

        return ShowTaskInstancesViewModel.Data(GroupListFragment.DataWrapper(customTimeDatas, task.current(now), listOf(), null, instanceDatas))
    }

    @Synchronized
    fun getShowNotificationGroupData(instanceKeys: Set<InstanceKey>): ShowNotificationGroupViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowNotificationGroupData")

        check(!instanceKeys.isEmpty())

        val now = ExactTimeStamp.now

        val instances = instanceKeys.map { getInstance(it) }
                .filter { it.isRootInstance(now) }
                .sortedBy { it.instanceDateTime }

        val customTimeDatas = getCurrentCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val instanceDatas = instances.associate { instance ->
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)
            val instanceData = GroupListFragment.InstanceData(instance.done, instance.instanceKey, instance.getDisplayText(now), instance.name, instance.instanceDateTime.timeStamp, task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.instanceDateTime.time.timePair, task.note, children, null, instance.ordinal)
            children.values.forEach { it.instanceDataParent = instanceData }
            instance.instanceKey to instanceData
        }.toMutableMap()

        val dataWrapper = GroupListFragment.DataWrapper(customTimeDatas, null, listOf(), null, instanceDatas)

        instanceDatas.values.forEach { it.instanceDataParent = dataWrapper }

        return ShowNotificationGroupViewModel.Data(dataWrapper)
    }

    @Synchronized
    fun getShowInstanceData(instanceKey: InstanceKey): ShowInstanceViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowInstanceData")

        val task = getTaskIfPresent(instanceKey.taskKey)
                ?: return ShowInstanceViewModel.Data(null)

        val now = ExactTimeStamp.now

        val instance = getInstance(instanceKey)
        return if (!task.current(now) && !instance.exists()) ShowInstanceViewModel.Data(null) else ShowInstanceViewModel.Data(ShowInstanceViewModel.InstanceData(instance.name, instance.getDisplayText(now), instance.done != null, task.current(now), instance.isRootInstance(now), instance.exists(), getGroupListData(instance, task, now)))
    }

    fun getScheduleDatas(schedules: List<Schedule>, now: ExactTimeStamp): kotlin.Pair<Map<CustomTimeKey, CustomTime>, Map<CreateTaskViewModel.ScheduleData, List<Schedule>>> {
        val customTimes = HashMap<CustomTimeKey, CustomTime>()

        val scheduleDatas = HashMap<CreateTaskViewModel.ScheduleData, List<Schedule>>()

        val weeklySchedules = HashMap<TimePair, MutableList<WeeklySchedule>>()

        for (schedule in schedules) {
            check(schedule.current(now))

            when (schedule) {
                is SingleSchedule -> {
                    scheduleDatas[CreateTaskViewModel.ScheduleData.SingleScheduleData(schedule.date, schedule.timePair)] = listOf<Schedule>(schedule)

                    schedule.customTimeKey?.let { customTimes[it] = getCustomTime(it) }
                }
                is WeeklySchedule -> {
                    val timePair = schedule.timePair
                    if (!weeklySchedules.containsKey(timePair))
                        weeklySchedules[timePair] = ArrayList()
                    weeklySchedules[timePair]!!.add(schedule)

                    schedule.customTimeKey?.let { customTimes[it] = getCustomTime(it) }
                }
                is MonthlyDaySchedule -> {
                    scheduleDatas[CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData(schedule.dayOfMonth, schedule.beginningOfMonth, schedule.timePair)] = listOf<Schedule>(schedule)

                    schedule.customTimeKey?.let { customTimes[it] = getCustomTime(it) }
                }
                is MonthlyWeekSchedule -> {
                    scheduleDatas[CreateTaskViewModel.ScheduleData.MonthlyWeekScheduleData(schedule.dayOfMonth, schedule.dayOfWeek, schedule.beginningOfMonth, schedule.timePair)] = listOf<Schedule>(schedule)

                    schedule.customTimeKey?.let { customTimes[it] = getCustomTime(it) }
                }
                else -> throw UnsupportedOperationException()
            }
        }

        for ((key, value) in weeklySchedules) {
            val daysOfWeek = value.flatMap { it.daysOfWeek }.toSet()
            scheduleDatas[CreateTaskViewModel.ScheduleData.WeeklyScheduleData(daysOfWeek, key)] = ArrayList<Schedule>(value)
        }

        return Pair<Map<CustomTimeKey, CustomTime>, Map<CreateTaskViewModel.ScheduleData, List<Schedule>>>(customTimes, scheduleDatas)
    }

    @Synchronized
    fun getCreateTaskData(taskKey: TaskKey?, joinTaskKeys: List<TaskKey>?): CreateTaskViewModel.Data {
        MyCrashlytics.log("DomainFactory.getCreateTaskData")

        check(taskKey == null || joinTaskKeys == null)

        val now = ExactTimeStamp.now

        val customTimes = getCurrentCustomTimes().associateBy { it.customTimeKey }.toMutableMap<CustomTimeKey, CustomTime>()

        val excludedTaskKeys = when {
            taskKey != null -> listOf(taskKey)
            joinTaskKeys != null -> joinTaskKeys
            else -> listOf()
        }

        var taskData: CreateTaskViewModel.TaskData? = null
        val parentTreeDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>
        if (taskKey != null) {
            val task = getTaskForce(taskKey)

            val taskParentKey: CreateTaskViewModel.ParentKey.TaskParentKey?
            var scheduleDatas: List<CreateTaskViewModel.ScheduleData>? = null

            if (task.isRootTask(now)) {
                val schedules = task.getCurrentSchedules(now)

                taskParentKey = null

                if (!schedules.isEmpty()) {
                    val pair = getScheduleDatas(schedules, now)
                    customTimes.putAll(pair.first)
                    scheduleDatas = pair.second.keys.toList()
                }
            } else {
                val parentTask = task.getParentTask(now)!!
                taskParentKey = CreateTaskViewModel.ParentKey.TaskParentKey(parentTask.taskKey)
            }

            val projectName = task.remoteNullableProject?.name

            taskData = CreateTaskViewModel.TaskData(task.name, taskParentKey, scheduleDatas, task.note, projectName)

            parentTreeDatas = if (task is RemoteTask) {
                getProjectTaskTreeDatas(now, task.remoteProject, excludedTaskKeys)
            } else {
                check(task is LocalTask)

                getParentTreeDatas(now, excludedTaskKeys)
            }
        } else {
            var projectId: String? = null
            if (joinTaskKeys != null) {
                check(joinTaskKeys.size > 1)

                val projectIds = joinTaskKeys.map { it.remoteProjectId }.distinct()

                projectId = projectIds.single()
            }

            parentTreeDatas = if (!projectId.isNullOrEmpty()) {
                check(remoteProjectFactory != null)

                val remoteProject = remoteProjectFactory!!.getRemoteProjectForce(projectId)

                getProjectTaskTreeDatas(now, remoteProject, excludedTaskKeys)
            } else {
                getParentTreeDatas(now, excludedTaskKeys)
            }
        }

        val customTimeDatas = customTimes.values.associate { it.customTimeKey to CreateTaskViewModel.CustomTimeData(it.customTimeKey, it.name, it.hourMinutes) }

        return CreateTaskViewModel.Data(taskData, parentTreeDatas, customTimeDatas)
    }

    @Synchronized
    fun getShowTaskData(taskKey: TaskKey): ShowTaskViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowTaskData")

        val now = ExactTimeStamp.now

        val task = getTaskForce(taskKey)
        check(task.current(now))

        val childTaskDatas = task.getChildTaskHierarchies(now)
                .map {
                    val childTask = it.childTask

                    TaskListFragment.ChildTaskData(childTask.name, childTask.getScheduleText(now), getTaskListChildTaskDatas(childTask, now), childTask.note, childTask.startExactTimeStamp, childTask.taskKey, HierarchyData(it.taskHierarchyKey, it.ordinal))
                }
                .sorted()

        return ShowTaskViewModel.Data(task.name, task.getScheduleText(now), TaskListFragment.TaskData(childTaskDatas, task.note), !task.existingInstances.isEmpty())
    }

    @Synchronized
    fun getMainData(): MainViewModel.Data {
        MyCrashlytics.log("DomainFactory.getMainData")

        val now = ExactTimeStamp.now

        return MainViewModel.Data(getMainData(now))
    }

    @Synchronized
    fun getProjectListData(): ProjectListViewModel.Data {
        MyCrashlytics.log("DomainFactory.getProjectListData")

        check(remoteProjectFactory != null)

        val now = ExactTimeStamp.now

        val projectDatas = remoteProjectFactory!!.remoteProjects
                .values
                .filter { it.current(now) }
                .associate {
                    val users = it.users.joinToString(", ") { it.name }

                    it.id to ProjectListViewModel.ProjectData(it.id, it.name, users)
                }
                .toSortedMap()

        return ProjectListViewModel.Data(projectDatas)
    }

    @Synchronized
    fun getFriendListData(): FriendListViewModel.Data {
        MyCrashlytics.log("DomainFactory.getFriendListData")

        check(RemoteFriendFactory.hasFriends())

        val userListDatas = RemoteFriendFactory.getFriends()
                .map { FriendListViewModel.UserListData(it.name, it.email, it.id) }
                .toSet()

        return FriendListViewModel.Data(userListDatas)
    }

    @Synchronized
    fun getShowProjectData(projectId: String?): ShowProjectViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowProjectData")

        check(remoteProjectFactory != null)
        check(userInfo != null)
        check(RemoteFriendFactory.hasFriends())

        val friendDatas = RemoteFriendFactory.getFriends()
                .map { ShowProjectViewModel.UserListData(it.name, it.email, it.id) }
                .associateBy { it.id }

        val name: String?
        val userListDatas: Set<ShowProjectViewModel.UserListData>
        if (!projectId.isNullOrEmpty()) {
            val remoteProject = remoteProjectFactory!!.getRemoteProjectForce(projectId)

            name = remoteProject.name

            userListDatas = remoteProject.users
                    .filterNot { it.id == userInfo!!.key }
                    .map { ShowProjectViewModel.UserListData(it.name, it.email, it.id) }
                    .toSet()
        } else {
            name = null
            userListDatas = setOf()
        }

        return ShowProjectViewModel.Data(name, userListDatas, friendDatas)
    }

    // sets

    @Synchronized
    fun setInstanceDateTime(dataId: Int, source: SaveService.Source, instanceKey: InstanceKey, instanceDate: Date, instanceTimePair: TimePair) {
        MyCrashlytics.log("DomainFactory.setInstanceDateTime")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val instance = getInstance(instanceKey)

        val now = ExactTimeStamp.now

        instance.setInstanceDateTime(instanceDate, instanceTimePair, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(instance.remoteNullableProject)
    }

    @Synchronized
    fun setInstancesDateTime(dataId: Int, source: SaveService.Source, instanceKeys: Set<InstanceKey>, instanceDate: Date, instanceTimePair: TimePair) {
        MyCrashlytics.log("DomainFactory.setInstancesDateTime")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(instanceKeys.size > 1)

        val now = ExactTimeStamp.now

        val instances = instanceKeys.map(this::getInstance)

        instances.forEach { it.setInstanceDateTime(instanceDate, instanceTimePair, now) }

        val remoteProjects = instances
                .filter { it.belongsToRemoteProject() }
                .map { it.remoteNonNullProject }
                .toSet()

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)
    }

    @Synchronized
    fun setInstanceAddHourService(source: SaveService.Source, instanceKey: InstanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHourService")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val instance = getInstance(instanceKey)

        val now = ExactTimeStamp.now
        val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

        val date = Date(calendar)
        val hourMinute = HourMinute(calendar)

        instance.setInstanceDateTime(date, TimePair(hourMinute), now)
        instance.setNotificationShown(false, now)

        updateNotifications(now)

        save(0, source)

        notifyCloud(instance.remoteNullableProject)
    }

    @Synchronized
    fun setInstanceAddHourActivity(dataId: Int, source: SaveService.Source, instanceKey: InstanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val instance = getInstance(instanceKey)

        val now = ExactTimeStamp.now
        val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

        val date = Date(calendar)
        val hourMinute = HourMinute(calendar)

        instance.setInstanceDateTime(date, TimePair(hourMinute), now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(instance.remoteNullableProject)
    }

    @Synchronized
    fun setInstancesAddHourActivity(dataId: Int, source: SaveService.Source, instanceKeys: Collection<InstanceKey>) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val now = ExactTimeStamp.now
        val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

        val date = Date(calendar)
        val hourMinute = HourMinute(calendar)

        val instances = instanceKeys.map(this::getInstance)

        instances.forEach { it.setInstanceDateTime(date, TimePair(hourMinute), now) }

        updateNotifications(now)

        save(dataId, source)

        val remoteProjects = instances.mapNotNull { it.remoteNullableProject }.toSet()

        notifyCloud(remoteProjects)
    }

    @Synchronized
    fun setInstanceNotificationDone(source: SaveService.Source, instanceKey: InstanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotificationDone")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val instance = getInstance(instanceKey)

        val now = ExactTimeStamp.now

        instance.setDone(true, now)
        instance.setNotificationShown(false, now)

        updateNotifications(now)

        save(0, source)

        notifyCloud(instance.remoteNullableProject)
    }

    @Synchronized
    fun setInstanceDone(dataId: Int, source: SaveService.Source, instanceKey: InstanceKey, done: Boolean): ExactTimeStamp? {
        MyCrashlytics.log("DomainFactory.setInstanceDone")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val now = ExactTimeStamp.now

        val instance = setInstanceDone(now, dataId, source, instanceKey, done)

        return instance.done
    }

    @Synchronized
    fun setInstancesDone(dataId: Int, source: SaveService.Source, instanceKeys: List<InstanceKey>, done: Boolean): ExactTimeStamp {
        MyCrashlytics.log("DomainFactory.setInstancesDone")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val now = ExactTimeStamp.now

        val instances = instanceKeys.map(this::getInstance)

        instances.forEach { it.setDone(done, now) }

        val remoteProjects = instances.mapNotNull(Instance::remoteNullableProject).toSet()

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)

        return now
    }

    @Synchronized
    fun setInstanceNotified(dataId: Int, source: SaveService.Source, instanceKey: InstanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotified")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        setInstanceNotified(instanceKey, ExactTimeStamp.now)

        save(dataId, source)
    }

    @Synchronized
    fun setInstancesNotified(source: SaveService.Source, instanceKeys: List<InstanceKey>) {
        MyCrashlytics.log("DomainFactory.setInstancesNotified")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(!instanceKeys.isEmpty())

        val now = ExactTimeStamp.now

        for (instanceKey in instanceKeys)
            setInstanceNotified(instanceKey, now)

        save(0, source)
    }

    fun createScheduleRootTask(now: ExactTimeStamp, dataId: Int, source: SaveService.Source, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?, projectId: String?): Task {
        check(name.isNotEmpty())
        check(!scheduleDatas.isEmpty())

        val task = if (projectId.isNullOrEmpty()) {
            localFactory.createScheduleRootTask(now, name, scheduleDatas, note)
        } else {
            check(remoteProjectFactory != null)

            remoteProjectFactory!!.createScheduleRootTask(now, name, scheduleDatas, note, projectId)
        }

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.remoteNullableProject)

        return task
    }

    @Synchronized
    fun createScheduleRootTask(dataId: Int, source: SaveService.Source, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?, projectId: String?) {
        MyCrashlytics.log("DomainFactory.createScheduleRootTask")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val now = ExactTimeStamp.now

        createScheduleRootTask(now, dataId, source, name, scheduleDatas, note, projectId)
    }

    fun updateScheduleTask(now: ExactTimeStamp, dataId: Int, source: SaveService.Source, taskKey: TaskKey, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?, projectId: String?): TaskKey {
        check(name.isNotEmpty())
        check(!scheduleDatas.isEmpty())

        var task = getTaskForce(taskKey)
        check(task.current(now))

        task = task.updateProject(now, projectId)

        task.setName(name, note)

        if (!task.isRootTask(now))
            getParentTaskHierarchy(task, now)!!.setEndExactTimeStamp(now)

        task.updateSchedules(scheduleDatas, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.remoteNullableProject)

        return task.taskKey
    }

    @Synchronized
    fun updateScheduleTask(dataId: Int, source: SaveService.Source, taskKey: TaskKey, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?, projectId: String?): TaskKey {
        MyCrashlytics.log("DomainFactory.updateScheduleTask")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(name.isNotEmpty())
        check(!scheduleDatas.isEmpty())

        val now = ExactTimeStamp.now

        return updateScheduleTask(now, dataId, source, taskKey, name, scheduleDatas, note, projectId)
    }

    @Synchronized
    fun createScheduleJoinRootTask(now: ExactTimeStamp, dataId: Int, source: SaveService.Source, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, joinTaskKeys: List<TaskKey>, note: String?, projectId: String?) {
        MyCrashlytics.log("DomainFactory.createScheduleJoinRootTask")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(name.isNotEmpty())
        check(!scheduleDatas.isEmpty())
        check(joinTaskKeys.size > 1)

        val joinProjectId = joinTaskKeys.map { it.remoteProjectId }
                .distinct()
                .single()

        val finalProjectId = if (!joinProjectId.isNullOrEmpty()) {
            check(projectId.isNullOrEmpty())

            joinProjectId
        } else if (!projectId.isNullOrEmpty()) {
            projectId
        } else {
            null
        }

        var joinTasks = joinTaskKeys.map { getTaskForce(it) }

        val newParentTask = if (!finalProjectId.isNullOrEmpty()) {
            check(remoteProjectFactory != null)
            check(userInfo != null)

            remoteProjectFactory!!.createScheduleRootTask(now, name, scheduleDatas, note, finalProjectId)
        } else {
            localFactory.createScheduleRootTask(now, name, scheduleDatas, note)
        }

        joinTasks = joinTasks.map { it.updateProject(now, projectId) }

        joinTasks(newParentTask, joinTasks, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(newParentTask.remoteNullableProject)
    }

    fun createChildTask(now: ExactTimeStamp, dataId: Int, source: SaveService.Source, parentTaskKey: TaskKey, name: String, note: String?): Task {
        check(name.isNotEmpty())

        val parentTask = getTaskForce(parentTaskKey)
        check(parentTask.current(now))

        val childTask = parentTask.createChildTask(now, name, note)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(childTask.remoteNullableProject)

        return childTask
    }

    @Synchronized
    fun createChildTask(dataId: Int, source: SaveService.Source, parentTaskKey: TaskKey, name: String, note: String?) {
        MyCrashlytics.log("DomainFactory.createChildTask")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val now = ExactTimeStamp.now

        createChildTask(now, dataId, source, parentTaskKey, name, note)
    }

    @Synchronized
    fun createJoinChildTask(dataId: Int, source: SaveService.Source, parentTaskKey: TaskKey, name: String, joinTaskKeys: List<TaskKey>, note: String?) {
        MyCrashlytics.log("DomainFactory.createJoinChildTask")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(name.isNotEmpty())
        check(joinTaskKeys.size > 1)

        val now = ExactTimeStamp.now

        val parentTask = getTaskForce(parentTaskKey)
        check(parentTask.current(now))

        val joinProjectIds = joinTaskKeys.map { it.remoteProjectId }.distinct()
        check(joinProjectIds.size == 1)

        val joinTasks = joinTaskKeys.map { getTaskForce(it) }

        val childTask = parentTask.createChildTask(now, name, note)

        joinTasks(childTask, joinTasks, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(childTask.remoteNullableProject)
    }

    @Synchronized
    fun updateChildTask(now: ExactTimeStamp, dataId: Int, source: SaveService.Source, taskKey: TaskKey, name: String, parentTaskKey: TaskKey, note: String?): TaskKey {
        MyCrashlytics.log("DomainFactory.updateChildTask")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(name.isNotEmpty())

        val task = getTaskForce(taskKey)
        check(task.current(now))

        val newParentTask = getTaskForce(parentTaskKey)
        check(task.current(now))

        task.setName(name, note)

        val oldParentTask = task.getParentTask(now)
        if (oldParentTask == null) {
            task.getCurrentSchedules(now).forEach { it.setEndExactTimeStamp(now) }

            newParentTask.addChild(task, now)
        } else if (oldParentTask !== newParentTask) {
            getParentTaskHierarchy(task, now)!!.setEndExactTimeStamp(now)

            newParentTask.addChild(task, now)
        }

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.remoteNullableProject)

        return task.taskKey
    }

    @Synchronized
    fun setTaskEndTimeStamp(dataId: Int, source: SaveService.Source, taskKey: TaskKey) {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamp")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val now = ExactTimeStamp.now

        val task = getTaskForce(taskKey)
        check(task.current(now))

        task.setEndExactTimeStamp(now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.remoteNullableProject)
    }

    @Synchronized
    fun setInstanceOrdinal(dataId: Int, instanceKey: InstanceKey, ordinal: Double) {
        MyCrashlytics.log("DomainFactory.setInstanceOrdinal")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val now = ExactTimeStamp.now

        val instance = getInstance(instanceKey)

        instance.setOrdinal(ordinal, now)

        updateNotifications(now)

        save(dataId, SaveService.Source.GUI)

        notifyCloud(instance.remoteNullableProject)
    }

    @Synchronized
    fun setTaskHierarchyOrdinal(dataId: Int, hierarchyData: HierarchyData) {
        MyCrashlytics.log("DomainFactory.setTaskHierarchyOrdinal")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val now = ExactTimeStamp.now

        val remoteProject: RemoteProject?
        val taskHierarchy: TaskHierarchy
        if (hierarchyData.taskHierarchyKey is TaskHierarchyKey.LocalTaskHierarchyKey) {

            remoteProject = null
            taskHierarchy = localFactory.getTaskHierarchy(hierarchyData.taskHierarchyKey)
        } else {
            check(hierarchyData.taskHierarchyKey is TaskHierarchyKey.RemoteTaskHierarchyKey)

            val (projectId, taskHierarchyId) = hierarchyData.taskHierarchyKey

            remoteProject = remoteProjectFactory!!.getRemoteProjectForce(projectId)
            taskHierarchy = remoteProject.getTaskHierarchy(taskHierarchyId)
        }

        check(taskHierarchy.current(now))

        taskHierarchy.ordinal = hierarchyData.ordinal

        updateNotifications(now)

        save(dataId, SaveService.Source.GUI)

        if (remoteProject != null)
            notifyCloud(remoteProject)
    }

    @Synchronized
    fun setTaskEndTimeStamps(dataId: Int, source: SaveService.Source, taskKeys: ArrayList<TaskKey>) {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(!taskKeys.isEmpty())

        val now = ExactTimeStamp.now

        val tasks = taskKeys.map { getTaskForce(it) }
        check(tasks.all { it.current(now) })

        tasks.forEach { it.setEndExactTimeStamp(now) }

        val remoteProjects = tasks.mapNotNull { it.remoteNullableProject }.toSet()

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)
    }

    @Synchronized
    fun createCustomTime(source: SaveService.Source, name: String, hourMinutes: Map<DayOfWeek, HourMinute>): Int {
        MyCrashlytics.log("DomainFactory.createCustomTime")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(name.isNotEmpty())

        check(DayOfWeek.values().all { hourMinutes[it] != null })

        val localCustomTime = localFactory.createLocalCustomTime(name, hourMinutes)

        save(0, source)

        return localCustomTime.id
    }

    @Synchronized
    fun updateCustomTime(dataId: Int, source: SaveService.Source, localCustomTimeId: Int, name: String, hourMinutes: Map<DayOfWeek, HourMinute>) {
        MyCrashlytics.log("DomainFactory.updateCustomTime")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(name.isNotEmpty())

        val localCustomTime = localFactory.getLocalCustomTime(localCustomTimeId)

        localCustomTime.setName(name)

        for (dayOfWeek in DayOfWeek.values()) {
            val hourMinute = hourMinutes[dayOfWeek]!!

            if (hourMinute != localCustomTime.getHourMinute(dayOfWeek))
                localCustomTime.setHourMinute(dayOfWeek, hourMinute)
        }

        save(dataId, source)
    }

    @Synchronized
    fun setCustomTimeCurrent(dataId: Int, source: SaveService.Source, localCustomTimeIds: List<Int>) {
        MyCrashlytics.log("DomainFactory.setCustomTimeCurrent")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(!localCustomTimeIds.isEmpty())

        for (localCustomTimeId in localCustomTimeIds)
            localFactory.getLocalCustomTime(localCustomTimeId).setCurrent()

        save(dataId, source)
    }

    fun createRootTask(now: ExactTimeStamp, dataId: Int, source: SaveService.Source, name: String, note: String?, projectId: String?): Task {
        check(name.isNotEmpty())

        val task = if (projectId.isNullOrEmpty()) {
            localFactory.createLocalTaskHelper(name, now, note)
        } else {
            check(remoteProjectFactory != null)

            remoteProjectFactory!!.createRemoteTaskHelper(now, name, note, projectId)
        }

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.remoteNullableProject)

        return task
    }

    @Synchronized
    fun createRootTask(dataId: Int, source: SaveService.Source, name: String, note: String?, projectId: String?) {
        MyCrashlytics.log("DomainFactory.createRootTask")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val now = ExactTimeStamp.now

        createRootTask(now, dataId, source, name, note, projectId)
    }

    @Synchronized
    fun createJoinRootTask(dataId: Int, source: SaveService.Source, name: String, joinTaskKeys: List<TaskKey>, note: String?, projectId: String?) {
        MyCrashlytics.log("DomainFactory.createJoinRootTask")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(name.isNotEmpty())
        check(joinTaskKeys.size > 1)

        val now = ExactTimeStamp.now

        val joinProjectId = joinTaskKeys.map { it.remoteProjectId }
                .distinct()
                .single()

        val finalProjectId = if (!joinProjectId.isNullOrEmpty()) {
            check(projectId.isNullOrEmpty())

            joinProjectId
        } else if (!projectId.isNullOrEmpty()) {
            projectId
        } else {
            null
        }

        var joinTasks = joinTaskKeys.map { getTaskForce(it) }

        val newParentTask = if (!finalProjectId.isNullOrEmpty()) {
            check(remoteProjectFactory != null)
            check(userInfo != null)

            remoteProjectFactory!!.createRemoteTaskHelper(now, name, note, finalProjectId)
        } else {
            localFactory.createLocalTaskHelper(name, now, note)
        }

        joinTasks = joinTasks.map { it.updateProject(now, projectId) }

        joinTasks(newParentTask, joinTasks, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(newParentTask.remoteNullableProject)
    }

    @Synchronized
    fun updateRootTask(dataId: Int, source: SaveService.Source, taskKey: TaskKey, name: String, note: String?, projectId: String?): TaskKey {
        MyCrashlytics.log("DomainFactory.updateRootTask")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        check(name.isNotEmpty())

        val now = ExactTimeStamp.now

        var task = getTaskForce(taskKey)
        check(task.current(now))

        task = task.updateProject(now, projectId)

        task.setName(name, note)

        getParentTaskHierarchy(task, now)?.setEndExactTimeStamp(now)

        task.getCurrentSchedules(now).forEach { it.setEndExactTimeStamp(now) }

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.remoteNullableProject)

        return task.taskKey
    }

    private fun updateNotificationsTick(now: ExactTimeStamp, source: SaveService.Source, silent: Boolean, sourceName: String): Irrelevant {
        updateNotifications(silent, now, listOf(), sourceName)

        val irrelevant = setIrrelevant(now)

        remoteProjectFactory?.let { localFactory.deleteInstanceShownRecords(it.taskKeys) }

        save(0, source)

        return irrelevant
    }

    @Synchronized
    fun updateNotificationsTick(source: SaveService.Source, silent: Boolean, sourceName: String) {
        MyCrashlytics.log("DomainFactory.updateNotificationsTick source: $sourceName")
        check(remoteProjectFactory == null || !remoteProjectFactory!!.isSaved)

        val now = ExactTimeStamp.now

        updateNotificationsTick(now, source, silent, sourceName)
    }

    @Synchronized
    fun removeFriends(keys: Set<String>) {
        MyCrashlytics.log("DomainFactory.removeFriends")

        check(userInfo != null)
        check(remoteProjectFactory != null)
        check(RemoteFriendFactory.hasFriends())
        check(!RemoteFriendFactory.isSaved())

        keys.forEach { RemoteFriendFactory.removeFriend(userInfo!!.key, it) }

        RemoteFriendFactory.save()
    }

    @Synchronized
    fun updateUserInfo(source: SaveService.Source, newUserInfo: UserInfo) {
        MyCrashlytics.log("DomainFactory.updateUserInfo")
        check(userInfo != null)
        check(remoteProjectFactory != null)

        if (userInfo == newUserInfo)
            return

        userInfo = newUserInfo
        DatabaseWrapper.setUserInfo(newUserInfo, localFactory.uuid)

        remoteProjectFactory!!.updateUserInfo(newUserInfo)

        save(0, source)
    }

    @Synchronized
    fun updateProject(dataId: Int, source: SaveService.Source, projectId: String, name: String, addedFriends: Set<String>, removedFriends: Set<String>) {
        MyCrashlytics.log("DomainFactory.updateProject")

        check(projectId.isNotEmpty())
        check(name.isNotEmpty())
        check(remoteProjectFactory != null)
        check(RemoteFriendFactory.hasFriends())

        val now = ExactTimeStamp.now

        val remoteProject = remoteProjectFactory!!.getRemoteProjectForce(projectId)

        remoteProject.name = name
        remoteProject.updateRecordOf(addedFriends.map { RemoteFriendFactory.getFriend(it) }.toSet(), removedFriends)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProject, removedFriends)
    }

    @Synchronized
    fun createProject(dataId: Int, source: SaveService.Source, name: String, friends: Set<String>) {
        MyCrashlytics.log("DomainFactory.createProject")

        check(name.isNotEmpty())
        check(remoteProjectFactory != null)
        check(userInfo != null)
        check(remoteRootUser != null)

        val now = ExactTimeStamp.now

        val recordOf = HashSet(friends)

        val key = userInfo!!.key
        check(!recordOf.contains(key))
        recordOf.add(key)

        val remoteProject = remoteProjectFactory!!.createRemoteProject(name, now, recordOf, remoteRootUser!!)

        save(dataId, source)

        notifyCloud(remoteProject)
    }

    @Synchronized
    fun setProjectEndTimeStamps(dataId: Int, source: SaveService.Source, projectIds: Set<String>) {
        MyCrashlytics.log("DomainFactory.setProjectEndTimeStamps")

        check(remoteProjectFactory != null)
        check(userInfo != null)
        check(!projectIds.isEmpty())

        val now = ExactTimeStamp.now

        val remoteProjects = projectIds.map { remoteProjectFactory!!.getRemoteProjectForce(it) }.toSet()
        check(remoteProjects.all { it.current(now) })

        remoteProjects.forEach { it.setEndExactTimeStamp(now) }

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)
    }

    // internal

    private fun getExistingInstanceIfPresent(taskKey: TaskKey, scheduleDateTime: DateTime): Instance? {
        val instanceKey = InstanceKey(taskKey, scheduleDateTime.date, scheduleDateTime.time.timePair)

        return getExistingInstanceIfPresent(instanceKey)
    }

    private fun getExistingInstanceIfPresent(instanceKey: InstanceKey): Instance? {
        return if (instanceKey.taskKey.localTaskId != null) {
            check(instanceKey.taskKey.remoteProjectId.isNullOrEmpty())
            check(instanceKey.taskKey.remoteTaskId.isNullOrEmpty())

            localFactory.getExistingInstanceIfPresent(instanceKey)
        } else {
            check(!instanceKey.taskKey.remoteProjectId.isNullOrEmpty())
            check(!instanceKey.taskKey.remoteTaskId.isNullOrEmpty())
            checkNotNull(remoteProjectFactory)

            remoteProjectFactory!!.getExistingInstanceIfPresent(instanceKey)
        }
    }

    fun getRemoteCustomTimeId(projectId: String, customTimeKey: CustomTimeKey) = when (customTimeKey) {
        is CustomTimeKey.RemoteCustomTimeKey -> customTimeKey.remoteCustomTimeId
        is CustomTimeKey.LocalCustomTimeKey -> {
            val localCustomTime = localFactory.getLocalCustomTime(customTimeKey.localCustomTimeId)

            check(localCustomTime.hasRemoteRecord(projectId))

            localCustomTime.getRemoteId(projectId)
        }
    }

    private fun generateInstance(taskKey: TaskKey, scheduleDateTime: DateTime): Instance {
        if (taskKey.localTaskId != null) {
            check(taskKey.remoteProjectId.isNullOrEmpty())
            check(taskKey.remoteTaskId.isNullOrEmpty())

            return LocalInstance(this, taskKey.localTaskId, scheduleDateTime)
        } else {
            check(remoteProjectFactory != null)
            check(!taskKey.remoteProjectId.isNullOrEmpty())
            check(!taskKey.remoteTaskId.isNullOrEmpty())

            val (remoteCustomTimeId, hour, minute) = scheduleDateTime.time
                    .timePair
                    .destructureRemote(this, taskKey.remoteProjectId)

            val instanceShownRecord = localFactory.getInstanceShownRecord(taskKey.remoteProjectId, taskKey.remoteTaskId, scheduleDateTime.date.year, scheduleDateTime.date.month, scheduleDateTime.date.day, remoteCustomTimeId, hour, minute)

            val remoteProject = remoteProjectFactory!!.getTaskForce(taskKey).remoteProject

            return RemoteInstance(this, remoteProject, taskKey.remoteTaskId, scheduleDateTime, instanceShownRecord)
        }
    }

    fun getInstance(taskKey: TaskKey, scheduleDateTime: DateTime): Instance {
        val existingInstance = getExistingInstanceIfPresent(taskKey, scheduleDateTime)

        return existingInstance ?: generateInstance(taskKey, scheduleDateTime)
    }

    fun getInstance(instanceKey: InstanceKey): Instance {
        getExistingInstanceIfPresent(instanceKey)?.let { return it }

        val dateTime = getDateTime(instanceKey.scheduleKey.scheduleDate, instanceKey.scheduleKey.scheduleTimePair)

        return generateInstance(instanceKey.taskKey, dateTime) // DateTime -> timePair
    }

    fun getPastInstances(task: Task, now: ExactTimeStamp): List<Instance> {
        val allInstances = HashMap<InstanceKey, Instance>()

        allInstances.putAll(task.existingInstances
                .values
                .filter { it.scheduleDateTime.timeStamp.toExactTimeStamp() <= now }
                .associateBy { it.instanceKey })

        allInstances.putAll(task.getInstances(null, now.plusOne(), now).associateBy { it.instanceKey })

        return ArrayList(allInstances.values)
    }

    private fun getRootInstances(startExactTimeStamp: ExactTimeStamp?, endExactTimeStamp: ExactTimeStamp, now: ExactTimeStamp): List<Instance> {
        check(startExactTimeStamp == null || startExactTimeStamp < endExactTimeStamp)

        val allInstances = HashMap<InstanceKey, Instance>()

        for (instance in getExistingInstances()) {
            val instanceExactTimeStamp = instance.instanceDateTime
                    .timeStamp
                    .toExactTimeStamp()

            if (startExactTimeStamp != null && startExactTimeStamp > instanceExactTimeStamp)
                continue

            if (endExactTimeStamp <= instanceExactTimeStamp)
                continue

            allInstances[instance.instanceKey] = instance
        }

        getTasks().forEach { task ->
            for (instance in task.getInstances(startExactTimeStamp, endExactTimeStamp, now)) {
                val instanceExactTimeStamp = instance.instanceDateTime.timeStamp.toExactTimeStamp()

                if (startExactTimeStamp != null && startExactTimeStamp > instanceExactTimeStamp)
                    continue

                if (endExactTimeStamp <= instanceExactTimeStamp)
                    continue

                allInstances[instance.instanceKey] = instance
            }
        }

        return allInstances.values.filter { it.isRootInstance(now) && it.isVisible(now) }
    }

    fun getTime(timePair: TimePair): Time {
        return if (timePair.hourMinute != null) {
            check(timePair.customTimeKey == null)

            NormalTime(timePair.hourMinute)
        } else {
            checkNotNull(timePair.customTimeKey)

            getCustomTime(timePair.customTimeKey)
        }
    }

    private fun getDateTime(date: Date, timePair: TimePair) = DateTime(date, getTime(timePair))

    fun getParentTask(childTask: Task, exactTimeStamp: ExactTimeStamp): Task? {
        check(childTask.notDeleted(exactTimeStamp))

        val parentTaskHierarchy = getParentTaskHierarchy(childTask, exactTimeStamp)
        return if (parentTaskHierarchy == null) {
            null
        } else {
            check(parentTaskHierarchy.notDeleted(exactTimeStamp))

            val parentTask = parentTaskHierarchy.parentTask
            check(parentTask.notDeleted(exactTimeStamp))

            parentTask
        }
    }

    fun getCustomTime(customTimeKey: CustomTimeKey) = when (customTimeKey) {
        is CustomTimeKey.LocalCustomTimeKey -> localFactory.getLocalCustomTime(customTimeKey.localCustomTimeId)
        is CustomTimeKey.RemoteCustomTimeKey -> remoteProjectFactory!!.getRemoteCustomTime(customTimeKey.remoteProjectId, customTimeKey.remoteCustomTimeId)
    }

    private fun getCurrentCustomTimes() = localFactory.currentCustomTimes

    private fun getChildInstanceDatas(instance: Instance, now: ExactTimeStamp): MutableMap<InstanceKey, GroupListFragment.InstanceData> {
        return instance.getChildInstances(now)
                .map { (childInstance, taskHierarchy) ->
                    val childTask = childInstance.task

                    val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

                    val children = getChildInstanceDatas(childInstance, now)
                    val instanceData = GroupListFragment.InstanceData(childInstance.done, childInstance.instanceKey, null, childInstance.name, childInstance.instanceDateTime.timeStamp, childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), childInstance.instanceDateTime.time.timePair, childTask.note, children, HierarchyData(taskHierarchy.taskHierarchyKey, taskHierarchy.ordinal), childInstance.ordinal)
                    children.values.forEach { it.instanceDataParent = instanceData }
                    childInstance.instanceKey to instanceData
                }
                .toMap()
                .toMutableMap()
    }

    private fun getTaskListChildTaskDatas(now: ExactTimeStamp, parentTask: Task, excludedTaskKeys: List<TaskKey>): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> =
            parentTask.getChildTaskHierarchies(now)
                    .asSequence()
                    .filterNot { excludedTaskKeys.contains(it.childTaskKey) }
                    .map {
                        val childTask = it.childTask
                        val taskParentKey = CreateTaskViewModel.ParentKey.TaskParentKey(it.childTaskKey)
                        val parentTreeData = CreateTaskViewModel.ParentTreeData(childTask.name, getTaskListChildTaskDatas(now, childTask, excludedTaskKeys), CreateTaskViewModel.ParentKey.TaskParentKey(childTask.taskKey), childTask.getScheduleText(now), childTask.note, CreateTaskViewModel.SortKey.TaskSortKey(childTask.startExactTimeStamp))

                        taskParentKey to parentTreeData
                    }
                    .toList()
                    .toMap()

    private fun getParentTreeDatas(now: ExactTimeStamp, excludedTaskKeys: List<TaskKey>): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> {
        val parentTreeDatas = mutableMapOf<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>()

        parentTreeDatas.putAll(localFactory.tasks
                .filter { !excludedTaskKeys.contains(it.taskKey) && it.current(now) && it.isVisible(now) && it.isRootTask(now) }
                .map {
                    val taskParentKey = CreateTaskViewModel.ParentKey.TaskParentKey(it.taskKey)
                    val parentTreeData = CreateTaskViewModel.ParentTreeData(it.name, getTaskListChildTaskDatas(now, it, excludedTaskKeys), taskParentKey, it.getScheduleText(now), it.note, CreateTaskViewModel.SortKey.TaskSortKey(it.startExactTimeStamp))

                    taskParentKey to parentTreeData
                }
                .toMap())

        if (remoteProjectFactory != null) {
            parentTreeDatas.putAll(remoteProjectFactory!!.remoteProjects
                    .values
                    .filter { it.current(now) }
                    .map {
                        val projectParentKey = CreateTaskViewModel.ParentKey.ProjectParentKey(it.id)

                        val users = it.users.joinToString(", ") { it.name }
                        val parentTreeData = CreateTaskViewModel.ParentTreeData(it.name, getProjectTaskTreeDatas(now, it, excludedTaskKeys), projectParentKey, users, null, CreateTaskViewModel.SortKey.ProjectSortKey(it.id))

                        projectParentKey to parentTreeData
                    }
                    .toMap())
        }

        return parentTreeDatas
    }

    private fun getProjectTaskTreeDatas(now: ExactTimeStamp, remoteProject: RemoteProject, excludedTaskKeys: List<TaskKey>): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> {
        return remoteProject.tasks
                .filter { !excludedTaskKeys.contains(it.taskKey) && it.current(now) && it.isVisible(now) && it.isRootTask(now) }
                .map {
                    val taskParentKey = CreateTaskViewModel.ParentKey.TaskParentKey(it.taskKey)
                    val parentTreeData = CreateTaskViewModel.ParentTreeData(it.name, getTaskListChildTaskDatas(now, it, excludedTaskKeys), taskParentKey, it.getScheduleText(now), it.note, CreateTaskViewModel.SortKey.TaskSortKey(it.startExactTimeStamp))

                    taskParentKey to parentTreeData
                }
                .toMap()
    }

    fun convertLocalToRemote(now: ExactTimeStamp, startingLocalTask: LocalTask, projectId: String): RemoteTask {
        check(projectId.isNotEmpty())

        checkNotNull(remoteProjectFactory)
        checkNotNull(userInfo)

        val localToRemoteConversion = LocalToRemoteConversion()
        localFactory.convertLocalToRemoteHelper(localToRemoteConversion, startingLocalTask)

        updateNotifications(true, now, localToRemoteConversion.localTasks
                .values
                .map { it.first.taskKey }, "other")

        val remoteProject = remoteProjectFactory!!.getRemoteProjectForce(projectId)

        for (pair in localToRemoteConversion.localTasks.values) {
            checkNotNull(pair)

            val remoteTask = remoteProject.copyTask(pair.first, pair.second, now)
            localToRemoteConversion.remoteTasks[pair.first.id] = remoteTask
        }

        for (localTaskHierarchy in localToRemoteConversion.localTaskHierarchies) {
            checkNotNull(localTaskHierarchy)

            val parentRemoteTask = localToRemoteConversion.remoteTasks[localTaskHierarchy.parentTaskId]!!
            val childRemoteTask = localToRemoteConversion.remoteTasks[localTaskHierarchy.childTaskId]!!

            val remoteTaskHierarchy = remoteProject.copyLocalTaskHierarchy(localTaskHierarchy, parentRemoteTask.id, childRemoteTask.id)

            localToRemoteConversion.remoteTaskHierarchies.add(remoteTaskHierarchy)
        }

        for (pair in localToRemoteConversion.localTasks.values) {
            pair.second.forEach { it.delete() }

            pair.first.delete()
        }

        return localToRemoteConversion.remoteTasks[startingLocalTask.id]!!
    }

    private fun joinTasks(newParentTask: Task, joinTasks: List<Task>, now: ExactTimeStamp) {
        check(newParentTask.current(now))
        check(joinTasks.size > 1)

        for (joinTask in joinTasks) {
            check(joinTask.current(now))

            if (joinTask.isRootTask(now)) {
                joinTask.getCurrentSchedules(now).forEach { it.setEndExactTimeStamp(now) }
            } else {
                val taskHierarchy = getParentTaskHierarchy(joinTask, now)!!

                taskHierarchy.setEndExactTimeStamp(now)
            }

            newParentTask.addChild(joinTask, now)
        }
    }

    fun getParentTaskHierarchy(childTask: Task, exactTimeStamp: ExactTimeStamp): TaskHierarchy? {
        if (childTask.current(exactTimeStamp)) {
            check(childTask.notDeleted(exactTimeStamp))

            val childTaskKey = childTask.taskKey

            val taskHierarchies = childTask.getTaskHierarchiesByChildTaskKey(childTaskKey).filter { it.current(exactTimeStamp) }

            return if (taskHierarchies.isEmpty()) {
                null
            } else {
                taskHierarchies.single()
            }
        } else {
            // jeśli child task jeszcze nie istnieje, ale będzie utworzony jako child, zwróć ów przyszły hierarchy
            // żeby można było dodawać child instances do past parent instance

            check(childTask.notDeleted(exactTimeStamp))

            val childTaskKey = childTask.taskKey

            val taskHierarchies = childTask.getTaskHierarchiesByChildTaskKey(childTaskKey).filter { it.startExactTimeStamp == childTask.startExactTimeStamp }

            return if (taskHierarchies.isEmpty()) {
                null
            } else {
                taskHierarchies.single()
            }
        }
    }

    private fun getTasks() = (localFactory.tasks.asSequence() + (remoteProjectFactory?.tasks?.asSequence()
            ?: emptySequence()))

    private val customTimes
        get() = localFactory.localCustomTimes.toMutableList<CustomTime>().apply {
            remoteProjectFactory?.let { addAll(it.remoteCustomTimes) }
        }

    fun getTaskForce(taskKey: TaskKey) = if (taskKey.localTaskId != null) {
        check(taskKey.remoteTaskId.isNullOrEmpty())

        localFactory.getTaskForce(taskKey.localTaskId)
    } else {
        check(!taskKey.remoteTaskId.isNullOrEmpty())
        checkNotNull(remoteProjectFactory)

        remoteProjectFactory!!.getTaskForce(taskKey)
    }

    private fun getTaskIfPresent(taskKey: TaskKey) = if (taskKey.localTaskId != null) {
        check(taskKey.remoteTaskId.isNullOrEmpty())

        localFactory.getTaskIfPresent(taskKey.localTaskId)
    } else {
        check(!taskKey.remoteTaskId.isNullOrEmpty())
        checkNotNull(remoteProjectFactory)

        remoteProjectFactory!!.getTaskIfPresent(taskKey)
    }

    fun getChildTaskHierarchies(parentTask: Task, exactTimeStamp: ExactTimeStamp): List<TaskHierarchy> {
        check(parentTask.current(exactTimeStamp))

        return parentTask.getTaskHierarchiesByParentTaskKey(parentTask.taskKey)
                .asSequence()
                .filter { it.current(exactTimeStamp) && it.childTask.current(exactTimeStamp) }
                .sortedBy { it.ordinal }
                .toList()
    }

    private fun getTaskListChildTaskDatas(parentTask: Task, now: ExactTimeStamp): List<TaskListFragment.ChildTaskData> {
        return parentTask.getChildTaskHierarchies(now)
                .asSequence()
                .sortedBy { it.ordinal }
                .map {
                    val childTask = it.childTask

                    TaskListFragment.ChildTaskData(childTask.name, childTask.getScheduleText(now), getTaskListChildTaskDatas(childTask, now), childTask.note, childTask.startExactTimeStamp, childTask.taskKey, HierarchyData(it.taskHierarchyKey, it.ordinal))
                }
                .toList()
    }

    private fun getExistingInstances() = localFactory.existingInstances
            .toMutableList<Instance>()
            .apply {
                remoteProjectFactory?.let { addAll(it.existingInstances) }
            }

    private fun getGroupListChildTaskDatas(parentTask: Task, now: ExactTimeStamp): List<GroupListFragment.TaskData> = parentTask.getChildTaskHierarchies(now)
            .map {
                val childTask = it.childTask

                GroupListFragment.TaskData(childTask.taskKey, childTask.name, getGroupListChildTaskDatas(childTask, now), childTask.startExactTimeStamp, childTask.note)
            }

    fun getMainData(now: ExactTimeStamp): TaskListFragment.TaskData {
        val childTaskDatas = getTasks().filter { it.current(now) && it.isVisible(now) && it.isRootTask(now) }
                .map { TaskListFragment.ChildTaskData(it.name, it.getScheduleText(now), getTaskListChildTaskDatas(it, now), it.note, it.startExactTimeStamp, it.taskKey, null) }
                .sortedDescending()
                .toMutableList()

        return TaskListFragment.TaskData(childTaskDatas, null)
    }

    fun setInstanceDone(now: ExactTimeStamp, dataId: Int, source: SaveService.Source, instanceKey: InstanceKey, done: Boolean): Instance {
        val instance = getInstance(instanceKey)

        instance.setDone(done, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(instance.remoteNullableProject)

        return instance
    }

    fun setIrrelevant(now: ExactTimeStamp): Irrelevant {
        val tasks = getTasks()

        for (task in tasks)
            task.updateOldestVisible(now)

        // relevant hack
        val taskRelevances = tasks.map { it.taskKey to TaskRelevance(this, it) }.toMap()

        val existingInstances = getExistingInstances()
        val rootInstances = getRootInstances(null, now.plusOne(), now)

        val instanceRelevances = (existingInstances + rootInstances)
                .asSequence()
                .distinct()
                .map { it.instanceKey to InstanceRelevance(it) }
                .toList()
                .toMap()
                .toMutableMap()

        val localCustomTimeRelevances = localFactory.localCustomTimes
                .map { it.id to LocalCustomTimeRelevance(it) }
                .toMap()

        tasks.asSequence()
                .filter { it.current(now) && it.isRootTask(now) && it.isVisible(now) }
                .map { taskRelevances[it.taskKey]!! }.toList()
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, localCustomTimeRelevances, now) }

        rootInstances.map { instanceRelevances[it.instanceKey]!! }.forEach { it.setRelevant(taskRelevances, instanceRelevances, localCustomTimeRelevances, now) }

        existingInstances.asSequence()
                .filter { it.isRootInstance(now) && it.isVisible(now) }
                .map { instanceRelevances[it.instanceKey]!! }.toList()
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, localCustomTimeRelevances, now) }

        getCurrentCustomTimes().map { localCustomTimeRelevances[it.id]!! }.forEach { it.setRelevant() }

        val relevantTasks = taskRelevances.values
                .filter { it.relevant }
                .map { it.task }

        val irrelevantTasks = tasks.toMutableList()
        irrelevantTasks.removeAll(relevantTasks)

        check(irrelevantTasks.none { it.isVisible(now) })

        val relevantInstances = instanceRelevances.values
                .filter { it.relevant }
                .map { it.instance }

        val relevantExistingInstances = relevantInstances.filter { it.exists() }

        val irrelevantExistingInstances = ArrayList<Instance>(existingInstances)
        irrelevantExistingInstances.removeAll(relevantExistingInstances)

        check(irrelevantExistingInstances.none { it.isVisible(now) })

        val relevantLocalCustomTimes = localCustomTimeRelevances.values
                .filter { it.relevant }
                .map { it.localCustomTime }

        val irrelevantLocalCustomTimes = ArrayList<LocalCustomTime>(localFactory.localCustomTimes)
        irrelevantLocalCustomTimes.removeAll(relevantLocalCustomTimes)

        check(irrelevantLocalCustomTimes.none { it.current })

        irrelevantExistingInstances.forEach { it.delete() }
        irrelevantTasks.forEach { it.delete() }
        irrelevantLocalCustomTimes.forEach { it.delete() }

        val irrelevantRemoteCustomTimes: MutableList<RemoteCustomTime>?
        val irrelevantRemoteProjects: MutableList<RemoteProject>?
        if (remoteProjectFactory != null) {
            val remoteCustomTimes = remoteProjectFactory!!.remoteCustomTimes
            val remoteCustomTimeRelevances = remoteCustomTimes.map { kotlin.Pair(it.projectId, it.id) to RemoteCustomTimeRelevance(it) }.toMap()

            val remoteProjects = remoteProjectFactory!!.remoteProjects.values
            val remoteProjectRelevances = remoteProjects.map { it.id to RemoteProjectRelevance(it) }.toMap()

            remoteProjects.filter { it.current(now) }
                    .map { remoteProjectRelevances[it.id]!! }
                    .forEach { it.setRelevant() }

            taskRelevances.values
                    .filter { it.relevant }
                    .forEach { it.setRemoteRelevant(remoteCustomTimeRelevances, remoteProjectRelevances) }

            instanceRelevances.values
                    .filter { it.relevant }
                    .forEach { it.setRemoteRelevant(remoteCustomTimeRelevances, remoteProjectRelevances) }

            val relevantRemoteCustomTimes = remoteCustomTimeRelevances.values
                    .filter { it.relevant }
                    .map { it.remoteCustomTime }

            irrelevantRemoteCustomTimes = ArrayList(remoteCustomTimes)
            irrelevantRemoteCustomTimes.removeAll(relevantRemoteCustomTimes)
            irrelevantRemoteCustomTimes.forEach { it.delete() }

            val relevantRemoteProjects = remoteProjectRelevances.values
                    .filter { it.relevant }
                    .map { it.remoteProject }

            irrelevantRemoteProjects = ArrayList(remoteProjects)
            irrelevantRemoteProjects.removeAll(relevantRemoteProjects)
            irrelevantRemoteProjects.forEach { it.delete() }

            val irrelevantInstanceShownRecords = localFactory.instanceShownRecords
                    .toMutableList()
                    .apply { removeAll(relevantInstances.map { it.nullableInstanceShownRecord }) }
            irrelevantInstanceShownRecords.forEach { it.delete() }
        } else {
            irrelevantRemoteCustomTimes = null
            irrelevantRemoteProjects = null
        }

        return Irrelevant(irrelevantLocalCustomTimes, irrelevantTasks, irrelevantExistingInstances, irrelevantRemoteCustomTimes, irrelevantRemoteProjects)
    }

    private fun notifyCloud(remoteProject: RemoteProject?) {
        val remoteProjects = setOf(remoteProject)
                .filterNotNull()
                .toSet()

        notifyCloud(remoteProjects)
    }

    private fun notifyCloud(remoteProjects: Set<RemoteProject>) {
        if (!remoteProjects.isEmpty()) {
            checkNotNull(userInfo)

            BackendNotifier.notify(remoteProjects, userInfo!!, listOf())
        }
    }

    private fun notifyCloud(remoteProject: RemoteProject, userKeys: Collection<String>) = BackendNotifier.notify(setOf(remoteProject), userInfo!!, userKeys)

    private fun updateNotifications(now: ExactTimeStamp) = updateNotifications(true, now, mutableListOf(), "other")

    private val taskKeys
        get() = localFactory.taskIds
                .map { TaskKey(it) }
                .toMutableSet()
                .apply { remoteProjectFactory?.let { addAll(it.taskKeys) } }

    private fun updateNotifications(silent: Boolean, now: ExactTimeStamp, removedTaskKeys: List<TaskKey>, sourceName: String) {
        Preferences.logLineDate("updateNotifications start $sourceName")

        val rootInstances = getRootInstances(null, now.plusOne(), now) // 24 hack

        val notificationInstances = rootInstances.filter { it.done == null && !it.notified && it.instanceDateTime.timeStamp.toExactTimeStamp() <= now && !removedTaskKeys.contains(it.taskKey) }.associateBy { it.instanceKey }

        val shownInstanceKeys = getExistingInstances().filter { it.notificationShown }
                .map { it.instanceKey }
                .toMutableSet()

        val instanceShownRecordNotificationDatas = localFactory.instanceShownRecords
                .filter { it.notificationShown }
                .map { instanceShownRecord ->
                    val scheduleDate = Date(instanceShownRecord.scheduleYear, instanceShownRecord.scheduleMonth, instanceShownRecord.scheduleDay)
                    val remoteCustomTimeId = instanceShownRecord.scheduleCustomTimeId

                    val customTimeKey: CustomTimeKey?
                    val hourMinute: HourMinute?
                    if (!remoteCustomTimeId.isNullOrEmpty()) {
                        check(instanceShownRecord.scheduleHour == null)
                        check(instanceShownRecord.scheduleMinute == null)

                        customTimeKey = getCustomTimeKey(instanceShownRecord.projectId, remoteCustomTimeId)
                        hourMinute = null
                    } else {
                        checkNotNull(instanceShownRecord.scheduleHour)
                        checkNotNull(instanceShownRecord.scheduleMinute)

                        customTimeKey = null
                        hourMinute = HourMinute(instanceShownRecord.scheduleHour, instanceShownRecord.scheduleMinute)
                    }

                    val taskKey = TaskKey(instanceShownRecord.projectId, instanceShownRecord.taskId)
                    val instanceKey = InstanceKey(taskKey, scheduleDate, TimePair(customTimeKey, hourMinute))

                    shownInstanceKeys.add(instanceKey)

                    instanceKey to Pair(Instance.getNotificationId(scheduleDate, customTimeKey, hourMinute, taskKey), instanceShownRecord)
                }
                .toMap()

        val showInstanceKeys = notificationInstances.keys.filter { !shownInstanceKeys.contains(it) }

        val hideInstanceKeys = shownInstanceKeys.filter { !notificationInstances.containsKey(it) }.toSet()

        for (showInstanceKey in showInstanceKeys)
            getInstance(showInstanceKey).setNotificationShown(true, now)

        val allTaskKeys = taskKeys

        for (hideInstanceKey in hideInstanceKeys) {
            if (allTaskKeys.contains(hideInstanceKey.taskKey))
                getInstance(hideInstanceKey).setNotificationShown(false, now)
            else
                instanceShownRecordNotificationDatas[hideInstanceKey]!!.second.notificationShown = false
        }

        var message = ""

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (notificationInstances.size > TickJobIntentService.MAX_NOTIFICATIONS) { // show group
                if (shownInstanceKeys.size > TickJobIntentService.MAX_NOTIFICATIONS) { // group shown
                    if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty()) {
                        NotificationWrapper.instance.notifyGroup(this, notificationInstances.values, silent, now)
                    } else {
                        NotificationWrapper.instance.notifyGroup(this, notificationInstances.values, true, now)
                    }
                } else { // instances shown
                    for (shownInstanceKey in shownInstanceKeys) {
                        if (allTaskKeys.contains(shownInstanceKey.taskKey)) {
                            NotificationWrapper.instance.cancelNotification(getInstance(shownInstanceKey).notificationId)
                        } else {
                            val notificationId = instanceShownRecordNotificationDatas[shownInstanceKey]!!.first

                            NotificationWrapper.instance.cancelNotification(notificationId)
                        }
                    }

                    NotificationWrapper.instance.notifyGroup(this, notificationInstances.values, silent, now)
                }
            } else { // show instances
                if (shownInstanceKeys.size > TickJobIntentService.MAX_NOTIFICATIONS) { // group shown
                    NotificationWrapper.instance.cancelNotification(0)

                    for (instance in notificationInstances.values)
                        notifyInstance(instance, silent, now)
                } else { // instances shown
                    for (hideInstanceKey in hideInstanceKeys) {
                        if (allTaskKeys.contains(hideInstanceKey.taskKey)) {
                            NotificationWrapper.instance.cancelNotification(getInstance(hideInstanceKey).notificationId)
                        } else {
                            val notificationId = instanceShownRecordNotificationDatas[hideInstanceKey]!!.first

                            NotificationWrapper.instance.cancelNotification(notificationId)
                        }
                    }

                    for (showInstanceKey in showInstanceKeys)
                        notifyInstance(notificationInstances[showInstanceKey]!!, silent, now)

                    notificationInstances.values
                            .filter { !showInstanceKeys.contains(it.instanceKey) }
                            .forEach { updateInstance(it, now) }
                }
            }
        } else {
            if (notificationInstances.isEmpty()) {
                message += ", hg"
                NotificationWrapper.instance.cancelNotification(0)
            } else {
                message += ", sg"
                NotificationWrapper.instance.notifyGroup(this, notificationInstances.values, true, now)
            }

            message += ", hiding " + hideInstanceKeys.size
            for (hideInstanceKey in hideInstanceKeys) {
                if (allTaskKeys.contains(hideInstanceKey.taskKey)) {
                    NotificationWrapper.instance.cancelNotification(getInstance(hideInstanceKey).notificationId)
                } else {
                    val notificationId = instanceShownRecordNotificationDatas[hideInstanceKey]!!.first

                    NotificationWrapper.instance.cancelNotification(notificationId)
                }
            }

            message += ", s " + showInstanceKeys.size
            for (showInstanceKey in showInstanceKeys)
                notifyInstance(notificationInstances[showInstanceKey]!!, silent, now)

            val updateInstances = notificationInstances.values.filter { !showInstanceKeys.contains(it.instanceKey) }

            message += ", u " + updateInstances.size
            updateInstances.forEach { updateInstance(it, now) }
        }

        Preferences.logLineHour("s? " + (if (silent) "t" else "f") + message)

        if (!silent)
            Preferences.lastTick = now.long

        var nextAlarm = getExistingInstances().map { it.instanceDateTime.timeStamp }
                .filter { it.toExactTimeStamp() > now }
                .min()

        val minSchedulesTimeStamp = getTasks().filter { it.current(now) && it.isRootTask(now) }
                .toList()
                .flatMap { it.getCurrentSchedules(now) }
                .mapNotNull { it.getNextAlarm(now) }
                .min()

        if (minSchedulesTimeStamp != null && (nextAlarm == null || nextAlarm > minSchedulesTimeStamp))
            nextAlarm = minSchedulesTimeStamp

        NotificationWrapper.instance.updateAlarm(nextAlarm)

        if (nextAlarm != null)
            Preferences.logLineHour("next tick: $nextAlarm")

        Preferences.logLineHour("updateNotifications stop $sourceName")
    }

    private fun notifyInstance(instance: Instance, silent: Boolean, now: ExactTimeStamp) {
        var reallySilent = silent
        val realtime = SystemClock.elapsedRealtime()

        val optional = lastNotificationBeeps.values.max()
        if (optional?.let { realtime - it < 5000 } == true) {
            Log.e("asdf", "skipping notification sound for " + instance.name)

            reallySilent = true
        }

        NotificationWrapper.instance.notifyInstance(this, instance, reallySilent, now)

        if (!reallySilent)
            lastNotificationBeeps[instance.instanceKey] = SystemClock.elapsedRealtime()
    }

    private fun updateInstance(instance: Instance, now: ExactTimeStamp) {
        val instanceKey = instance.instanceKey

        val realtime = SystemClock.elapsedRealtime()

        if (lastNotificationBeeps.containsKey(instanceKey)) {
            val then = lastNotificationBeeps[instanceKey]!!

            check(realtime > then)

            if (realtime - then < 5000) {
                Log.e("asdf", "skipping notification update for " + instance.name)

                return
            }
        }

        NotificationWrapper.instance.notifyInstance(this, instance, true, now)
    }

    private fun setInstanceNotified(instanceKey: InstanceKey, now: ExactTimeStamp) {
        if (instanceKey.type === TaskKey.Type.LOCAL) {
            val instance = getInstance(instanceKey)

            instance.setNotified(now)
            instance.setNotificationShown(false, now)
        } else {
            val taskKey = instanceKey.taskKey

            val projectId = taskKey.remoteProjectId
            check(!projectId.isNullOrEmpty())

            val taskId = taskKey.remoteTaskId
            check(!taskId.isNullOrEmpty())

            val scheduleKey = instanceKey.scheduleKey
            val scheduleDate = scheduleKey.scheduleDate

            val stream = localFactory.instanceShownRecords
                    .asSequence()
                    .filter { it.projectId == projectId && it.taskId == taskId && it.scheduleYear == scheduleDate.year && it.scheduleMonth == scheduleDate.month && it.scheduleDay == scheduleDate.day }

            val matches: Sequence<InstanceShownRecord>
            if (scheduleKey.scheduleTimePair.customTimeKey != null) {
                check(scheduleKey.scheduleTimePair.hourMinute == null)

                check(scheduleKey.scheduleTimePair.customTimeKey is CustomTimeKey.RemoteCustomTimeKey) // remote custom time key hack
                check(projectId == scheduleKey.scheduleTimePair.customTimeKey.remoteProjectId)

                val customTimeId = scheduleKey.scheduleTimePair.customTimeKey.remoteCustomTimeId

                matches = stream.filter { customTimeId == it.scheduleCustomTimeId }
            } else {
                check(scheduleKey.scheduleTimePair.hourMinute != null)

                val hourMinute = scheduleKey.scheduleTimePair.hourMinute

                matches = stream.filter { hourMinute.hour == it.scheduleHour && hourMinute.minute == it.scheduleMinute }
            }

            val instanceShownRecord = matches.single()

            instanceShownRecord.notified = true
            instanceShownRecord.notificationShown = false
        }
    }

    private fun getGroupListData(timeStamp: TimeStamp, now: ExactTimeStamp): GroupListFragment.DataWrapper {
        val endCalendar = timeStamp.calendar.apply { add(Calendar.MINUTE, 1) }
        val endTimeStamp = TimeStamp(endCalendar)

        val rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now)

        val currentInstances = rootInstances.filter { it.instanceDateTime.timeStamp.compareTo(timeStamp) == 0 }

        val customTimeDatas = getCurrentCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val instanceDatas = HashMap<InstanceKey, GroupListFragment.InstanceData>()
        for (instance in currentInstances) {
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)
            val instanceData = GroupListFragment.InstanceData(instance.done, instance.instanceKey, null, instance.name, instance.instanceDateTime.timeStamp, task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.instanceDateTime.time.timePair, task.note, children, null, instance.ordinal)
            children.values.forEach { it.instanceDataParent = instanceData }
            instanceDatas[instance.instanceKey] = instanceData
        }

        val dataWrapper = GroupListFragment.DataWrapper(customTimeDatas, null, listOf(), null, instanceDatas)

        instanceDatas.values.forEach { it.instanceDataParent = dataWrapper }

        return dataWrapper
    }

    private fun getGroupListData(instance: Instance, task: Task, now: ExactTimeStamp): GroupListFragment.DataWrapper {
        val customTimeDatas = getCurrentCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val instanceDatas = instance.getChildInstances(now)
                .map { (childInstance, taskHierarchy) ->
                    val childTask = childInstance.task

                    val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

                    val children = getChildInstanceDatas(childInstance, now)
                    val instanceData = GroupListFragment.InstanceData(childInstance.done, childInstance.instanceKey, null, childInstance.name, childInstance.instanceDateTime.timeStamp, childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), childInstance.instanceDateTime.time.timePair, childTask.note, children, HierarchyData(taskHierarchy.taskHierarchyKey, taskHierarchy.ordinal), childInstance.ordinal)
                    children.values.forEach { it.instanceDataParent = instanceData }
                    childInstance.instanceKey to instanceData
                }
                .toMap()
                .toMutableMap()

        val dataWrapper = GroupListFragment.DataWrapper(customTimeDatas, task.current(now), listOf(), task.note, instanceDatas)

        instanceDatas.values.forEach { it.instanceDataParent = dataWrapper }

        return dataWrapper
    }

    fun getCustomTimeKey(remoteProjectId: String, remoteCustomTimeId: String) = localFactory.getLocalCustomTime(remoteProjectId, remoteCustomTimeId)?.customTimeKey
            ?: CustomTimeKey.RemoteCustomTimeKey(remoteProjectId, remoteCustomTimeId)
}