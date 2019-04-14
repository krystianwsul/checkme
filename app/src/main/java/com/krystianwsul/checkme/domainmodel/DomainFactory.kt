package com.krystianwsul.checkme.domainmodel

import android.os.Build
import com.androidhuman.rxfirebase2.database.ChildEvent
import com.google.firebase.database.DataSnapshot
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.relevance.*
import com.krystianwsul.checkme.firebase.*
import com.krystianwsul.checkme.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.checkme.gui.HierarchyData
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.SnackbarListener
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.checkme.persistencemodel.PersistenceManager
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.*
import java.util.*

@Suppress("LeakingThis")
open class DomainFactory(
        persistenceManager: PersistenceManager,
        private var userInfo: UserInfo,
        remoteStart: ExactTimeStamp,
        sharedSnapshot: DataSnapshot,
        privateSnapshot: DataSnapshot,
        userSnapshot: DataSnapshot,
        friendSnapshot: DataSnapshot) {

    companion object {

        val instanceRelay = BehaviorRelay.createDefault(NullableWrapper<DomainFactory>())

        val nullableInstance get() = instanceRelay.value!!.value

        val instance get() = instanceRelay.value!!.value!!

        private val firebaseListeners = mutableListOf<(DomainFactory) -> Unit>()

        @Synchronized // still running?
        fun setFirebaseTickListener(source: SaveService.Source, newTickData: TickData): Boolean {
            check(MyApplication.instance.hasUserInfo)

            val domainFactory = nullableInstance

            val savedFalse = domainFactory?.remoteProjectFactory?.eitherSaved == false

            Preferences.logLineHour("DomainFactory.setFirebaseTickListener savedFalse: $savedFalse")

            val tickData = TickHolder.getTickData()

            if (savedFalse) {
                val silent = (tickData?.silent ?: true) && newTickData.silent

                domainFactory!!.updateNotificationsTick(source, silent, newTickData.source)
            }

            return if (!savedFalse || tickData?.privateRefreshed == false || tickData?.sharedRefreshed == false) {
                TickHolder.addTickData(newTickData)

                true
            } else {
                tickData?.release()
                newTickData.release()

                false
            }
        }

        @Synchronized
        fun addFirebaseListener(firebaseListener: (DomainFactory) -> Unit) {
            val domainFactory = nullableInstance
            if (domainFactory?.remoteProjectFactory?.eitherSaved == false && !domainFactory.remoteFriendFactory.isSaved)
                firebaseListener(domainFactory)
            else
                firebaseListeners.add(firebaseListener)
        }
    }

    val localReadTimes: ReadTimes

    var remoteReadTimes: ReadTimes
        private set

    var remoteUpdateTime: Long? = null
        private set

    val localFactory: LocalFactory

    var remoteProjectFactory: RemoteProjectFactory
        private set

    private val remoteUserFactory: RemoteUserFactory

    var remoteFriendFactory: RemoteFriendFactory
        private set

    private var skipSave = false

    val domainChanged = BehaviorRelay.createDefault(listOf<Int>())

    private var firstTaskEvent = true

    init {
        val start = ExactTimeStamp.now

        localFactory = LocalFactory(persistenceManager)

        val localRead = ExactTimeStamp.now

        localFactory.initialize(this)

        val stop = ExactTimeStamp.now

        localReadTimes = ReadTimes(start, localRead, stop)

        val remoteRead = ExactTimeStamp.now

        remoteProjectFactory = RemoteProjectFactory(this, sharedSnapshot.children, privateSnapshot, userInfo, remoteRead)

        remoteReadTimes = ReadTimes(remoteStart, remoteRead, ExactTimeStamp.now)

        remoteUserFactory = RemoteUserFactory(this, userSnapshot, userInfo)
        remoteUserFactory.remoteUser.setToken(userInfo.token)

        remoteFriendFactory = RemoteFriendFactory(this, friendSnapshot.children)

        tryNotifyListeners("DomainFactory.init")
    }

    private val defaultProjectId by lazy { remoteProjectFactory.remotePrivateProject.id }

    // misc

    val isHoldingWakeLock get() = TickHolder.isHeld

    val taskCount get() = remoteProjectFactory.taskCount
    val instanceCount get() = remoteProjectFactory.instanceCount

    val customTimeCount get() = customTimes.size

    val instanceShownCount get() = localFactory.instanceShownRecords.size

    val uuid get() = localFactory.uuid

    fun save(dataId: Int, source: SaveService.Source) = save(listOf(dataId), source)

    fun save(dataIds: List<Int>, source: SaveService.Source) {
        if (skipSave)
            return

        val localChanges = localFactory.save(source)
        val remoteChanges = remoteProjectFactory.save()
        val userChanges = remoteUserFactory.save()

        if (localChanges || remoteChanges || userChanges)
            domainChanged.accept(dataIds)
    }

    // firebase

    @Synchronized
    fun clearUserInfo() = updateNotifications(ExactTimeStamp.now, true)

    @Synchronized
    fun updatePrivateProjectRecord(dataSnapshot: DataSnapshot) {
        MyCrashlytics.log("updatePrivateProjectRecord")

        val start = ExactTimeStamp.now

        if (remoteProjectFactory.isPrivateSaved) {
            remoteProjectFactory.isPrivateSaved = false
            return
        }

        remoteProjectFactory.onNewPrivate(dataSnapshot, ExactTimeStamp.now)

        val stop = ExactTimeStamp.now

        remoteUpdateTime = stop.long - start.long

        TickHolder.getTickData()?.privateRefreshed = true

        tryNotifyListeners("DomainFactory.updatePrivateProjectRecord")
    }

    @Synchronized
    fun updateSharedProjectRecords(childEvent: ChildEvent) {
        MyCrashlytics.log("updateSharedProjectRecord")

        if (remoteProjectFactory.isSharedSaved) {
            remoteProjectFactory.isSharedSaved = false
            return
        }

        remoteProjectFactory.onChildEvent(childEvent, ExactTimeStamp.now)

        TickHolder.getTickData()?.sharedRefreshed = true

        tryNotifyListeners("DomainFactory.updateSharedProjectRecords")
    }

    private fun tryNotifyListeners(source: String) {
        if (remoteProjectFactory.eitherSaved || remoteFriendFactory.isSaved)
            return

        skipSave = true

        firebaseListeners.forEach { it.invoke(this) }
        firebaseListeners.clear()

        val tickData = TickHolder.getTickData()
        if (tickData == null) {
            updateNotifications(firstTaskEvent, ExactTimeStamp.now, listOf(), source)
        } else {
            updateNotificationsTick(SaveService.Source.GUI, tickData.silent, tickData.source)

            if (tickData.privateRefreshed && tickData.sharedRefreshed)
                tickData.release()
        }

        firstTaskEvent = false

        skipSave = false
        save(0, SaveService.Source.GUI)
    }

    @Synchronized
    fun updateUserRecord(dataSnapshot: DataSnapshot) {
        MyCrashlytics.log("updateUserRecord")

        if (remoteUserFactory.isSaved) {
            remoteUserFactory.isSaved = false
            return
        }

        remoteUserFactory.onNewSnapshot(dataSnapshot)

        tryNotifyListeners("DomainFactory.updateUserRecord")
    }

    @Synchronized
    fun setFriendRecords(dataSnapshot: DataSnapshot) {
        if (remoteFriendFactory.isSaved) {
            remoteFriendFactory.isSaved = false
            return
        }

        remoteFriendFactory = RemoteFriendFactory(this, dataSnapshot.children)

        tryNotifyListeners("DomainFactory.setFriendRecords")
    }

    // gets

    @Synchronized
    fun getEditInstanceData(instanceKey: InstanceKey): EditInstanceViewModel.Data {
        MyCrashlytics.log("DomainFactory.getEditInstanceData")

        val now = ExactTimeStamp.now

        val currentCustomTimes = getCurrentRemoteCustomTimes().associateBy { it.customTimeKey }.toMutableMap<CustomTimeKey<*>, RemoteCustomTime<*>>()

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

        check(instanceKeys.isNotEmpty())

        val now = ExactTimeStamp.now

        val currentCustomTimes = getCurrentRemoteCustomTimes().associateBy { it.customTimeKey }.toMutableMap<CustomTimeKey<*>, CustomTime>()

        val instanceDatas = mutableMapOf<InstanceKey, EditInstancesViewModel.InstanceData>()

        for (instanceKey in instanceKeys) {
            val instance = getInstance(instanceKey)
            check(instance.isRootInstance(now))
            check(instance.done == null)

            instanceDatas[instanceKey] = EditInstancesViewModel.InstanceData(instance.instanceDateTime, instance.name, instance.done != null)

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
    fun getShowCustomTimeData(customTimeId: RemoteCustomTimeId.Private): ShowCustomTimeViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowCustomTimeData")

        val customTime = remoteProjectFactory.remotePrivateProject.getRemoteCustomTime(customTimeId)

        val hourMinutes = DayOfWeek.values().associate { it to customTime.getHourMinute(it) }

        return ShowCustomTimeViewModel.Data(customTime.id, customTime.name, hourMinutes)
    }

    @Synchronized
    fun getShowCustomTimesData(): ShowCustomTimesViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowCustomTimesData")

        val entries = getCurrentRemoteCustomTimes().map { ShowCustomTimesViewModel.CustomTimeData(it.id, it.name) }.toMutableList()

        return ShowCustomTimesViewModel.Data(entries)
    }

    @Synchronized
    fun getGroupListData(now: ExactTimeStamp, position: Int, timeRange: MainActivity.TimeRange): DayViewModel.DayData {
        MyCrashlytics.log("DomainFactory.getGroupListData")

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

        val customTimeDatas = getCurrentRemoteCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val taskDatas = if (position == 0) {
            getTasks().filter { it.current(now) && it.isVisible(now, true) && it.isRootTask(now) && it.getCurrentSchedules(now).isEmpty() }
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
            val instanceData = GroupListFragment.InstanceData(instance.done, instance.instanceKey, instance.getDisplayText(now), instance.name, instance.instanceDateTime.timeStamp, task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.instanceDateTime.time.timePair, task.note, children, null, instance.ordinal, instance.notificationShown)
            children.values.forEach { it.instanceDataParent = instanceData }
            instanceDatas[instanceData.instanceKey] = instanceData
        }

        val dataWrapper = GroupListFragment.DataWrapper(customTimeDatas, null, taskDatas, null, instanceDatas, null)
        val data = DayViewModel.DayData(dataWrapper)

        instanceDatas.values.forEach { it.instanceDataParent = dataWrapper }

        return data
    }

    @Synchronized
    fun getShowGroupData(timeStamp: TimeStamp): ShowGroupViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowGroupData")

        val now = ExactTimeStamp.now

        val date = timeStamp.date
        val dayOfWeek = date.dayOfWeek
        val hourMinute = timeStamp.hourMinute

        val time = getCurrentRemoteCustomTimes().firstOrNull { it.getHourMinute(dayOfWeek) == hourMinute }
                ?: NormalTime(hourMinute)

        val displayText = DateTime(date, time).getDisplayText()

        return ShowGroupViewModel.Data(displayText, getGroupListData(timeStamp, now))
    }

    @Synchronized
    fun getShowTaskInstancesData(taskKey: TaskKey): ShowTaskInstancesViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowTaskInstancesData")

        val task = getTaskForce(taskKey)
        val now = ExactTimeStamp.now

        val customTimeDatas = getCurrentRemoteCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val isRootTask = if (task.current(now)) task.isRootTask(now) else null

        val instances = task.existingInstances.values + task.getInstances(null, now, now)

        val instanceDatas = instances.associate {
            val children = getChildInstanceDatas(it, now)

            val hierarchyData = if (task.isRootTask(now)) {
                null
            } else {
                val taskHierarchy = getParentTaskHierarchy(task, now)!!

                HierarchyData(taskHierarchy.taskHierarchyKey, taskHierarchy.ordinal)
            }

            it.instanceKey to GroupListFragment.InstanceData(it.done, it.instanceKey, it.getDisplayText(now), it.name, it.instanceDateTime.timeStamp, task.current(now), it.isRootInstance(now), isRootTask, it.exists(), it.instanceDateTime.time.timePair, task.note, children, hierarchyData, it.ordinal, it.notificationShown)
        }.toMutableMap()

        return ShowTaskInstancesViewModel.Data(GroupListFragment.DataWrapper(
                customTimeDatas,
                task.current(now),
                listOf(),
                null,
                instanceDatas,
                null))
    }

    @Synchronized
    fun getShowNotificationGroupData(instanceKeys: Set<InstanceKey>): ShowNotificationGroupViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowNotificationGroupData")

        check(instanceKeys.isNotEmpty())

        val now = ExactTimeStamp.now

        val instances = instanceKeys.map { getInstance(it) }
                .filter { it.isRootInstance(now) }
                .sortedBy { it.instanceDateTime }

        val customTimeDatas = getCurrentRemoteCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val instanceDatas = instances.associate { instance ->
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)
            val instanceData = GroupListFragment.InstanceData(instance.done, instance.instanceKey, instance.getDisplayText(now), instance.name, instance.instanceDateTime.timeStamp, task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.instanceDateTime.time.timePair, task.note, children, null, instance.ordinal, instance.notificationShown)
            children.values.forEach { it.instanceDataParent = instanceData }
            instance.instanceKey to instanceData
        }.toMutableMap()

        val dataWrapper = GroupListFragment.DataWrapper(customTimeDatas, null, listOf(), null, instanceDatas, null)

        instanceDatas.values.forEach { it.instanceDataParent = dataWrapper }

        return ShowNotificationGroupViewModel.Data(dataWrapper)
    }

    @Synchronized
    fun getShowInstanceData(instanceKey: InstanceKey): ShowInstanceViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowInstanceData")

        val task = getTaskForce(instanceKey.taskKey)

        val now = ExactTimeStamp.now

        val instance = getInstance(instanceKey)
        val instanceDateTime = instance.instanceDateTime
        val parentInstance = instance.getParentInstance(now)
        val displayText = parentInstance?.name ?: instanceDateTime.getDisplayText()
        return ShowInstanceViewModel.Data(
                instance.name,
                instanceDateTime,
                instance.done != null,
                task.current(now),
                parentInstance == null,
                instance.exists(),
                getGroupListData(instance, task, now, true),
                instance.notificationShown,
                displayText)
    }

    fun getScheduleDatas(schedules: List<Schedule>, now: ExactTimeStamp): Pair<Map<CustomTimeKey<*>, CustomTime>, Map<CreateTaskViewModel.ScheduleData, List<Schedule>>> {
        val customTimes = HashMap<CustomTimeKey<*>, CustomTime>()

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

        return Pair<Map<CustomTimeKey<*>, CustomTime>, Map<CreateTaskViewModel.ScheduleData, List<Schedule>>>(customTimes, scheduleDatas)
    }

    @Synchronized
    fun getCreateTaskData(taskKey: TaskKey?, joinTaskKeys: List<TaskKey>?, parentTaskKeyHint: TaskKey?): CreateTaskViewModel.Data {
        MyCrashlytics.logMethod(this, "parentTaskKeyHint: $parentTaskKeyHint")

        check(taskKey == null || joinTaskKeys == null)

        val now = ExactTimeStamp.now

        val customTimes = getCurrentRemoteCustomTimes().associateBy { it.customTimeKey }.toMutableMap<CustomTimeKey<*>, CustomTime>()

        val excludedTaskKeys = when {
            taskKey != null -> setOf(taskKey)
            joinTaskKeys != null -> joinTaskKeys.toSet()
            else -> setOf()
        }

        val includeTaskKeys = listOfNotNull(parentTaskKeyHint).toMutableSet()

        fun checkHintPresent(taskParentKey: CreateTaskViewModel.ParentKey.TaskParentKey, parentTreeDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>): Boolean {
            return parentTreeDatas.containsKey(taskParentKey) || parentTreeDatas.any { checkHintPresent(taskParentKey, it.value.parentTreeDatas) }
        }

        fun checkHintPresent(parentTreeDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>) = parentTaskKeyHint?.let { checkHintPresent(CreateTaskViewModel.ParentKey.TaskParentKey(it), parentTreeDatas) }
                ?: true

        var taskData: CreateTaskViewModel.TaskData? = null
        val parentTreeDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>
        if (taskKey != null) {
            val task = getTaskForce(taskKey)

            val parentKey: CreateTaskViewModel.ParentKey?
            var scheduleDatas: List<CreateTaskViewModel.ScheduleData>? = null

            if (task.isRootTask(now)) {
                val schedules = task.getCurrentSchedules(now)

                parentKey = task.project.takeIf { it is RemoteSharedProject }?.let { CreateTaskViewModel.ParentKey.ProjectParentKey(it.id) }

                if (schedules.isNotEmpty()) {
                    val pair = getScheduleDatas(schedules, now)
                    customTimes.putAll(pair.first)
                    scheduleDatas = pair.second.keys.toList()
                }
            } else {
                val parentTask = task.getParentTask(now)!!
                parentKey = CreateTaskViewModel.ParentKey.TaskParentKey(parentTask.taskKey)
                includeTaskKeys.add(parentTask.taskKey)
            }

            val projectName = task.project.name

            taskData = CreateTaskViewModel.TaskData(task.name, parentKey, scheduleDatas, task.note, projectName, task.image)

            parentTreeDatas = getParentTreeDatas(now, excludedTaskKeys, includeTaskKeys)
            check(checkHintPresent(parentTreeDatas))
        } else {
            var projectId: String? = null
            if (joinTaskKeys != null) {
                check(joinTaskKeys.size > 1)

                val projectIds = joinTaskKeys.map { it.remoteProjectId }.distinct()

                projectId = projectIds.single()
            }

            parentTreeDatas = if (!projectId.isNullOrEmpty()) {
                val remoteProject = remoteProjectFactory.getRemoteProjectForce(projectId)

                getProjectTaskTreeDatas(now, remoteProject, excludedTaskKeys, includeTaskKeys)
            } else {
                getParentTreeDatas(now, excludedTaskKeys, includeTaskKeys)
            }
            check(checkHintPresent(parentTreeDatas))
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

        return ShowTaskViewModel.Data(
                task.name,
                task.getScheduleText(now, true),
                TaskListFragment.TaskData(childTaskDatas.toMutableList(), task.note),
                task.existingInstances.values.isNotEmpty() || task.getInstances(null, now, now).isNotEmpty(),
                task.image)
    }

    @Synchronized
    fun getMainData(): MainViewModel.Data {
        MyCrashlytics.log("DomainFactory.getMainData")

        val now = ExactTimeStamp.now

        return MainViewModel.Data(getMainData(now))
    }

    @Synchronized
    fun getDrawerData(): DrawerViewModel.Data {
        MyCrashlytics.log("DomainFactory.getDrawerData")

        return remoteUserFactory.remoteUser.run { DrawerViewModel.Data(name, email, photoUrl) }
    }

    @Synchronized
    fun getProjectListData(): ProjectListViewModel.Data {
        MyCrashlytics.log("DomainFactory.getProjectListData")

        val remoteProjects = remoteProjectFactory.remoteSharedProjects

        val now = ExactTimeStamp.now

        val projectDatas = remoteProjects.values
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

        val friends = remoteFriendFactory.friends

        val userListDatas = friends.map { FriendListViewModel.UserListData(it.name, it.email, it.id, it.photoUrl) }.toMutableSet()

        return FriendListViewModel.Data(userListDatas)
    }

    @Synchronized
    fun getShowProjectData(projectId: String?): ShowProjectViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowProjectData")

        val friendDatas = remoteFriendFactory.friends
                .map { ShowProjectViewModel.UserListData(it.name, it.email, it.id, it.photoUrl) }
                .associateBy { it.id }

        val name: String?
        val userListDatas: Set<ShowProjectViewModel.UserListData>
        if (!projectId.isNullOrEmpty()) {
            val remoteProject = remoteProjectFactory.getRemoteProjectForce(projectId) as RemoteSharedProject

            name = remoteProject.name

            userListDatas = remoteProject.users
                    .filterNot { it.id == userInfo.key }
                    .map { ShowProjectViewModel.UserListData(it.name, it.email, it.id, it.photoUrl) }
                    .toSet()
        } else {
            name = null
            userListDatas = setOf()
        }

        return ShowProjectViewModel.Data(name, userListDatas, friendDatas)
    }

    // sets

    @Synchronized
    fun setInstancesDateTime(dataId: Int, source: SaveService.Source, instanceKeys: Set<InstanceKey>, instanceDate: Date, instanceTimePair: TimePair) {
        MyCrashlytics.log("DomainFactory.setInstancesDateTime")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(instanceKeys.isNotEmpty())

        val now = ExactTimeStamp.now

        val instances = instanceKeys.map(this::getInstance)

        instances.forEach { it.setInstanceDateTime(instanceDate, instanceTimePair, now) }

        val remoteProjects = instances
                .filter { it.belongsToRemoteProject() }
                .map { it.project }
                .toSet()

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)
    }

    @Synchronized
    fun setInstanceAddHourService(source: SaveService.Source, instanceKey: InstanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHourService")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val instance = getInstance(instanceKey)

        val now = ExactTimeStamp.now
        val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

        val date = Date(calendar)
        val hourMinute = HourMinute(calendar)

        instance.setInstanceDateTime(date, TimePair(hourMinute), now)
        instance.notificationShown = false

        updateNotifications(now)

        save(0, source)

        notifyCloud(instance.project)
    }

    @Synchronized
    fun setInstancesAddHourActivity(dataId: Int, source: SaveService.Source, instanceKeys: Collection<InstanceKey>): HourUndoData {
        MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now
        val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

        val date = Date(calendar)
        val hourMinute = HourMinute(calendar)
        val timePair = TimePair(hourMinute)

        val instances = instanceKeys.map(this::getInstance)

        val instanceDateTimes = instances.associate { it.instanceKey to it.instanceDateTime }

        instances.forEach { it.setInstanceDateTime(date, timePair, now) }

        updateNotifications(now)

        save(dataId, source)

        val remoteProjects = instances.map { it.project }.toSet()

        notifyCloud(remoteProjects)

        return HourUndoData(instanceDateTimes)
    }

    @Synchronized
    fun undoInstancesAddHour(dataId: Int, source: SaveService.Source, hourUndoData: HourUndoData) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val pairs = hourUndoData.instanceDateTimes.map { (instanceKey, instanceDateTime) -> Pair(getInstance(instanceKey), instanceDateTime) }

        pairs.forEach { (instance, instanceDateTime) ->
            instance.setInstanceDateTime(instanceDateTime.date, instanceDateTime.time.timePair, now)
        }

        updateNotifications(now)

        save(dataId, source)

        val remoteProjects = pairs.map { it.first.project }.toSet()

        notifyCloud(remoteProjects)
    }

    @Synchronized
    fun setInstanceNotificationDone(source: SaveService.Source, instanceKey: InstanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotificationDone")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val instance = getInstance(instanceKey)

        val now = ExactTimeStamp.now

        instance.setDone(true, now)
        instance.notificationShown = false

        updateNotifications(now)

        save(0, source)

        notifyCloud(instance.project)
    }

    @Synchronized
    fun setInstanceDone(dataId: Int, source: SaveService.Source, instanceKey: InstanceKey, done: Boolean): ExactTimeStamp? {
        MyCrashlytics.log("DomainFactory.setInstanceDone")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val instance = setInstanceDone(now, dataId, source, instanceKey, done)

        return instance.done
    }

    @Synchronized
    fun setInstancesNotNotified(dataId: Int, source: SaveService.Source, instanceKeys: List<InstanceKey>) {
        MyCrashlytics.log("DomainFactory.setInstancesNotNotified")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        instanceKeys.forEach {
            val instance = getInstance(it)
            check(instance.done == null)
            check(instance.instanceDateTime.timeStamp.toExactTimeStamp() <= now)
            check(!instance.notificationShown)
            check(instance.isRootInstance(now))

            instance.notified = false
            instance.notificationShown = false
        }

        updateNotifications(now)

        save(dataId, source)
    }

    @Synchronized
    fun setInstancesDone(dataId: Int, source: SaveService.Source, instanceKeys: List<InstanceKey>, done: Boolean): ExactTimeStamp {
        MyCrashlytics.log("DomainFactory.setInstancesDone")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val instances = instanceKeys.map(this::getInstance)

        instances.forEach { it.setDone(done, now) }

        val remoteProjects = instances.mapNotNull(Instance::project).toSet()

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)

        return now
    }

    @Synchronized
    fun setInstanceNotified(dataId: Int, source: SaveService.Source, instanceKey: InstanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotified")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        setInstanceNotified(instanceKey)

        save(dataId, source)
    }

    @Synchronized
    fun setInstancesNotified(source: SaveService.Source, instanceKeys: List<InstanceKey>) {
        MyCrashlytics.log("DomainFactory.setInstancesNotified")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(instanceKeys.isNotEmpty())

        for (instanceKey in instanceKeys)
            setInstanceNotified(instanceKey)

        save(0, source)
    }

    fun createScheduleRootTask(
            now: ExactTimeStamp,
            dataId: Int,
            source: SaveService.Source,
            name: String,
            scheduleDatas: List<CreateTaskViewModel.ScheduleData>,
            note: String?,
            projectId: String?,
            imagePath: String?): Task {
        check(name.isNotEmpty())
        check(scheduleDatas.isNotEmpty())

        val finalProjectId = projectId.takeUnless { it.isNullOrEmpty() } ?: defaultProjectId

        val imageUuid = imagePath?.let { newUuid() }

        val task = remoteProjectFactory.createScheduleRootTask(now, name, scheduleDatas, note, finalProjectId, imageUuid)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.project)

        imageUuid?.let {
            Uploader.addUpload(task.taskKey, it, imagePath)
        }

        return task
    }

    @Synchronized
    fun createScheduleRootTask(dataId: Int, source: SaveService.Source, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?, projectId: String?, imagePath: String?) {
        MyCrashlytics.log("DomainFactory.createScheduleRootTask")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        createScheduleRootTask(now, dataId, source, name, scheduleDatas, note, projectId, imagePath)
    }

    fun updateScheduleTask(now: ExactTimeStamp, dataId: Int, source: SaveService.Source, taskKey: TaskKey, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?, projectId: String?): TaskKey {
        check(name.isNotEmpty())
        check(scheduleDatas.isNotEmpty())

        var task = getTaskForce(taskKey)
        check(task.current(now))

        val finalProjectId = projectId.takeUnless { it.isNullOrEmpty() } ?: defaultProjectId

        task = task.updateProject(now, finalProjectId)

        task.setName(name, note)

        if (!task.isRootTask(now))
            getParentTaskHierarchy(task, now)!!.setEndExactTimeStamp(now)

        task.updateSchedules(scheduleDatas, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.project)

        return task.taskKey
    }

    @Synchronized
    fun updateScheduleTask(dataId: Int, source: SaveService.Source, taskKey: TaskKey, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?, projectId: String?): TaskKey {
        MyCrashlytics.log("DomainFactory.updateScheduleTask")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(name.isNotEmpty())
        check(scheduleDatas.isNotEmpty())

        val now = ExactTimeStamp.now

        return updateScheduleTask(now, dataId, source, taskKey, name, scheduleDatas, note, projectId)
    }

    @Synchronized
    fun createScheduleJoinRootTask(
            now: ExactTimeStamp,
            dataId: Int,
            source: SaveService.Source,
            name: String,
            scheduleDatas: List<CreateTaskViewModel.ScheduleData>,
            joinTaskKeys: List<TaskKey>,
            note: String?,
            projectId: String?,
            imagePath: String?) {
        MyCrashlytics.log("DomainFactory.createScheduleJoinRootTask")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(name.isNotEmpty())
        check(scheduleDatas.isNotEmpty())
        check(joinTaskKeys.size > 1)

        val finalProjectId = projectId.takeUnless { it.isNullOrEmpty() }
                ?: joinTaskKeys.map { it.remoteProjectId }
                        .distinct()
                        .single()

        var joinTasks = joinTaskKeys.map { getTaskForce(it) }

        val imageUuid = imagePath?.let { newUuid() }

        val newParentTask = remoteProjectFactory.createScheduleRootTask(now, name, scheduleDatas, note, finalProjectId, imageUuid)

        joinTasks = joinTasks.map { it.updateProject(now, finalProjectId) }

        joinTasks(newParentTask, joinTasks, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(newParentTask.project)

        imageUuid?.let {
            Uploader.addUpload(newParentTask.taskKey, it, imagePath)
        }
    }

    fun createRootTask(
            now: ExactTimeStamp,
            dataId: Int,
            source: SaveService.Source,
            name: String,
            note: String?,
            projectId: String?,
            imagePath: String?): Task {
        check(name.isNotEmpty())

        val finalProjectId = projectId.takeUnless { it.isNullOrEmpty() } ?: defaultProjectId

        val imageUuid = imagePath?.let { newUuid() }

        val task = remoteProjectFactory.createRemoteTaskHelper(now, name, note, finalProjectId, imageUuid)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.project)

        imageUuid?.let {
            Uploader.addUpload(task.taskKey, it, imagePath)
        }

        return task
    }

    @Synchronized
    fun createRootTask(
            dataId: Int,
            source: SaveService.Source,
            name: String,
            note: String?,
            projectId: String?,
            imagePath: String?) {
        MyCrashlytics.log("DomainFactory.createRootTask")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        createRootTask(now, dataId, source, name, note, projectId, imagePath)
    }

    @Synchronized
    fun createJoinRootTask(
            dataId: Int,
            source: SaveService.Source,
            name: String,
            joinTaskKeys: List<TaskKey>,
            note: String?,
            projectId: String?,
            imagePath: String?) {
        MyCrashlytics.log("DomainFactory.createJoinRootTask")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(name.isNotEmpty())
        check(joinTaskKeys.size > 1)

        val now = ExactTimeStamp.now

        val finalProjectId = projectId.takeUnless { it.isNullOrEmpty() }
                ?: joinTaskKeys.map { it.remoteProjectId }
                        .distinct()
                        .single()

        var joinTasks = joinTaskKeys.map { getTaskForce(it) }

        val imageUuid = imagePath?.let { newUuid() }

        val newParentTask = remoteProjectFactory.createRemoteTaskHelper(now, name, note, finalProjectId, imageUuid)

        joinTasks = joinTasks.map { it.updateProject(now, finalProjectId) }

        joinTasks(newParentTask, joinTasks, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(newParentTask.project)

        imageUuid?.let {
            Uploader.addUpload(newParentTask.taskKey, it, imagePath)
        }
    }

    @Synchronized
    fun updateRootTask(dataId: Int, source: SaveService.Source, taskKey: TaskKey, name: String, note: String?, projectId: String?): TaskKey {
        MyCrashlytics.log("DomainFactory.updateRootTask")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(name.isNotEmpty())

        val now = ExactTimeStamp.now

        var task = getTaskForce(taskKey)
        check(task.current(now))

        val finalProjectId = projectId.takeUnless { it.isNullOrEmpty() } ?: defaultProjectId

        task = task.updateProject(now, finalProjectId)

        task.setName(name, note)

        getParentTaskHierarchy(task, now)?.setEndExactTimeStamp(now)

        task.getCurrentSchedules(now).forEach { it.setEndExactTimeStamp(now) }

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.project)

        return task.taskKey
    }

    fun createChildTask(
            now: ExactTimeStamp,
            dataId: Int,
            source: SaveService.Source,
            parentTaskKey: TaskKey,
            name: String,
            note: String?,
            imagePath: String?): Task {
        check(name.isNotEmpty())

        val parentTask = getTaskForce(parentTaskKey)
        check(parentTask.current(now))

        val imageUuid = imagePath?.let { newUuid() }

        val childTask = parentTask.createChildTask(now, name, note, imageUuid)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(childTask.project)

        imageUuid?.let {
            Uploader.addUpload(childTask.taskKey, it, imagePath)
        }

        return childTask
    }

    @Synchronized
    fun createChildTask(
            dataId: Int,
            source: SaveService.Source,
            parentTaskKey: TaskKey,
            name: String,
            note: String?,
            imagePath: String?) {
        MyCrashlytics.log("DomainFactory.createChildTask")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        createChildTask(now, dataId, source, parentTaskKey, name, note, imagePath)
    }

    @Synchronized
    fun createJoinChildTask(
            dataId: Int,
            source: SaveService.Source,
            parentTaskKey: TaskKey,
            name: String,
            joinTaskKeys: List<TaskKey>,
            note: String?,
            imagePath: String?) {
        MyCrashlytics.log("DomainFactory.createJoinChildTask")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(name.isNotEmpty())
        check(joinTaskKeys.size > 1)

        val now = ExactTimeStamp.now

        val parentTask = getTaskForce(parentTaskKey)
        check(parentTask.current(now))

        check(joinTaskKeys.map { it.remoteProjectId }.distinct().size == 1)

        val joinTasks = joinTaskKeys.map { getTaskForce(it) }

        val uuid = imagePath?.let { newUuid() }

        val childTask = parentTask.createChildTask(now, name, note, uuid)

        joinTasks(childTask, joinTasks, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(childTask.project)

        uuid?.let {
            Uploader.addUpload(childTask.taskKey, it, imagePath)
        }
    }

    @Synchronized
    fun updateChildTask(now: ExactTimeStamp, dataId: Int, source: SaveService.Source, taskKey: TaskKey, name: String, parentTaskKey: TaskKey, note: String?): TaskKey {
        MyCrashlytics.log("DomainFactory.updateChildTask")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

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

        notifyCloud(task.project)

        return task.taskKey
    }

    @Synchronized
    fun setTaskEndTimeStamp(dataId: Int, source: SaveService.Source, taskKey: TaskKey): TaskUndoData {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamp")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val task = getTaskForce(taskKey)
        check(task.current(now))

        val taskUndoData = TaskUndoData()

        task.setEndExactTimeStamp(now, taskUndoData)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.project)

        return taskUndoData
    }

    @Synchronized
    fun setInstanceOrdinal(dataId: Int, instanceKey: InstanceKey, ordinal: Double) {
        MyCrashlytics.log("DomainFactory.setInstanceOrdinal")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val instance = getInstance(instanceKey)

        instance.setOrdinal(ordinal, now)

        updateNotifications(now)

        save(dataId, SaveService.Source.GUI)

        notifyCloud(instance.project)
    }

    @Synchronized
    fun setTaskHierarchyOrdinal(dataId: Int, hierarchyData: HierarchyData) {
        MyCrashlytics.log("DomainFactory.setTaskHierarchyOrdinal")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val remoteProject: RemoteProject<*>?
        val taskHierarchy: TaskHierarchy

        val (projectId, taskHierarchyId) = hierarchyData.taskHierarchyKey as TaskHierarchyKey.RemoteTaskHierarchyKey

        remoteProject = remoteProjectFactory.getRemoteProjectForce(projectId)
        taskHierarchy = remoteProject.getTaskHierarchy(taskHierarchyId)

        check(taskHierarchy.current(now))

        taskHierarchy.ordinal = hierarchyData.ordinal

        updateNotifications(now)

        save(dataId, SaveService.Source.GUI)

        notifyCloud(remoteProject)
    }

    @Synchronized
    fun setTaskEndTimeStamps(dataId: Int, source: SaveService.Source, taskKeys: Set<TaskKey>): TaskUndoData {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(taskKeys.isNotEmpty())

        val now = ExactTimeStamp.now

        val tasks = taskKeys.map { getTaskForce(it) }.toMutableSet()
        check(tasks.all { it.current(now) })

        fun parentPresent(task: Task): Boolean = task.getParentTask(now)?.let {
            tasks.contains(it) || parentPresent(it)
        } ?: false

        tasks.toMutableSet().forEach {
            if (parentPresent(it))
                tasks.remove(it)
        }

        val taskUndoData = TaskUndoData()

        tasks.forEach { it.setEndExactTimeStamp(now, taskUndoData) }

        val remoteProjects = tasks.map { it.project }.toSet()

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)

        return taskUndoData
    }

    @Synchronized
    fun clearTaskEndTimeStamps(dataId: Int, source: SaveService.Source, taskUndoData: TaskUndoData) {
        MyCrashlytics.log("DomainFactory.clearTaskEndTimeStamps")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        processTaskUndoData(taskUndoData, now)

        updateNotifications(now)

        save(dataId, source)

        val remoteProjects = taskUndoData.taskKeys
                .map { getTaskForce(it).project }
                .toSet()

        notifyCloud(remoteProjects)
    }

    private fun processTaskUndoData(taskUndoData: TaskUndoData, now: ExactTimeStamp) {
        taskUndoData.taskKeys
                .map { getTaskForce(it) }
                .forEach {
                    check(!it.current(now))

                    it.clearEndExactTimeStamp(now)
                }

        val remoteTaskHierarchyKeys = taskUndoData.taskHierarchyKeys.filterIsInstance<TaskHierarchyKey.RemoteTaskHierarchyKey>()
        val remoteScheduleIds = taskUndoData.scheduleIds.filterIsInstance<ScheduleId.Remote>()

        remoteTaskHierarchyKeys.map { remoteProjectFactory.getTaskHierarchy(it) }.forEach {
            check(!it.current(now))

            it.clearEndExactTimeStamp(now)
        }

        remoteScheduleIds.map { remoteProjectFactory.getSchedule(it) }.forEach {
            check(!it.current(now))

            it.clearEndExactTimeStamp(now)
        }
    }

    @Synchronized
    fun createCustomTime(source: SaveService.Source, name: String, hourMinutes: Map<DayOfWeek, HourMinute>): CustomTimeKey.Private {
        MyCrashlytics.log("DomainFactory.createCustomTime")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(name.isNotEmpty())

        check(DayOfWeek.values().all { hourMinutes[it] != null })

        val customTimeJson = PrivateCustomTimeJson(
                name,
                hourMinutes.getValue(DayOfWeek.SUNDAY).hour,
                hourMinutes.getValue(DayOfWeek.SUNDAY).minute,
                hourMinutes.getValue(DayOfWeek.MONDAY).hour,
                hourMinutes.getValue(DayOfWeek.MONDAY).minute,
                hourMinutes.getValue(DayOfWeek.TUESDAY).hour,
                hourMinutes.getValue(DayOfWeek.TUESDAY).minute,
                hourMinutes.getValue(DayOfWeek.WEDNESDAY).hour,
                hourMinutes.getValue(DayOfWeek.WEDNESDAY).minute,
                hourMinutes.getValue(DayOfWeek.THURSDAY).hour,
                hourMinutes.getValue(DayOfWeek.THURSDAY).minute,
                hourMinutes.getValue(DayOfWeek.FRIDAY).hour,
                hourMinutes.getValue(DayOfWeek.FRIDAY).minute,
                hourMinutes.getValue(DayOfWeek.SATURDAY).hour,
                hourMinutes.getValue(DayOfWeek.SATURDAY).minute,
                true)

        val remoteCustomTime = remoteProjectFactory.remotePrivateProject.newRemoteCustomTime(customTimeJson)

        save(0, source)

        return remoteCustomTime.customTimeKey
    }

    @Synchronized
    fun updateCustomTime(dataId: Int, source: SaveService.Source, customTimeId: RemoteCustomTimeId.Private, name: String, hourMinutes: Map<DayOfWeek, HourMinute>) {
        MyCrashlytics.log("DomainFactory.updateCustomTime")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(name.isNotEmpty())

        val customTime = remoteProjectFactory.remotePrivateProject.getRemoteCustomTime(customTimeId)

        customTime.name = name

        for (dayOfWeek in DayOfWeek.values()) {
            val hourMinute = hourMinutes.getValue(dayOfWeek)

            if (hourMinute != customTime.getHourMinute(dayOfWeek))
                customTime.setHourMinute(dayOfWeek, hourMinute)
        }

        save(dataId, source)
    }

    @Synchronized
    fun setCustomTimesCurrent(dataId: Int, source: SaveService.Source, customTimeIds: List<RemoteCustomTimeId.Private>, current: Boolean) {
        MyCrashlytics.log("DomainFactory.setCustomTimesCurrent")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        check(customTimeIds.isNotEmpty())

        for (remoteCustomTimeId in customTimeIds) {
            val remotePrivateCustomTime = remoteProjectFactory.remotePrivateProject.getRemoteCustomTime(remoteCustomTimeId)
            remotePrivateCustomTime.current = current
        }

        save(dataId, source)
    }

    fun updateNotificationsTick(now: ExactTimeStamp, source: SaveService.Source, silent: Boolean, sourceName: String): Irrelevant {
        updateNotifications(silent, now, listOf(), sourceName)

        val irrelevant = setIrrelevant(now)

        remoteProjectFactory.let { localFactory.deleteInstanceShownRecords(it.taskKeys) }

        save(0, source)

        return irrelevant
    }

    @Synchronized
    fun updateNotificationsTick(source: SaveService.Source, silent: Boolean, sourceName: String) {
        MyCrashlytics.log("DomainFactory.updateNotificationsTick source: $sourceName")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        updateNotificationsTick(now, source, silent, sourceName)
    }

    @Synchronized
    fun removeFriends(keys: Set<String>) {
        MyCrashlytics.log("DomainFactory.removeFriends")
        check(!remoteFriendFactory.isSaved)

        keys.forEach { remoteFriendFactory.removeFriend(userInfo.key, it) }

        remoteFriendFactory.save()
    }

    @Synchronized
    fun updateToken(source: SaveService.Source, token: String?) {
        MyCrashlytics.log("DomainFactory.updateToken")
        if (remoteUserFactory.isSaved || remoteProjectFactory.isSharedSaved) throw SavedFactoryException()

        userInfo.token = token

        remoteUserFactory.remoteUser.setToken(token)
        remoteProjectFactory.updateToken(token)

        save(0, source)
    }

    @Synchronized
    fun updatePhotoUrl(source: SaveService.Source, photoUrl: String) {
        MyCrashlytics.log("DomainFactory.updatePhotoUrl")
        if (remoteUserFactory.isSaved || remoteProjectFactory.isSharedSaved) throw SavedFactoryException()

        remoteUserFactory.remoteUser.photoUrl = photoUrl
        remoteProjectFactory.updatePhotoUrl(userInfo, photoUrl)

        save(0, source)
    }

    @Synchronized
    fun updateProject(dataId: Int, source: SaveService.Source, projectId: String, name: String, addedFriends: Set<String>, removedFriends: Set<String>) {
        MyCrashlytics.log("DomainFactory.updateProject")

        check(projectId.isNotEmpty())
        check(name.isNotEmpty())

        val now = ExactTimeStamp.now

        val remoteProject = remoteProjectFactory.getRemoteProjectForce(projectId)

        remoteProject.name = name
        remoteProject.updateRecordOf(addedFriends.map { remoteFriendFactory.getFriend(it) }.toSet(), removedFriends)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProject, removedFriends)
    }

    @Synchronized
    fun createProject(dataId: Int, source: SaveService.Source, name: String, friends: Set<String>) {
        MyCrashlytics.log("DomainFactory.createProject")

        check(name.isNotEmpty())

        val now = ExactTimeStamp.now

        val recordOf = HashSet(friends)

        val key = userInfo.key
        check(!recordOf.contains(key))
        recordOf.add(key)

        val remoteProject = remoteProjectFactory.createRemoteProject(name, now, recordOf, remoteUserFactory.remoteUser)

        save(dataId, source)

        notifyCloud(remoteProject)
    }

    @Synchronized
    fun setProjectEndTimeStamps(dataId: Int, source: SaveService.Source, projectIds: Set<String>): ProjectUndoData {
        MyCrashlytics.log("DomainFactory.setProjectEndTimeStamps")

        check(projectIds.isNotEmpty())

        val now = ExactTimeStamp.now

        val remoteProjects = projectIds.map { remoteProjectFactory.getRemoteProjectForce(it) }.toSet()
        check(remoteProjects.all { it.current(now) })

        val projectUndoData = ProjectUndoData()

        remoteProjects.forEach { it.setEndExactTimeStamp(now, projectUndoData) }

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)

        return projectUndoData
    }

    @Synchronized
    fun clearProjectEndTimeStamps(dataId: Int, source: SaveService.Source, projectUndoData: ProjectUndoData) {
        MyCrashlytics.log("DomainFactory.clearProjectEndTimeStamps")

        val now = ExactTimeStamp.now

        val remoteProjects = projectUndoData.projectIds
                .map { remoteProjectFactory.getRemoteProjectForce(it) }
                .toSet()
        check(remoteProjects.none { it.current(now) })

        remoteProjects.forEach { it.clearEndExactTimeStamp(now) }

        processTaskUndoData(projectUndoData.taskUndoData, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)
    }

    @Synchronized
    fun setTaskImageUploaded(source: SaveService.Source, taskKey: TaskKey, imageUuid: String) {
        MyCrashlytics.log("DomainFactory.clearProjectEndTimeStamps")
        if (remoteProjectFactory.eitherSaved) throw SavedFactoryException()

        val task = getTaskForce(taskKey)
        check(task.image == ImageState.Local(imageUuid))

        task.image = ImageState.Remote(imageUuid)

        save(0, source)

        notifyCloud(task.project)
    }

    // internal

    private fun getExistingInstanceIfPresent(taskKey: TaskKey, scheduleDateTime: DateTime): Instance? {
        val customTimeKey = scheduleDateTime.time
                .timePair
                .customTimeKey

        val timePair = TimePair(customTimeKey, scheduleDateTime.time.timePair.hourMinute)

        val instanceKey = InstanceKey(taskKey, scheduleDateTime.date, timePair)

        return getExistingInstanceIfPresent(instanceKey)
    }

    private fun getExistingInstanceIfPresent(instanceKey: InstanceKey) = remoteProjectFactory.getExistingInstanceIfPresent(instanceKey)

    fun getSharedCustomTimes(privateCustomTimeId: RemoteCustomTimeId.Private) = remoteProjectFactory.remoteSharedProjects
            .values
            .mapNotNull { it.getSharedTimeIfPresent(privateCustomTimeId) }

    private fun generateInstance(taskKey: TaskKey, scheduleDateTime: DateTime): Instance {
        val (remoteCustomTimeId, hour, minute) = scheduleDateTime.time
                .timePair
                .destructureRemote()

        val instanceShownRecord = localFactory.getInstanceShownRecord(taskKey.remoteProjectId, taskKey.remoteTaskId, scheduleDateTime.date.year, scheduleDateTime.date.month, scheduleDateTime.date.day, remoteCustomTimeId, hour, minute)

        return remoteProjectFactory.getTaskForce(taskKey).generateInstance(scheduleDateTime, instanceShownRecord)
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

        return allInstances.values.filter { it.isRootInstance(now) && it.isVisible(now, true) }
    }

    private fun getTime(timePair: TimePair): Time {
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

    fun getCustomTime(customTimeKey: CustomTimeKey<*>) = remoteProjectFactory.getRemoteCustomTime(customTimeKey.remoteProjectId, customTimeKey.remoteCustomTimeId)

    private fun getCurrentRemoteCustomTimes() = remoteProjectFactory.remotePrivateProject
            .customTimes
            .filter { it.current }

    private fun getChildInstanceDatas(instance: Instance, now: ExactTimeStamp): MutableMap<InstanceKey, GroupListFragment.InstanceData> {
        return instance.getChildInstances(now)
                .map { (childInstance, taskHierarchy) ->
                    val childTask = childInstance.task

                    val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

                    val children = getChildInstanceDatas(childInstance, now)
                    val instanceData = GroupListFragment.InstanceData(childInstance.done, childInstance.instanceKey, null, childInstance.name, childInstance.instanceDateTime.timeStamp, childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), childInstance.instanceDateTime.time.timePair, childTask.note, children, HierarchyData(taskHierarchy.taskHierarchyKey, taskHierarchy.ordinal), childInstance.ordinal, childInstance.notificationShown)
                    children.values.forEach { it.instanceDataParent = instanceData }
                    childInstance.instanceKey to instanceData
                }
                .toMap()
                .toMutableMap()
    }

    private fun getTaskListChildTaskDatas(now: ExactTimeStamp, parentTask: Task, excludedTaskKeys: Set<TaskKey>): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> =
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

    private fun Task.showAsParent(now: ExactTimeStamp, excludedTaskKeys: Set<TaskKey>, includedTaskKeys: Set<TaskKey>): Boolean {
        check(excludedTaskKeys.intersect(includedTaskKeys).isEmpty())

        if (!current(now)) {
            check(!includedTaskKeys.contains(taskKey))

            return false
        }

        if (!isRootTask(now))
            return false

        if (includedTaskKeys.contains(taskKey)) {
            check(isVisible(now, true))

            return true
        }

        if (excludedTaskKeys.contains(taskKey))
            return false

        if (!isVisible(now, false))
            return false

        return true
    }

    private fun getParentTreeDatas(now: ExactTimeStamp, excludedTaskKeys: Set<TaskKey>, includedTaskKeys: Set<TaskKey>): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> {
        val parentTreeDatas = mutableMapOf<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>()

        parentTreeDatas.putAll(remoteProjectFactory.remotePrivateProject
                .tasks
                .filter { it.showAsParent(now, excludedTaskKeys, includedTaskKeys) }
                .map {
                    val taskParentKey = CreateTaskViewModel.ParentKey.TaskParentKey(it.taskKey)
                    val parentTreeData = CreateTaskViewModel.ParentTreeData(it.name, getTaskListChildTaskDatas(now, it, excludedTaskKeys), taskParentKey, it.getScheduleText(now), it.note, CreateTaskViewModel.SortKey.TaskSortKey(it.startExactTimeStamp))

                    taskParentKey to parentTreeData
                }
                .toMap())

        parentTreeDatas.putAll(remoteProjectFactory.remoteSharedProjects
                .values
                .filter { it.current(now) }
                .map {
                    val projectParentKey = CreateTaskViewModel.ParentKey.ProjectParentKey(it.id)

                    val users = it.users.joinToString(", ") { it.name }
                    val parentTreeData = CreateTaskViewModel.ParentTreeData(it.name, getProjectTaskTreeDatas(now, it, excludedTaskKeys, includedTaskKeys), projectParentKey, users, null, CreateTaskViewModel.SortKey.ProjectSortKey(it.id))

                    projectParentKey to parentTreeData
                }
                .toMap())

        return parentTreeDatas
    }

    private fun getProjectTaskTreeDatas(now: ExactTimeStamp, remoteProject: RemoteProject<*>, excludedTaskKeys: Set<TaskKey>, includedTaskKeys: Set<TaskKey>): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> {
        return remoteProject.tasks
                .filter { it.showAsParent(now, excludedTaskKeys, includedTaskKeys) }
                .map {
                    val taskParentKey = CreateTaskViewModel.ParentKey.TaskParentKey(it.taskKey)
                    val parentTreeData = CreateTaskViewModel.ParentTreeData(it.name, getTaskListChildTaskDatas(now, it, excludedTaskKeys), taskParentKey, it.getScheduleText(now), it.note, CreateTaskViewModel.SortKey.TaskSortKey(it.startExactTimeStamp))

                    taskParentKey to parentTreeData
                }
                .toMap()
    }

    fun <T : RemoteCustomTimeId> convertRemoteToRemote(now: ExactTimeStamp, startingRemoteTask: RemoteTask<T>, projectId: String): RemoteTask<*> {
        check(projectId.isNotEmpty())

        val remoteToRemoteConversion = RemoteToRemoteConversion<T>()
        val startProject = startingRemoteTask.remoteProject
        startProject.convertRemoteToRemoteHelper(remoteToRemoteConversion, startingRemoteTask)

        updateNotifications(true, now, remoteToRemoteConversion.startTasks
                .values
                .map { it.first.taskKey }, "other")

        val remoteProject = remoteProjectFactory.getRemoteProjectForce(projectId)

        for (pair in remoteToRemoteConversion.startTasks.values) {
            val remoteTask = remoteProject.copyTask(pair.first, pair.second, now)
            remoteToRemoteConversion.endTasks[pair.first.id] = remoteTask
        }

        for (startTaskHierarchy in remoteToRemoteConversion.startTaskHierarchies) {
            val parentRemoteTask = remoteToRemoteConversion.endTasks[startTaskHierarchy.parentTaskId]!!
            val childRemoteTask = remoteToRemoteConversion.endTasks[startTaskHierarchy.childTaskId]!!

            val remoteTaskHierarchy = remoteProject.copyRemoteTaskHierarchy(startTaskHierarchy, parentRemoteTask.id, childRemoteTask.id)

            remoteToRemoteConversion.endTaskHierarchies.add(remoteTaskHierarchy)
        }

        for (pair in remoteToRemoteConversion.startTasks.values) {
            pair.second.forEach { it.delete() }

            pair.first.delete()
        }

        return remoteToRemoteConversion.endTasks[startingRemoteTask.id]!!
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

    private fun getTasks() = remoteProjectFactory.tasks.asSequence()

    private val customTimes get() = remoteProjectFactory.remoteCustomTimes

    fun getTaskForce(taskKey: TaskKey) = remoteProjectFactory.getTaskForce(taskKey)

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

    private fun getExistingInstances() = remoteProjectFactory.existingInstances

    private fun getGroupListChildTaskDatas(parentTask: Task, now: ExactTimeStamp): List<GroupListFragment.TaskData> = parentTask.getChildTaskHierarchies(now)
            .map {
                val childTask = it.childTask

                GroupListFragment.TaskData(childTask.taskKey, childTask.name, getGroupListChildTaskDatas(childTask, now), childTask.startExactTimeStamp, childTask.note)
            }

    fun getMainData(now: ExactTimeStamp): TaskListFragment.TaskData {
        val childTaskDatas = getTasks().filter { it.current(now) && it.isVisible(now, false) && it.isRootTask(now) }
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

        notifyCloud(instance.project)

        return instance
    }

    fun setIrrelevant(now: ExactTimeStamp): Irrelevant {
        if (SnackbarListener.deleting)
            return Irrelevant(listOf(), listOf(), listOf(), listOf())

        val tasks = getTasks()

        for (task in tasks)
            task.updateOldestVisible(now)

        // relevant hack
        val taskRelevances = tasks.map { it.taskKey to TaskRelevance(this, it) }.toMap()

        val taskHierarchies = remoteProjectFactory.remoteProjects
                .map { it.value.taskHierarchies }
                .flatten()
        val taskHierarchyRelevances = taskHierarchies.associate { it.taskHierarchyKey to TaskHierarchyRelevance(this, it) }

        val existingInstances = getExistingInstances()
        val rootInstances = getRootInstances(null, now.plusOne(), now)

        val instanceRelevances = (existingInstances + rootInstances)
                .asSequence()
                .distinct()
                .map { it.instanceKey to InstanceRelevance(it) }
                .toList()
                .toMap()
                .toMutableMap()

        tasks.asSequence()
                .filter { it.current(now) && it.isRootTask(now) && it.isVisible(now, true) }
                .map { taskRelevances.getValue(it.taskKey) }.toList()
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        rootInstances.map { instanceRelevances[it.instanceKey]!! }.forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        existingInstances.asSequence()
                .filter { it.isRootInstance(now) && it.isVisible(now, true) }
                .map { instanceRelevances[it.instanceKey]!! }.toList()
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        val relevantTaskRelevances = taskRelevances.values.filter { it.relevant }
        val relevantTasks = relevantTaskRelevances.map { it.task }

        val irrelevantTasks = tasks.toMutableList()
        irrelevantTasks.removeAll(relevantTasks)

        check(irrelevantTasks.none { it.isVisible(now, true) })

        val relevantTaskHierarchyRelevances = taskHierarchyRelevances.values.filter { it.relevant }
        val relevantTaskHierarchies = relevantTaskHierarchyRelevances.map { it.taskHierarchy }

        val irrelevantTaskHierarchies = taskHierarchies.toMutableList().apply { removeAll(relevantTaskHierarchies) }

        val relevantInstances = instanceRelevances.values
                .filter { it.relevant }
                .map { it.instance }

        val relevantExistingInstances = relevantInstances.filter { it.exists() }

        val irrelevantExistingInstances = ArrayList<Instance>(existingInstances)
        irrelevantExistingInstances.removeAll(relevantExistingInstances)

        check(irrelevantExistingInstances.none { it.isVisible(now, true) })

        irrelevantExistingInstances.forEach { it.delete() }
        irrelevantTasks.forEach { it.delete() }
        irrelevantTaskHierarchies.forEach { it.delete() }

        val remoteCustomTimes = remoteProjectFactory.remoteCustomTimes
        val remoteCustomTimeRelevances = remoteCustomTimes.map { Pair(it.projectId, it.id) to RemoteCustomTimeRelevance(it) }.toMap()

        remoteProjectFactory.remotePrivateProject
                .customTimes
                .filter { it.current }
                .forEach { remoteCustomTimeRelevances.getValue(Pair(it.projectId, it.id)).setRelevant() }

        val remoteProjects = remoteProjectFactory.remoteProjects.values
        val remoteProjectRelevances = remoteProjects.map { it.id to RemoteProjectRelevance(it) }.toMap()

        remoteProjects.filter { it.current(now) }
                .map { remoteProjectRelevances.getValue(it.id) }
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

        val irrelevantRemoteCustomTimes = ArrayList(remoteCustomTimes)
        irrelevantRemoteCustomTimes.removeAll(relevantRemoteCustomTimes)
        irrelevantRemoteCustomTimes.forEach { it.delete() }

        val relevantRemoteProjects = remoteProjectRelevances.values
                .filter { it.relevant }
                .map { it.remoteProject }

        val irrelevantRemoteProjects = ArrayList(remoteProjects)
        irrelevantRemoteProjects.removeAll(relevantRemoteProjects)
        irrelevantRemoteProjects.forEach { it.delete() }

        val irrelevantInstanceShownRecords = localFactory.instanceShownRecords
                .toMutableList()
                .apply { removeAll(relevantInstances.map { it.instanceShownRecord }) }
        irrelevantInstanceShownRecords.forEach { it.delete() }

        return Irrelevant(irrelevantTasks, irrelevantExistingInstances, irrelevantRemoteCustomTimes, irrelevantRemoteProjects)
    }

    private fun notifyCloud(remoteProject: RemoteProject<*>) = notifyCloud(setOf(remoteProject))

    private fun notifyCloud(remoteProjects: Set<RemoteProject<*>>) {
        if (remoteProjects.isNotEmpty())
            notifyCloudPrivateFixed(remoteProjects.toMutableSet(), userInfo, mutableListOf())
    }

    private fun notifyCloud(remoteProject: RemoteProject<*>, userKeys: Collection<String>) = notifyCloudPrivateFixed(mutableSetOf(remoteProject), userInfo, userKeys.toMutableList())

    private fun notifyCloudPrivateFixed(remoteProjects: MutableSet<RemoteProject<*>>, userInfo: UserInfo, userKeys: MutableCollection<String>) {
        val remotePrivateProject = remoteProjects.singleOrNull { it is RemotePrivateProject }

        remotePrivateProject?.let {
            remoteProjects.remove(it)

            userKeys.add(userInfo.key)
        }

        BackendNotifier.notify(remoteProjects, userInfo, userKeys)
    }

    private fun updateNotifications(now: ExactTimeStamp, clear: Boolean = false) = updateNotifications(true, now, mutableListOf(), "other", clear)

    private fun updateNotifications(silent: Boolean, now: ExactTimeStamp, removedTaskKeys: List<TaskKey>, sourceName: String, clear: Boolean = false) {
        Preferences.logLineDate("updateNotifications start $sourceName")

        val notificationInstances = if (clear) mapOf() else getRootInstances(null, now.plusOne(), now /* 24 hack */).filter { it.done == null && !it.notified && it.instanceDateTime.timeStamp.toExactTimeStamp() <= now && !removedTaskKeys.contains(it.taskKey) }.associateBy { it.instanceKey }

        val shownInstanceKeys = getExistingInstances().filter { it.notificationShown }
                .map { it.instanceKey }
                .toMutableSet()

        val instanceShownPairs = localFactory.instanceShownRecords
                .filter { it.notificationShown }
                .map { Pair(it, remoteProjectFactory.getRemoteProjectIfPresent(it.projectId)?.getRemoteTaskIfPresent(it.taskId)) }

        instanceShownPairs.filter { it.second == null }.forEach { (instanceShownRecord, _) ->
            val scheduleDate = Date(instanceShownRecord.scheduleYear, instanceShownRecord.scheduleMonth, instanceShownRecord.scheduleDay)
            val remoteCustomTimeId = instanceShownRecord.scheduleCustomTimeId

            val customTimePair: Pair<String, String>?
            val hourMinute: HourMinute?
            if (!remoteCustomTimeId.isNullOrEmpty()) {
                check(instanceShownRecord.scheduleHour == null)
                check(instanceShownRecord.scheduleMinute == null)

                customTimePair = Pair(instanceShownRecord.projectId, remoteCustomTimeId)
                hourMinute = null
            } else {
                checkNotNull(instanceShownRecord.scheduleHour)
                checkNotNull(instanceShownRecord.scheduleMinute)

                customTimePair = null
                hourMinute = HourMinute(instanceShownRecord.scheduleHour, instanceShownRecord.scheduleMinute)
            }

            val taskKey = TaskKey(instanceShownRecord.projectId, instanceShownRecord.taskId)

            NotificationWrapper.instance.cancelNotification(Instance.getNotificationId(scheduleDate, customTimePair, hourMinute, taskKey))
            instanceShownRecord.notificationShown = false
        }

        instanceShownPairs.filter { it.second != null }.forEach { (instanceShownRecord, task) ->
            val scheduleDate = Date(instanceShownRecord.scheduleYear, instanceShownRecord.scheduleMonth, instanceShownRecord.scheduleDay)
            val remoteCustomTimeId = instanceShownRecord.scheduleCustomTimeId

            val customTimeKey: CustomTimeKey<*>?
            val hourMinute: HourMinute?
            if (!remoteCustomTimeId.isNullOrEmpty()) {
                check(instanceShownRecord.scheduleHour == null)
                check(instanceShownRecord.scheduleMinute == null)

                val project = task!!.project

                customTimeKey = getCustomTimeKey(instanceShownRecord.projectId, project.getRemoteCustomTimeId(remoteCustomTimeId))
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
        }

        val showInstanceKeys = notificationInstances.keys.filter { !shownInstanceKeys.contains(it) }

        val hideInstanceKeys = shownInstanceKeys.filter { !notificationInstances.containsKey(it) }.toSet()

        for (showInstanceKey in showInstanceKeys)
            getInstance(showInstanceKey).notificationShown = true

        for (hideInstanceKey in hideInstanceKeys)
            getInstance(hideInstanceKey).notificationShown = false

        Preferences.logLineHour("silent? $silent")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (notificationInstances.size > TickJobIntentService.MAX_NOTIFICATIONS) { // show group
                if (shownInstanceKeys.size > TickJobIntentService.MAX_NOTIFICATIONS) { // group shown
                    val silentParam = if (showInstanceKeys.isNotEmpty() || hideInstanceKeys.isNotEmpty()) silent else true

                    NotificationWrapper.instance.notifyGroup(notificationInstances.values, silentParam, now)
                } else { // instances shown
                    for (shownInstanceKey in shownInstanceKeys)
                        NotificationWrapper.instance.cancelNotification(getInstance(shownInstanceKey).notificationId)

                    NotificationWrapper.instance.notifyGroup(notificationInstances.values, silent, now)
                }
            } else { // show instances
                if (shownInstanceKeys.size > TickJobIntentService.MAX_NOTIFICATIONS) { // group shown
                    NotificationWrapper.instance.cancelNotification(0)

                    for (instance in notificationInstances.values)
                        notifyInstance(instance, silent, now)
                } else { // instances shown
                    for (hideInstanceKey in hideInstanceKeys)
                        NotificationWrapper.instance.cancelNotification(getInstance(hideInstanceKey).notificationId)

                    for (showInstanceKey in showInstanceKeys)
                        notifyInstance(notificationInstances.getValue(showInstanceKey), silent, now)

                    notificationInstances.values
                            .filter { !showInstanceKeys.contains(it.instanceKey) }
                            .forEach { updateInstance(it, now) }
                }
            }
        } else {
            if (notificationInstances.isEmpty()) {
                Preferences.logLineHour("hiding group")
                NotificationWrapper.instance.cancelNotification(0)
            } else {
                Preferences.logLineHour("showing group")
                NotificationWrapper.instance.notifyGroup(notificationInstances.values, true, now)
            }

            for (hideInstanceKey in hideInstanceKeys) {
                val instance = getInstance(hideInstanceKey)
                Preferences.logLineHour("hiding '" + instance.name + "'")
                NotificationWrapper.instance.cancelNotification(instance.notificationId)
            }

            for (showInstanceKey in showInstanceKeys) {
                val instance = notificationInstances.getValue(showInstanceKey)
                Preferences.logLineHour("showing '" + instance.name + "'")
                notifyInstance(instance, silent, now)
            }

            val updateInstances = notificationInstances.values.filter { !showInstanceKeys.contains(it.instanceKey) }

            updateInstances.forEach {
                Preferences.logLineHour("updating '" + it.name + "' " + it.instanceDateTime)
                updateInstance(it, now)
            }
        }

        if (!silent)
            Preferences.lastTick = now.long

        var nextAlarm = getExistingInstances().map { it.instanceDateTime.timeStamp }
                .filter { it.toExactTimeStamp() > now }
                .min()
                .takeUnless { clear }

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
    }

    private fun notifyInstance(instance: Instance, silent: Boolean, now: ExactTimeStamp) = NotificationWrapper.instance.notifyInstance(instance, silent, now)

    private fun updateInstance(instance: Instance, now: ExactTimeStamp) = NotificationWrapper.instance.notifyInstance(instance, true, now)

    private fun setInstanceNotified(instanceKey: InstanceKey) {
        getInstance(instanceKey).apply {
            notified = true
            notificationShown = false
        }
    }

    private fun getGroupListData(timeStamp: TimeStamp, now: ExactTimeStamp): GroupListFragment.DataWrapper {
        val endCalendar = timeStamp.calendar.apply { add(Calendar.MINUTE, 1) }
        val endTimeStamp = TimeStamp(endCalendar)

        val rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now)

        val currentInstances = rootInstances.filter { it.instanceDateTime.timeStamp.compareTo(timeStamp) == 0 }

        val customTimeDatas = getCurrentRemoteCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val instanceDatas = HashMap<InstanceKey, GroupListFragment.InstanceData>()
        for (instance in currentInstances) {
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)
            val instanceData = GroupListFragment.InstanceData(instance.done, instance.instanceKey, null, instance.name, instance.instanceDateTime.timeStamp, task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.instanceDateTime.time.timePair, task.note, children, null, instance.ordinal, instance.notificationShown)
            children.values.forEach { it.instanceDataParent = instanceData }
            instanceDatas[instance.instanceKey] = instanceData
        }

        val dataWrapper = GroupListFragment.DataWrapper(customTimeDatas, null, listOf(), null, instanceDatas, null)

        instanceDatas.values.forEach { it.instanceDataParent = dataWrapper }

        return dataWrapper
    }

    private fun getGroupListData(
            instance: Instance,
            task: Task,
            now: ExactTimeStamp,
            showImage: Boolean): GroupListFragment.DataWrapper {
        val customTimeDatas = getCurrentRemoteCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val instanceDatas = instance.getChildInstances(now)
                .map { (childInstance, taskHierarchy) ->
                    val childTask = childInstance.task

                    val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

                    val children = getChildInstanceDatas(childInstance, now)
                    val instanceData = GroupListFragment.InstanceData(childInstance.done, childInstance.instanceKey, null, childInstance.name, childInstance.instanceDateTime.timeStamp, childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), childInstance.instanceDateTime.time.timePair, childTask.note, children, HierarchyData(taskHierarchy.taskHierarchyKey, taskHierarchy.ordinal), childInstance.ordinal, childInstance.notificationShown)
                    children.values.forEach { it.instanceDataParent = instanceData }
                    childInstance.instanceKey to instanceData
                }
                .toMap()
                .toMutableMap()

        val imageState = if (showImage) task.image else null

        val dataWrapper = GroupListFragment.DataWrapper(
                customTimeDatas,
                task.current(now),
                listOf(),
                task.note,
                instanceDatas,
                imageState)

        instanceDatas.values.forEach { it.instanceDataParent = dataWrapper }

        return dataWrapper
    }

    fun getCustomTimeKey(remoteProjectId: String, remoteCustomTimeId: RemoteCustomTimeId): CustomTimeKey<*> {
        return remoteProjectFactory.getRemoteCustomTime(remoteProjectId, remoteCustomTimeId).customTimeKey
    }

    class ProjectUndoData {

        val projectIds = mutableSetOf<String>()
        val taskUndoData = TaskUndoData()
    }

    class TaskUndoData {

        val taskKeys = mutableSetOf<TaskKey>()
        val scheduleIds = mutableSetOf<ScheduleId>()
        val taskHierarchyKeys = mutableSetOf<TaskHierarchyKey>()
    }

    class HourUndoData(val instanceDateTimes: Map<InstanceKey, DateTime>)

    class ReadTimes(start: ExactTimeStamp, read: ExactTimeStamp, stop: ExactTimeStamp) {

        val readMillis = read.long - start.long
        val instantiateMillis = stop.long - read.long
    }

    private inner class SavedFactoryException : Exception("private.isSaved == " + remoteProjectFactory.isPrivateSaved + ", shared.isSaved == " + remoteProjectFactory.isSharedSaved + ", user.isSaved == " + remoteUserFactory.isSaved)
}