package com.krystianwsul.checkme.domainmodel

import android.net.Uri
import android.os.Build
import androidx.core.content.pm.ShortcutManagerCompat
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.firebase.*
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.gui.HierarchyData
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.checkme.utils.newUuid
import com.krystianwsul.checkme.utils.prettyPrint
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.utils.time.toDateTimeSoy
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.checkme.viewmodels.*
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ProjectUndoData
import com.krystianwsul.common.domain.RemoteToRemoteConversion
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.domain.schedules.ScheduleGroup
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
import io.reactivex.Observable
import java.util.*

@Suppress("LeakingThis")
class DomainFactory(
        private val localFactory: LocalFactory,
        private val remoteUserFactory: RemoteUserFactory,
        val projectsFactory: ProjectsFactory,
        val remoteFriendFactory: RemoteFriendFactory,
        _deviceDbInfo: DeviceDbInfo,
        startTime: ExactTimeStamp,
        readTime: ExactTimeStamp
) : PrivateCustomTime.AllRecordsSource, Task.ProjectUpdater, FactoryProvider.Domain {

    companion object {

        val instanceRelay = BehaviorRelay.createDefault(NullableWrapper<DomainFactory>())

        val nullableInstance get() = instanceRelay.value!!.value

        val instance get() = nullableInstance!!

        private val firebaseListeners = mutableListOf<Pair<(DomainFactory) -> Unit, String>>()

        var firstRun = false

        val isSaved = instanceRelay.switchMap { it.value?.isSaved ?: Observable.just(false) }
                .distinctUntilChanged()
                .replay(1)!!
                .apply { connect() }

        @Synchronized // still running?
        fun setFirebaseTickListener(source: SaveService.Source, newTickData: TickData) {
            check(MyApplication.instance.hasUserInfo)

            val domainFactory = nullableInstance

            if (domainFactory?.projectsFactory?.isSaved != false) {
                TickHolder.addTickData(newTickData)
            } else {
                val tickData = TickHolder.getTickData()

                val silent = (tickData?.silent ?: true) && newTickData.silent

                domainFactory.updateNotificationsTick(source, silent, newTickData.source)

                if (tickData?.waiting == true) {
                    TickHolder.addTickData(newTickData)
                } else {
                    tickData?.release()
                    newTickData.release()
                }
            }
        }

        @Synchronized
        fun addFirebaseListener(firebaseListener: (DomainFactory) -> Unit) { // todo route all external calls through here
            val domainFactory = nullableInstance
            if (domainFactory?.projectsFactory?.isSaved == false && !domainFactory.remoteFriendFactory.isSaved) {
                domainFactory.checkSave()
                firebaseListener(domainFactory)
            } else {
                firebaseListeners.add(Pair(firebaseListener, "other"))
            }
        }

        @Synchronized
        fun addFirebaseListener(source: String, firebaseListener: (DomainFactory) -> Unit) {
            val domainFactory = nullableInstance
            if (domainFactory?.projectsFactory?.isSaved == false && !domainFactory.remoteFriendFactory.isSaved) {
                Preferences.tickLog.logLineHour("running firebaseListener $source")
                firebaseListener(domainFactory)
            } else {
                Preferences.tickLog.logLineHour("queuing firebaseListener $source")
                Preferences.tickLog.logLineHour("listeners before: " + firebaseListeners.joinToString("; ") { it.second })
                firebaseListeners.add(Pair(firebaseListener, source))
                Preferences.tickLog.logLineHour("listeners after: " + firebaseListeners.joinToString("; ") { it.second })
            }
        }
    }

    var remoteReadTimes: ReadTimes
        private set

    private var aggregateData: AggregateData? = null

    val domainChanged = BehaviorRelay.createDefault(setOf<Int>())

    var deviceDbInfo = _deviceDbInfo
        private set

    val isSaved = BehaviorRelay.createDefault(false)

    init {
        Preferences.tickLog.logLineHour("DomainFactory.init")

        val now = ExactTimeStamp.now

        remoteReadTimes = ReadTimes(startTime, readTime, now)

        tryNotifyListeners(now, "DomainFactory.init", if (firstRun) RunType.APP_START else RunType.SIGN_IN)

        firstRun = false

        updateShortcuts(now)
    }

    private val defaultProjectId by lazy { projectsFactory.privateProject.projectKey }

    // misc

    val taskCount get() = projectsFactory.taskCount
    val instanceCount get() = projectsFactory.instanceCount

    val customTimeCount get() = customTimes.size

    val instanceShownCount get() = localFactory.instanceShownRecords.size

    val uuid get() = localFactory.uuid

    private fun updateIsSaved() = isSaved.accept(projectsFactory.isSaved || remoteUserFactory.isSaved || remoteFriendFactory.isSaved)

    fun save(
            dataId: Int,
            source: SaveService.Source,
            forceDomainChanged: Boolean = false,
            values: MutableMap<String, Any?> = mutableMapOf()
    ) = save(setOf(dataId), source, forceDomainChanged, values)

    fun save(
            dataIds: Set<Int>,
            source: SaveService.Source,
            forceDomainChanged: Boolean = false,
            values: MutableMap<String, Any?> = mutableMapOf()
    ) {
        val skipping = aggregateData != null
        Preferences.tickLog.logLineHour("DomainFactory.save: skipping? $skipping")

        if (skipping) {
            check(dataIds.single() == 0)

            return
        }

        val localChanges = localFactory.save(source)
        projectsFactory.save(values)
        remoteUserFactory.save(values)
        remoteFriendFactory.save(values)

        val changes = localChanges || values.isNotEmpty()

        if (values.isNotEmpty())
            AndroidDatabaseWrapper.update(values, checkError(this, "DomainFactory.save", values))

        if (changes || forceDomainChanged)
            domainChanged.accept(dataIds)

        if (changes)
            updateIsSaved()
    }

    private fun updateShortcuts(now: ExactTimeStamp) {
        val shortcutTasks = ShortcutManager.getShortcuts()
                .map { Pair(it.value, getTaskIfPresent(it.key)) }
                .filter { it.second?.isVisible(now, false) == true }
                .map { Pair(it.first, it.second!!) }

        ShortcutManager.keepShortcuts(shortcutTasks.map { it.second.taskKey })

        val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(MyApplication.instance) - 4
        if (maxShortcuts <= 0)
            return

        val shortcutDatas = shortcutTasks.sortedBy { it.first }
                .takeLast(maxShortcuts)
                .map { ShortcutQueue.ShortcutData(deviceDbInfo, it.second) }

        ShortcutQueue.updateShortcuts(shortcutDatas)
    }

    // firebase

    @Synchronized
    override fun clearUserInfo() = updateNotifications(ExactTimeStamp.now, true)

    @Synchronized
    override fun onProjectsInstancesChange(changeType: ChangeType, now: ExactTimeStamp) {
        MyCrashlytics.log("DomainFactory.onChange")

        val runType = when (changeType) {
            ChangeType.LOCAL -> RunType.LOCAL
            ChangeType.REMOTE -> RunType.REMOTE
        }

        updateShortcuts(now)

        tryNotifyListeners(now, "DomainFactory.onChange", runType)
    }

    @Synchronized
    override fun updateUserRecord(dataSnapshot: Snapshot) {
        MyCrashlytics.log("DomainFactory.updateUserRecord")

        val runType = if (remoteUserFactory.isSaved) {
            remoteUserFactory.isSaved = false

            RunType.LOCAL
        } else {
            remoteUserFactory.onNewSnapshot(dataSnapshot)

            RunType.REMOTE
        }

        tryNotifyListeners(ExactTimeStamp.now, "DomainFactory.updateUserRecord", runType)
    }

    @Synchronized
    override fun updateFriendRecords(databaseEvent: DatabaseEvent) {
        MyCrashlytics.log("updateFriendRecords")

        val localChange = remoteFriendFactory.onDatabaseEvent(databaseEvent)

        val runType = if (localChange) RunType.LOCAL else RunType.REMOTE

        tryNotifyListeners(ExactTimeStamp.now, "DomainFactory.setFriendRecords", runType)
    }

    private fun tryNotifyListeners(now: ExactTimeStamp, source: String, runType: RunType) {
        MyCrashlytics.log("DomainFactory.tryNotifyListeners $source $runType")

        if (projectsFactory.isSaved || remoteFriendFactory.isSaved || remoteUserFactory.isSaved)
            return

        updateIsSaved()

        check(aggregateData == null)

        aggregateData = AggregateData()

        Preferences.tickLog.logLineHour("DomainFactory: notifiying ${firebaseListeners.size} listeners")
        firebaseListeners.forEach { it.first(this) }
        firebaseListeners.clear()

        val copyAggregateData = aggregateData!!
        aggregateData = null

        val tickData = TickHolder.getTickData()

        fun tick(tickData: TickData, forceNotify: Boolean) {
            updateNotificationsTick(now, tickData.silent && !forceNotify, tickData.source)

            if (!tickData.waiting)
                tickData.release()
        }

        fun notify() {
            check(tickData == null)

            updateNotifications(now, silent = false, sourceName = source)
        }

        when (runType) {
            RunType.APP_START, RunType.LOCAL -> tickData?.let { tick(it, false) }
            RunType.SIGN_IN -> tickData?.let { tick(it, false) } ?: notify()
            RunType.REMOTE -> tickData?.let { tick(it, true) } ?: notify()
        }

        save(0, SaveService.Source.GUI, runType == RunType.REMOTE)

        copyAggregateData.run {
            if (listOf(notificationProjects, notificationUserKeys).any { it.isNotEmpty() })
                notifyCloudPrivateFixed(notificationProjects, notificationUserKeys)
        }
    }

    private enum class RunType {

        APP_START, SIGN_IN, LOCAL, REMOTE
    }

    // gets

    @Synchronized
    fun getEditInstanceData(instanceKey: InstanceKey): EditInstanceViewModel.Data {
        MyCrashlytics.log("DomainFactory.getEditInstanceData")

        val now = ExactTimeStamp.now

        val currentCustomTimes = getCurrentRemoteCustomTimes(now).associateBy {
            it.key
        }.toMutableMap<CustomTimeKey<*>, Time.Custom<*>>()

        val instance = getInstance(instanceKey)
        check(instance.isRootInstance(now))

        (instance.instanceTime as? Time.Custom<*>)?.let {
            currentCustomTimes[it.key] = it
        }

        val customTimeDatas = currentCustomTimes.mapValues {
            it.value.let { EditInstanceViewModel.CustomTimeData(it.key, it.name, it.hourMinutes.toSortedMap()) }
        }

        return EditInstanceViewModel.Data(instance.instanceKey, instance.instanceDate, instance.instanceTimePair, instance.name, customTimeDatas, instance.done != null, instance.instanceDateTime.timeStamp.toExactTimeStamp() <= now)
    }

    @Synchronized
    fun getEditInstancesData(instanceKeys: List<InstanceKey>): EditInstancesViewModel.Data {
        MyCrashlytics.log("DomainFactory.getEditInstancesData")

        check(instanceKeys.isNotEmpty())

        val now = ExactTimeStamp.now

        val currentCustomTimes = getCurrentRemoteCustomTimes(now).associateBy {
            it.key
        }.toMutableMap<CustomTimeKey<*>, Time.Custom<*>>()

        val instanceDatas = mutableMapOf<InstanceKey, EditInstancesViewModel.InstanceData>()

        for (instanceKey in instanceKeys) {
            val instance = getInstance(instanceKey)
            check(instance.isRootInstance(now))
            check(instance.done == null)

            instanceDatas[instanceKey] = EditInstancesViewModel.InstanceData(instance.instanceDateTime, instance.name, instance.done != null)

            (instance.instanceTime as? Time.Custom<*>)?.let {
                currentCustomTimes[it.key] = it
            }
        }

        val customTimeDatas = currentCustomTimes.mapValues {
            it.value.let {
                EditInstancesViewModel.CustomTimeData(
                        it.key,
                        it.name,
                        it.hourMinutes.toSortedMap())
            }
        }

        val showHour = instanceDatas.values.all { it.instanceDateTime.timeStamp.toExactTimeStamp() < now }

        return EditInstancesViewModel.Data(instanceDatas, customTimeDatas, showHour)
    }

    @Synchronized
    fun getShowCustomTimeData(customTimeKey: CustomTimeKey.Private): ShowCustomTimeViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowCustomTimeData")

        val customTime = projectsFactory.privateProject.getCustomTime(customTimeKey)

        val hourMinutes = DayOfWeek.values().associate { it to customTime.getHourMinute(it) }

        return ShowCustomTimeViewModel.Data(customTimeKey, customTime.name, hourMinutes)
    }

    @Synchronized
    fun getShowCustomTimesData(): ShowCustomTimesViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowCustomTimesData")

        val now = ExactTimeStamp.now

        val entries = getCurrentRemoteCustomTimes(now).map {
            val days = it.hourMinutes
                    .entries
                    .groupBy { it.value }
                    .mapValues { it.value.map { it.key } }
                    .entries
                    .sortedBy { it.key }

            val details = days.joinToString("; ") {
                it.value
                        .toSet()
                        .prettyPrint() + it.key
            }

            ShowCustomTimesViewModel.CustomTimeData(
                    it.key,
                    it.name,
                    details
            )
        }.toMutableList()

        return ShowCustomTimesViewModel.Data(entries)
    }

    @Synchronized
    fun getGroupListData(
            now: ExactTimeStamp,
            position: Int,
            timeRange: MainActivity.TimeRange
    ): DayViewModel.DayData {
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

            startExactTimeStamp = ExactTimeStamp(Date(startCalendar.toDateTimeTz()), HourMilli(0, 0, 0, 0))
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

        endExactTimeStamp = ExactTimeStamp(Date(endCalendar.toDateTimeTz()), HourMilli(0, 0, 0, 0))

        val currentInstances = getRootInstances(startExactTimeStamp, endExactTimeStamp, now)

        val customTimeDatas = getCurrentRemoteCustomTimes(now).map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes.toSortedMap()) }

        val taskDatas = if (position == 0) {
            getTasks().filter { it.current(now) && it.isVisible(now, true) && it.isRootTask(now) && it.getCurrentSchedules(now).isEmpty() }
                    .map {
                        GroupListFragment.TaskData(
                                it.taskKey,
                                it.name,
                                getGroupListChildTaskDatas(it, now),
                                it.startExactTimeStamp,
                                it.note,
                                it.getImage(deviceDbInfo)
                        )
                    }
                    .toList()
        } else {
            listOf()
        }

        val instanceDatas = currentInstances.map { instance ->
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)

            val instanceData = GroupListFragment.InstanceData(
                    instance.done,
                    instance.instanceKey,
                    instance.getDisplayData(now)?.getDisplayText(),
                    instance.name,
                    instance.instanceDateTime.timeStamp,
                    task.current(now),
                    instance.isRootInstance(now),
                    isRootTask,
                    instance.exists(),
                    instance.getCreateTaskTimePair(ownerKey),
                    task.note,
                    children,
                    null,
                    instance.ordinal,
                    instance.getNotificationShown(localFactory),
                    task.getImage(deviceDbInfo)
            )

            children.values.forEach { it.instanceDataParent = instanceData }

            instanceData
        }

        val dataWrapper = GroupListFragment.DataWrapper(
                customTimeDatas,
                null,
                taskDatas,
                null,
                instanceDatas,
                null
        )

        instanceDatas.forEach { it.instanceDataParent = dataWrapper }

        return DayViewModel.DayData(dataWrapper)
    }

    @Synchronized
    fun getShowGroupData(timeStamp: TimeStamp): ShowGroupViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowGroupData")

        val now = ExactTimeStamp.now

        val date = timeStamp.date
        val dayOfWeek = date.dayOfWeek
        val hourMinute = timeStamp.hourMinute

        val time = getCurrentRemoteCustomTimes(now).firstOrNull { it.getHourMinute(dayOfWeek) == hourMinute }
                ?: Time.Normal(hourMinute)

        val displayText = DateTime(date, time).getDisplayText()

        return ShowGroupViewModel.Data(displayText, getGroupListData(timeStamp, now))
    }

    @Synchronized
    fun getShowTaskInstancesData(taskKey: TaskKey): ShowTaskInstancesViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowTaskInstancesData")

        val task = getTaskForce(taskKey)
        val now = ExactTimeStamp.now

        val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
            GroupListFragment.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
        }

        val isRootTask = if (task.current(now)) task.isRootTask(now) else null

        val instances = (task.existingInstances.values + task.getInstances(null, now, now)).toSet()

        val hierarchyExactTimeStamp = task.getHierarchyExactTimeStamp(now)

        val instanceDatas = instances.map {
            val children = getChildInstanceDatas(it, now)

            val hierarchyData = if (task.isRootTask(hierarchyExactTimeStamp))
                null
            else
                task.getParentTaskHierarchy(hierarchyExactTimeStamp)!!.run { HierarchyData(taskHierarchyKey, ordinal) }

            GroupListFragment.InstanceData(
                    it.done,
                    it.instanceKey,
                    it.getDisplayData(now)?.getDisplayText(),
                    it.name,
                    it.instanceDateTime.timeStamp,
                    task.current(now),
                    it.isRootInstance(now),
                    isRootTask,
                    it.exists(),
                    it.getCreateTaskTimePair(ownerKey),
                    task.note,
                    children,
                    hierarchyData,
                    it.ordinal,
                    it.getNotificationShown(localFactory),
                    task.getImage(deviceDbInfo)
            )
        }

        val dataWrapper = GroupListFragment.DataWrapper(
                customTimeDatas,
                task.current(now),
                listOf(),
                null,
                instanceDatas,
                null
        )

        instanceDatas.forEach { it.instanceDataParent = dataWrapper }

        return ShowTaskInstancesViewModel.Data(dataWrapper)
    }

    @Synchronized
    fun getShowNotificationGroupData(instanceKeys: Set<InstanceKey>): ShowNotificationGroupViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowNotificationGroupData")

        check(instanceKeys.isNotEmpty())

        val now = ExactTimeStamp.now

        val instances = instanceKeys.map { getInstance(it) }
                .filter { it.isRootInstance(now) }
                .sortedBy { it.instanceDateTime }

        val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
            GroupListFragment.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
        }

        val instanceDatas = instances.map { instance ->
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)

            val instanceData = GroupListFragment.InstanceData(
                    instance.done,
                    instance.instanceKey,
                    instance.getDisplayData(now)?.getDisplayText(),
                    instance.name,
                    instance.instanceDateTime.timeStamp,
                    task.current(now),
                    instance.isRootInstance(now),
                    isRootTask,
                    instance.exists(),
                    instance.getCreateTaskTimePair(ownerKey),
                    task.note,
                    children,
                    null,
                    instance.ordinal,
                    instance.getNotificationShown(localFactory),
                    task.getImage(deviceDbInfo)
            )

            children.values.forEach { it.instanceDataParent = instanceData }

            instanceData
        }

        val dataWrapper = GroupListFragment.DataWrapper(
                customTimeDatas,
                null,
                listOf(),
                null,
                instanceDatas,
                null
        )

        instanceDatas.forEach { it.instanceDataParent = dataWrapper }

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
        val displayText = if (instance.isRootInstance(now)) instanceDateTime.getDisplayText() else null

        return ShowInstanceViewModel.Data(
                instance.name,
                instance.getParentName(now),
                instanceDateTime,
                instance.done != null,
                task.current(now),
                parentInstance == null,
                instance.exists(),
                getGroupListData(instance, task, now),
                instance.getNotificationShown(localFactory),
                displayText,
                task.taskKey
        )
    }

    @Synchronized
    fun getCreateTaskData(
            taskKey: TaskKey?,
            joinTaskKeys: List<TaskKey>?,
            parentTaskKeyHint: TaskKey?
    ): CreateTaskViewModel.Data {
        MyCrashlytics.logMethod(this, "parentTaskKeyHint: $parentTaskKeyHint")

        check(taskKey == null || joinTaskKeys == null)

        val now = ExactTimeStamp.now

        val customTimes = getCurrentRemoteCustomTimes(now).associateBy {
            it.key
        }.toMutableMap<CustomTimeKey<*>, Time.Custom<*>>()

        val excludedTaskKeys = when {
            taskKey != null -> setOf(taskKey)
            joinTaskKeys != null -> joinTaskKeys.toSet()
            else -> setOf()
        }

        val includeTaskKeys = listOfNotNull(parentTaskKeyHint).toMutableSet()

        fun checkHintPresent(
                task: CreateTaskViewModel.ParentKey.Task,
                parentTreeDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>
        ): Boolean = parentTreeDatas.containsKey(task) || parentTreeDatas.any {
            checkHintPresent(task, it.value.parentTreeDatas)
        }

        fun checkHintPresent(
                parentTreeDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>
        ) = parentTaskKeyHint?.let {
            checkHintPresent(CreateTaskViewModel.ParentKey.Task(it), parentTreeDatas)
        } ?: true

        val taskData = if (taskKey != null) {
            val task = getTaskForce(taskKey)

            val parentKey: CreateTaskViewModel.ParentKey?
            var scheduleDataWrappers: List<CreateTaskViewModel.ScheduleDataWrapper>? = null

            if (task.isRootTask(now)) {
                val schedules = task.getCurrentSchedules(now)

                customTimes += schedules.mapNotNull { it.customTimeKey }.map {
                    it to task.project.getCustomTime(it.customTimeId)
                }

                parentKey = task.project
                        .takeIf { it is SharedProject }
                        ?.let { CreateTaskViewModel.ParentKey.Project(it.projectKey) }

                if (schedules.isNotEmpty()) {
                    scheduleDataWrappers = ScheduleGroup.getGroups(schedules).map {
                        CreateTaskViewModel.ScheduleDataWrapper.fromScheduleData(it.scheduleData)
                    }
                }
            } else {
                val parentTask = task.getParentTask(now)!!
                parentKey = CreateTaskViewModel.ParentKey.Task(parentTask.taskKey)
                includeTaskKeys.add(parentTask.taskKey)
            }

            CreateTaskViewModel.TaskData(
                    task.name,
                    parentKey,
                    scheduleDataWrappers,
                    task.note,
                    task.project.name,
                    task.getImage(deviceDbInfo)
            )
        } else {
            null
        }

        val parentTreeDatas = getParentTreeDatas(now, excludedTaskKeys, includeTaskKeys)

        check(checkHintPresent(parentTreeDatas))

        val customTimeDatas = customTimes.values.associate {
            it.key to CreateTaskViewModel.CustomTimeData(it.key, it.name, it.hourMinutes.toSortedMap())
        }

        return CreateTaskViewModel.Data(
                taskData,
                parentTreeDatas,
                customTimeDatas,
                remoteUserFactory.remoteUser.defaultReminder
        )
    }

    @Synchronized
    fun getShowTaskData(taskKey: TaskKey): ShowTaskViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowTaskData")

        val now = ExactTimeStamp.now

        val task = getTaskForce(taskKey)
        val hierarchyTimeStamp = task.getHierarchyExactTimeStamp(now)

        val childTaskDatas = task.getChildTaskHierarchies(hierarchyTimeStamp)
                .map {
                    val childTask = it.childTask

                    TaskListFragment.ChildTaskData(
                            childTask.name,
                            childTask.getScheduleText(ScheduleText, hierarchyTimeStamp),
                            getTaskListChildTaskDatas(childTask, now, true, hierarchyTimeStamp),
                            childTask.note,
                            childTask.startExactTimeStamp,
                            childTask.taskKey,
                            HierarchyData(it.taskHierarchyKey, it.ordinal),
                            childTask.getImage(deviceDbInfo),
                            childTask.current(now),
                            childTask.hasInstances(now),
                            true)
                }
                .sorted()

        return ShowTaskViewModel.Data(
                task.name,
                task.getParentName(hierarchyTimeStamp),
                task.getScheduleTextMultiline(ScheduleText, hierarchyTimeStamp),
                TaskListFragment.TaskData(childTaskDatas.toMutableList(), task.note, task.current(now)),
                task.hasInstances(now),
                task.getImage(deviceDbInfo),
                task.current(now))
    }

    @Synchronized
    fun getMainData(): MainViewModel.Data {
        MyCrashlytics.log("DomainFactory.getMainData")

        val now = ExactTimeStamp.now

        val childTaskDatas = getTasks().map {
            val hierarchyExactTimeStamp = it.getHierarchyExactTimeStamp(now)
            Pair(it, hierarchyExactTimeStamp)
        }
                .filter { (task, hierarchyExactTimeStamp) -> task.isRootTask(hierarchyExactTimeStamp) }
                .map { (task, hierarchyExactTimeStamp) ->
                    TaskListFragment.ChildTaskData(
                            task.name,
                            task.getScheduleText(ScheduleText, hierarchyExactTimeStamp),
                            getTaskListChildTaskDatas(task, now, false, hierarchyExactTimeStamp),
                            task.note,
                            task.startExactTimeStamp,
                            task.taskKey,
                            null,
                            task.getImage(deviceDbInfo),
                            task.current(now),
                            task.hasInstances(now),
                            false
                    )
                }
                .sortedDescending()
                .toMutableList()

        return MainViewModel.Data(
                TaskListFragment.TaskData(childTaskDatas, null, true),
                remoteUserFactory.remoteUser.defaultTab
        )
    }

    @Synchronized
    fun getDrawerData(): DrawerViewModel.Data {
        MyCrashlytics.log("DomainFactory.getDrawerData")

        return remoteUserFactory.remoteUser.run { DrawerViewModel.Data(name, email, photoUrl) }
    }

    @Synchronized
    fun getProjectListData(): ProjectListViewModel.Data {
        MyCrashlytics.log("DomainFactory.getProjectListData")

        val remoteProjects = projectsFactory.sharedProjects

        val now = ExactTimeStamp.now

        val projectDatas = remoteProjects.values
                .filter { it.current(now) }
                .associate {
                    val users = it.users.joinToString(", ") { it.name }

                    it.projectKey to ProjectListViewModel.ProjectData(it.projectKey, it.name, users)
                }
                .toSortedMap()

        return ProjectListViewModel.Data(projectDatas)
    }

    @Synchronized
    fun getFriendListData(): FriendListViewModel.Data {
        MyCrashlytics.log("DomainFactory.getFriendListData")

        val friends = remoteFriendFactory.friends

        val userListDatas = friends.map {
            FriendListViewModel.UserListData(it.name, it.email, it.id, it.photoUrl, it.userWrapper)
        }.toMutableSet()

        return FriendListViewModel.Data(userListDatas)
    }

    @Synchronized
    fun getShowProjectData(projectId: ProjectKey.Shared?): ShowProjectViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowProjectData")

        val friendDatas = remoteFriendFactory.friends
                .map { ShowProjectViewModel.UserListData(it.name, it.email, it.id, it.photoUrl) }
                .associateBy { it.id }

        val name: String?
        val userListDatas: Set<ShowProjectViewModel.UserListData>
        if (projectId != null) {
            val remoteProject = projectsFactory.getProjectForce(projectId) as SharedProject

            name = remoteProject.name

            userListDatas = remoteProject.users
                    .filterNot { it.id == deviceDbInfo.key }
                    .map { ShowProjectViewModel.UserListData(it.name, it.email, it.id, it.photoUrl) }
                    .toSet()
        } else {
            name = null
            userListDatas = setOf()
        }

        return ShowProjectViewModel.Data(name, userListDatas, friendDatas)
    }

    @Synchronized
    fun getSettingsData(): SettingsViewModel.Data {
        MyCrashlytics.log("DomainFactory.getSettingsData")

        return SettingsViewModel.Data(remoteUserFactory.remoteUser.defaultReminder)
    }

    // sets

    @Synchronized
    fun setInstancesDateTime(dataId: Int, source: SaveService.Source, instanceKeys: Set<InstanceKey>, instanceDate: Date, instanceTimePair: TimePair) {
        MyCrashlytics.log("DomainFactory.setInstancesDateTime")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(instanceKeys.isNotEmpty())

        val now = ExactTimeStamp.now

        val instances = instanceKeys.map(this::getInstance)

        instances.forEach { it.setInstanceDateTime(localFactory, ownerKey, DateTime(instanceDate, getTime(instanceTimePair)), now) }

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
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val instance = getInstance(instanceKey)
        Preferences.tickLog.logLineHour("DomainFactory: adding hour to ${instance.name}")

        val now = ExactTimeStamp.now
        val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

        val date = Date(calendar.toDateTimeTz())
        val hourMinute = HourMinute(calendar.toDateTimeTz())

        instance.setInstanceDateTime(localFactory, ownerKey, DateTime(date, Time.Normal(hourMinute)), now)
        instance.setNotificationShown(localFactory, false)

        updateNotifications(now, sourceName = "setInstanceAddHourService ${instance.name}")

        save(0, source)

        notifyCloud(instance.project)
    }

    @Synchronized
    fun setInstancesAddHourActivity(dataId: Int, source: SaveService.Source, instanceKeys: Collection<InstanceKey>): HourUndoData {
        MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now
        val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

        val date = Date(calendar.toDateTimeTz())
        val hourMinute = HourMinute(calendar.toDateTimeTz())

        val instances = instanceKeys.map(this::getInstance)

        val instanceDateTimes = instances.associate { it.instanceKey to it.instanceDateTime }

        instances.forEach { it.setInstanceDateTime(localFactory, ownerKey, DateTime(date, Time.Normal(hourMinute)), now) }

        updateNotifications(now)

        save(dataId, source)

        val remoteProjects = instances.map { it.project }.toSet()

        notifyCloud(remoteProjects)

        return HourUndoData(instanceDateTimes)
    }

    @Synchronized
    fun undoInstancesAddHour(dataId: Int, source: SaveService.Source, hourUndoData: HourUndoData) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val pairs = hourUndoData.instanceDateTimes.map { (instanceKey, instanceDateTime) -> Pair(getInstance(instanceKey), instanceDateTime) }

        pairs.forEach { (instance, instanceDateTime) ->
            instance.setInstanceDateTime(localFactory, ownerKey, instanceDateTime, now)
        }

        updateNotifications(now)

        save(dataId, source)

        val remoteProjects = pairs.map { it.first.project }.toSet()

        notifyCloud(remoteProjects)
    }

    @Synchronized
    fun setInstanceNotificationDone(source: SaveService.Source, instanceKey: InstanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotificationDone")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val instance = getInstance(instanceKey)
        Preferences.tickLog.logLineHour("DomainFactory: setting ${instance.name} done")

        val now = ExactTimeStamp.now

        instance.setDone(uuid, localFactory, true, now)
        instance.setNotificationShown(localFactory, false)

        updateNotifications(now, sourceName = "setInstanceNotificationDone ${instance.name}")

        save(0, source)

        notifyCloud(instance.project)
    }

    @Synchronized
    fun setInstanceDone(dataId: Int, source: SaveService.Source, instanceKey: InstanceKey, done: Boolean): ExactTimeStamp? {
        MyCrashlytics.log("DomainFactory.setInstanceDone")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val instance = getInstance(instanceKey)

        instance.setDone(uuid, localFactory, done, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(instance.project)

        return instance.done
    }

    @Synchronized
    fun setInstancesNotNotified(dataId: Int, source: SaveService.Source, instanceKeys: List<InstanceKey>) {
        MyCrashlytics.log("DomainFactory.setInstancesNotNotified")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        instanceKeys.forEach {
            val instance = getInstance(it)
            check(instance.done == null)
            check(instance.instanceDateTime.timeStamp.toExactTimeStamp() <= now)
            check(!instance.getNotificationShown(localFactory))
            check(instance.isRootInstance(now))

            instance.setNotified(localFactory, false)
            instance.setNotificationShown(localFactory, false)
        }

        updateNotifications(now)

        save(dataId, source)
    }

    @Synchronized
    fun setInstancesDone(dataId: Int, source: SaveService.Source, instanceKeys: List<InstanceKey>, done: Boolean): ExactTimeStamp {
        MyCrashlytics.log("DomainFactory.setInstancesDone")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val instances = instanceKeys.map(this::getInstance)

        instances.forEach { it.setDone(uuid, localFactory, done, now) }

        val remoteProjects = instances.map { it.project }.toSet()

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)

        return now
    }

    @Synchronized
    fun checkSave() {
        if (projectsFactory.isSaved) throw SavedFactoryException()
    }

    @Synchronized
    fun setInstanceNotified(dataId: Int, source: SaveService.Source, instanceKey: InstanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotified")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val instance = getInstance(instanceKey)

        Preferences.tickLog.logLineHour("DomainFactory: setting notified: ${instance.name}")
        setInstanceNotified(instanceKey)

        save(dataId, source)
    }

    @Synchronized
    fun setInstancesNotified(source: SaveService.Source, instanceKeys: List<InstanceKey>) {
        MyCrashlytics.log("DomainFactory.setInstancesNotified")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(instanceKeys.isNotEmpty())

        for (instanceKey in instanceKeys)
            setInstanceNotified(instanceKey)

        save(0, source)
    }

    @Synchronized
    fun createScheduleRootTask(
            dataId: Int,
            source: SaveService.Source,
            name: String,
            scheduleDatas: List<ScheduleData>,
            note: String?,
            projectId: ProjectKey<*>?,
            imagePath: Pair<String, Uri>?,
            copyTaskKey: TaskKey? = null
    ): TaskKey {
        MyCrashlytics.log("DomainFactory.createScheduleRootTask")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        check(name.isNotEmpty())
        check(scheduleDatas.isNotEmpty())

        val finalProjectId = projectId ?: defaultProjectId

        val imageUuid = imagePath?.let { newUuid() }

        val task = projectsFactory.createScheduleRootTask(
                now,
                name,
                scheduleDatas.map { it to getTime(it.timePair) },
                note,
                finalProjectId,
                imageUuid,
                deviceDbInfo
        )

        copyTaskKey?.let { copyTask(now, task, it) }

        updateNotifications(now)

        task.updateOldestVisible(uuid, now)

        save(dataId, source)

        notifyCloud(task.project)

        imageUuid?.let {
            Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath)
        }

        return task.taskKey
    }

    @Synchronized
    fun updateScheduleTask(
            dataId: Int,
            source: SaveService.Source,
            taskKey: TaskKey,
            name: String,
            scheduleDatas: List<ScheduleData>,
            note: String?,
            projectId: ProjectKey<*>?,
            imagePath: NullableWrapper<Pair<String, Uri>>?
    ): TaskKey {
        MyCrashlytics.log("DomainFactory.updateScheduleTask")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(name.isNotEmpty())
        check(scheduleDatas.isNotEmpty())

        val now = ExactTimeStamp.now

        check(name.isNotEmpty())
        check(scheduleDatas.isNotEmpty())

        val imageUuid = imagePath?.value?.let { newUuid() }

        val task = getTaskForce(taskKey).run {
            requireCurrent(now)
            updateProject(this@DomainFactory, now, projectId ?: defaultProjectId)
        }.apply {
            setName(name, note)

            if (!isRootTask(now))
                getParentTaskHierarchy(now)!!.setEndExactTimeStamp(now)

            updateSchedules(ownerKey, localFactory, scheduleDatas.map { it to getTime(it.timePair) }, now)

            if (imagePath != null)
                setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })
        }

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.project)

        imageUuid?.let {
            Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value)
        }

        return task.taskKey
    }

    @Synchronized
    fun createScheduleJoinRootTask(
            now: ExactTimeStamp,
            dataId: Int,
            source: SaveService.Source,
            name: String,
            scheduleDatas: List<ScheduleData>,
            joinTaskKeys: List<TaskKey>,
            note: String?,
            projectId: ProjectKey<*>?,
            imagePath: Pair<String, Uri>?,
            removeInstanceKeys: List<InstanceKey>
    ): TaskKey {
        MyCrashlytics.log("DomainFactory.createScheduleJoinRootTask")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(name.isNotEmpty())
        check(scheduleDatas.isNotEmpty())
        check(joinTaskKeys.size > 1)

        val finalProjectId = projectId ?: defaultProjectId

        val joinTasks = joinTaskKeys.map { getTaskForce(it).updateProject(this, now, finalProjectId) }

        val imageUuid = imagePath?.let { newUuid() }

        val newParentTask = projectsFactory.createScheduleRootTask(
                now,
                name,
                scheduleDatas.map { it to getTime(it.timePair) },
                note,
                finalProjectId,
                imageUuid,
                deviceDbInfo
        )

        joinTasks(newParentTask, joinTasks, now, removeInstanceKeys)

        updateNotifications(now)

        newParentTask.updateOldestVisible(uuid, now)

        save(dataId, source)

        notifyCloud(newParentTask.project)

        imageUuid?.let {
            Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath)
        }

        return newParentTask.taskKey
    }

    @Synchronized
    fun createRootTask(
            dataId: Int,
            source: SaveService.Source,
            name: String,
            note: String?,
            projectId: ProjectKey<*>?,
            imagePath: Pair<String, Uri>?,
            copyTaskKey: TaskKey? = null
    ): TaskKey {
        MyCrashlytics.log("DomainFactory.createRootTask")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(name.isNotEmpty())

        val now = ExactTimeStamp.now

        val finalProjectId = projectId ?: defaultProjectId

        val imageUuid = imagePath?.let { newUuid() }

        val task = projectsFactory.createTaskHelper(
                now,
                name,
                note,
                finalProjectId,
                imageUuid,
                deviceDbInfo
        )

        copyTaskKey?.let { copyTask(now, task, it) }

        updateNotifications(now)

        task.updateOldestVisible(uuid, now)

        save(dataId, source)

        notifyCloud(task.project)

        imageUuid?.let {
            Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath)
        }

        return task.taskKey
    }

    @Synchronized
    fun createJoinRootTask(
            dataId: Int,
            source: SaveService.Source,
            name: String,
            joinTaskKeys: List<TaskKey>,
            note: String?,
            projectId: ProjectKey<*>?,
            imagePath: Pair<String, Uri>?,
            removeInstanceKeys: List<InstanceKey>
    ): TaskKey {
        MyCrashlytics.log("DomainFactory.createJoinRootTask")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(name.isNotEmpty())
        check(joinTaskKeys.size > 1)

        val now = ExactTimeStamp.now

        val finalProjectId = projectId ?: joinTaskKeys.map { it.projectKey }
                .distinct()
                .single()

        val joinTasks = joinTaskKeys.map { getTaskForce(it).updateProject(this, now, finalProjectId) }

        val imageUuid = imagePath?.let { newUuid() }

        val newParentTask = projectsFactory.createTaskHelper(
                now,
                name,
                note,
                finalProjectId,
                imageUuid,
                deviceDbInfo
        )

        joinTasks(newParentTask, joinTasks, now, removeInstanceKeys)

        updateNotifications(now)

        newParentTask.updateOldestVisible(uuid, now)

        save(dataId, source)

        notifyCloud(newParentTask.project)

        imageUuid?.let {
            Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath)
        }

        return newParentTask.taskKey
    }

    @Synchronized
    fun updateRootTask(
            dataId: Int,
            source: SaveService.Source,
            taskKey: TaskKey,
            name: String,
            note: String?,
            projectId: ProjectKey<*>?,
            imagePath: NullableWrapper<Pair<String, Uri>>?
    ): TaskKey {
        MyCrashlytics.log("DomainFactory.updateRootTask")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(name.isNotEmpty())

        val now = ExactTimeStamp.now

        val task = getTaskForce(taskKey).run {
            requireCurrent(now)
            updateProject(this@DomainFactory, now, projectId ?: defaultProjectId)
        }.apply {
            setName(name, note)
            getParentTaskHierarchy(now)?.setEndExactTimeStamp(now)
            getCurrentSchedules(now).forEach { it.setEndExactTimeStamp(now) }
        }

        val imageUuid = imagePath?.value?.let { newUuid() }
        if (imagePath != null)
            task.setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.project)

        imageUuid?.let {
            Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value)
        }

        return task.taskKey
    }

    @Synchronized
    fun createChildTask(
            dataId: Int,
            source: SaveService.Source,
            parentTaskKey: TaskKey,
            name: String,
            note: String?,
            imagePath: Pair<String, Uri>?,
            copyTaskKey: TaskKey? = null
    ): TaskKey {
        MyCrashlytics.log("DomainFactory.createChildTask")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        check(name.isNotEmpty())

        val parentTask = getTaskForce(parentTaskKey)
        parentTask.requireCurrent(now)

        val imageUuid = imagePath?.let { newUuid() }

        val childTask = createChildTask(now, parentTask, name, note, imageUuid?.let { TaskJson.Image(it, uuid) }, copyTaskKey)

        updateNotifications(now)

        childTask.updateOldestVisible(uuid, now)

        save(dataId, source)

        notifyCloud(childTask.project)

        imageUuid?.let {
            Uploader.addUpload(deviceDbInfo, childTask.taskKey, it, imagePath)
        }

        return childTask.taskKey
    }

    private fun <T : ProjectType> createChildTask(
            now: ExactTimeStamp,
            parentTask: Task<T>,
            name: String,
            note: String?,
            imageJson: TaskJson.Image?,
            copyTaskKey: TaskKey? = null
    ): Task<T> {
        check(name.isNotEmpty())
        parentTask.requireCurrent(now)

        val childTask = parentTask.createChildTask(now, name, note, imageJson)

        copyTaskKey?.let { copyTask(now, childTask, it) }

        return childTask
    }

    @Synchronized
    fun createJoinChildTask(
            dataId: Int,
            source: SaveService.Source,
            parentTaskKey: TaskKey,
            name: String,
            joinTaskKeys: List<TaskKey>,
            note: String?,
            imagePath: Pair<String, Uri>?,
            removeInstanceKeys: List<InstanceKey>
    ): TaskKey {
        MyCrashlytics.log("DomainFactory.createJoinChildTask")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(name.isNotEmpty())
        check(joinTaskKeys.size > 1)

        val now = ExactTimeStamp.now

        val parentTask = getTaskForce(parentTaskKey)
        parentTask.requireCurrent(now)

        check(joinTaskKeys.map { it.projectKey }.distinct().size == 1)

        val joinTasks = joinTaskKeys.map { getTaskForce(it) }

        val imageUuid = imagePath?.let { newUuid() }

        val childTask = parentTask.createChildTask(now, name, note, imageUuid?.let { TaskJson.Image(it, uuid) })

        joinTasks(childTask, joinTasks, now, removeInstanceKeys)

        updateNotifications(now)

        childTask.updateOldestVisible(uuid, now)

        save(dataId, source)

        notifyCloud(childTask.project)

        imageUuid?.let {
            Uploader.addUpload(deviceDbInfo, childTask.taskKey, it, imagePath)
        }

        return childTask.taskKey
    }

    @Synchronized
    fun updateChildTask(
            now: ExactTimeStamp,
            dataId: Int,
            source: SaveService.Source,
            taskKey: TaskKey,
            name: String,
            parentTaskKey: TaskKey,
            note: String?,
            imagePath: NullableWrapper<Pair<String, Uri>>?
    ): TaskKey {
        MyCrashlytics.log("DomainFactory.updateChildTask")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(name.isNotEmpty())

        val task = getTaskForce(taskKey)
        task.requireCurrent(now)

        val newParentTask = getTaskForce(parentTaskKey)
        newParentTask.requireCurrent(now)

        task.setName(name, note)

        val oldParentTask = task.getParentTask(now)
        if (oldParentTask == null) {
            task.getCurrentSchedules(now).forEach { it.setEndExactTimeStamp(now) }

            newParentTask.addChild(task, now)
        } else if (oldParentTask !== newParentTask) {
            task.getParentTaskHierarchy(now)!!.setEndExactTimeStamp(now)

            newParentTask.addChild(task, now)
        }

        val imageUuid = imagePath?.value?.let { newUuid() }
        if (imagePath != null)
            task.setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(task.project) // todo image on server, purge images after this call

        imageUuid?.let {
            Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value)
        }

        return task.taskKey
    }

    @Synchronized
    fun setInstanceOrdinal(dataId: Int, instanceKey: InstanceKey, ordinal: Double) {
        MyCrashlytics.log("DomainFactory.setInstanceOrdinal")
        if (projectsFactory.isSaved) throw SavedFactoryException()

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
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val (projectId, taskHierarchyId) = hierarchyData.taskHierarchyKey

        val remoteProject = projectsFactory.getProjectForce(projectId)
        val taskHierarchy = remoteProject.getTaskHierarchy(taskHierarchyId)
        taskHierarchy.requireCurrent(now)

        taskHierarchy.ordinal = hierarchyData.ordinal

        updateNotifications(now)

        save(dataId, SaveService.Source.GUI)

        notifyCloud(remoteProject)
    }

    private fun setTaskEndTimeStamps(source: SaveService.Source, taskKeys: Set<TaskKey>, deleteInstances: Boolean, now: ExactTimeStamp): TaskUndoData {
        check(taskKeys.isNotEmpty())

        val tasks = taskKeys.map { getTaskForce(it) }.toMutableSet()
        tasks.forEach { it.requireCurrent(now) }

        fun parentPresent(task: Task<*>): Boolean = task.getParentTask(now)?.let {
            tasks.contains(it) || parentPresent(it)
        } ?: false

        tasks.toMutableSet().forEach {
            if (parentPresent(it))
                tasks.remove(it)
        }

        val taskUndoData = TaskUndoData()

        tasks.forEach { it.setEndData(uuid, Task.EndData(now, deleteInstances), taskUndoData) }

        val remoteProjects = tasks.map { it.project }.toSet()

        updateNotifications(now)

        save(0, source)

        notifyCloud(remoteProjects)

        return taskUndoData
    }

    @Synchronized
    fun setTaskEndTimeStamps(source: SaveService.Source, taskKeys: Set<TaskKey>, deleteInstances: Boolean): TaskUndoData {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        return setTaskEndTimeStamps(source, taskKeys, deleteInstances, ExactTimeStamp.now)
    }

    @Synchronized
    fun setTaskEndTimeStamps(source: SaveService.Source, taskKeys: Set<TaskKey>, deleteInstances: Boolean, instanceKey: InstanceKey): Pair<TaskUndoData, Boolean> {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        val taskUndoData = setTaskEndTimeStamps(source, taskKeys, deleteInstances, now)

        val instance = getInstance(instanceKey)
        val task = instance.task

        val instanceExactTimeStamp by lazy {
            instance.instanceDateTime
                    .timeStamp
                    .toExactTimeStamp()
        }

        val visible = task.notDeleted(now) || (instance.done != null || instanceExactTimeStamp <= now) || (!deleteInstances && instance.exists())

        return Pair(taskUndoData, visible)
    }

    @Synchronized
    fun clearTaskEndTimeStamps(source: SaveService.Source, taskUndoData: TaskUndoData) {
        MyCrashlytics.log("DomainFactory.clearTaskEndTimeStamps")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        processTaskUndoData(taskUndoData, now)

        updateNotifications(now)

        save(0, source)

        val remoteProjects = taskUndoData.taskKeys
                .map { getTaskForce(it).project }
                .toSet()

        notifyCloud(remoteProjects)
    }

    private fun processTaskUndoData(taskUndoData: TaskUndoData, now: ExactTimeStamp) {
        taskUndoData.taskKeys
                .map { getTaskForce(it) }
                .forEach {
                    it.requireNotCurrent(now)

                    it.clearEndExactTimeStamp(uuid, now)
                }

        taskUndoData.taskHierarchyKeys
                .map { projectsFactory.getTaskHierarchy(it) }
                .forEach {
                    it.requireNotCurrent(now)

                    it.clearEndExactTimeStamp(now)
                }

        taskUndoData.scheduleIds
                .map { projectsFactory.getSchedule(it) }
                .forEach {
                    it.requireNotCurrent(now)

                    it.clearEndExactTimeStamp(now)
                }
    }

    @Synchronized
    fun createCustomTime(source: SaveService.Source, name: String, hourMinutes: Map<DayOfWeek, HourMinute>): CustomTimeKey.Private {
        MyCrashlytics.log("DomainFactory.createCustomTime")
        if (projectsFactory.isSaved) throw SavedFactoryException()

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
                true
        )

        val remoteCustomTime = projectsFactory.privateProject.newRemoteCustomTime(customTimeJson)

        save(0, source)

        return remoteCustomTime.key
    }

    @Synchronized
    fun updateCustomTime(
            dataId: Int,
            source: SaveService.Source,
            customTimeId: CustomTimeKey.Private,
            name: String,
            hourMinutes: Map<DayOfWeek, HourMinute>
    ) {
        MyCrashlytics.log("DomainFactory.updateCustomTime")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(name.isNotEmpty())

        val customTime = projectsFactory.privateProject.getCustomTime(customTimeId)

        customTime.setName(this, name)

        for (dayOfWeek in DayOfWeek.values()) {
            val hourMinute = hourMinutes.getValue(dayOfWeek)

            if (hourMinute != customTime.getHourMinute(dayOfWeek))
                customTime.setHourMinute(this, dayOfWeek, hourMinute)
        }

        save(dataId, source)
    }

    @Synchronized
    fun setCustomTimesCurrent(
            dataId: Int,
            source: SaveService.Source,
            customTimeIds: List<CustomTimeKey<ProjectType.Private>>,
            current: Boolean
    ) {
        MyCrashlytics.log("DomainFactory.setCustomTimesCurrent")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        check(customTimeIds.isNotEmpty())

        val now = ExactTimeStamp.now
        val endExactTimeStamp = now.takeUnless { current }

        for (customTimeId in customTimeIds) {
            val remotePrivateCustomTime = projectsFactory.privateProject.getCustomTime(customTimeId)

            remotePrivateCustomTime.endExactTimeStamp = endExactTimeStamp
        }

        save(dataId, source)
    }

    @Synchronized
    fun updateNotificationsTick(source: SaveService.Source, silent: Boolean, sourceName: String) {
        MyCrashlytics.log("DomainFactory.updateNotificationsTick source: $sourceName")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.now

        updateNotificationsTick(now, silent, sourceName)

        save(0, source)
    }

    private fun updateNotificationsTick(now: ExactTimeStamp, silent: Boolean, sourceName: String) {
        updateNotifications(now, silent = silent, sourceName = sourceName)

        setIrrelevant(now)

        projectsFactory.let { localFactory.deleteInstanceShownRecords(it.taskKeys) }
    }

    @Synchronized
    fun removeFriends(source: SaveService.Source, keys: Set<UserKey>) {
        MyCrashlytics.log("DomainFactory.removeFriends")
        check(!remoteFriendFactory.isSaved)

        keys.forEach { remoteUserFactory.remoteUser.removeFriend(it) }

        save(0, source)
    }

    @Synchronized
    fun addFriend(source: SaveService.Source, userKey: UserKey, userWrapper: UserWrapper) {
        MyCrashlytics.log("DomainFactory.addFriend")
        check(!remoteUserFactory.isSaved)

        remoteUserFactory.remoteUser.addFriend(userKey)
        remoteFriendFactory.addFriend(userKey, userWrapper)

        save(0, source)
    }

    @Synchronized
    fun addFriends(source: SaveService.Source, userMap: Map<UserKey, UserWrapper>) {
        MyCrashlytics.log("DomainFactory.addFriends")
        check(!remoteUserFactory.isSaved)

        userMap.forEach {
            remoteUserFactory.remoteUser.addFriend(it.key)
            remoteFriendFactory.addFriend(it.key, it.value)
        }

        save(0, source)
    }

    @Synchronized
    override fun updateDeviceDbInfo(deviceDbInfo: DeviceDbInfo, source: SaveService.Source) {
        MyCrashlytics.log("DomainFactory.updateDeviceDbInfo")
        if (remoteUserFactory.isSaved || projectsFactory.isSharedSaved) throw SavedFactoryException()

        this.deviceDbInfo = deviceDbInfo

        remoteUserFactory.remoteUser.setToken(deviceDbInfo)
        projectsFactory.updateDeviceInfo(deviceDbInfo)

        save(0, source)
    }

    @Synchronized
    fun updatePhotoUrl(source: SaveService.Source, photoUrl: String) {
        MyCrashlytics.log("DomainFactory.updatePhotoUrl")
        if (remoteUserFactory.isSaved || projectsFactory.isSharedSaved) throw SavedFactoryException()

        remoteUserFactory.remoteUser.photoUrl = photoUrl
        projectsFactory.updatePhotoUrl(deviceDbInfo.deviceInfo, photoUrl)

        save(0, source)
    }

    @Synchronized
    fun updateDefaultReminder(dataId: Int, source: SaveService.Source, defaultReminder: Boolean) {
        MyCrashlytics.log("DomainFactory.updateDefaultReminder")
        if (remoteUserFactory.isSaved) throw SavedFactoryException()

        remoteUserFactory.remoteUser.defaultReminder = defaultReminder

        save(dataId, source)
    }

    @Synchronized
    fun updateDefaultTab(source: SaveService.Source, defaultTab: Int) {
        MyCrashlytics.log("DomainFactory.updateDefaultTab")
        if (remoteUserFactory.isSaved) throw SavedFactoryException()

        remoteUserFactory.remoteUser.defaultTab = defaultTab

        save(0, source)
    }

    @Synchronized
    fun updateProject(
            dataId: Int,
            source: SaveService.Source,
            projectId: ProjectKey.Shared,
            name: String,
            addedFriends: Set<UserKey>,
            removedFriends: Set<UserKey>
    ) {
        MyCrashlytics.log("DomainFactory.updateProject")

        check(name.isNotEmpty())

        val now = ExactTimeStamp.now

        val remoteProject = projectsFactory.getProjectForce(projectId) as SharedProject

        remoteProject.name = name
        remoteProject.updateUsers(addedFriends.map { remoteFriendFactory.getFriend(it) }.toSet(), removedFriends)

        remoteFriendFactory.updateProjects(projectId, addedFriends, removedFriends)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProject, removedFriends)
    }

    @Synchronized
    fun createProject(dataId: Int, source: SaveService.Source, name: String, friends: Set<UserKey>) {
        MyCrashlytics.log("DomainFactory.createProject")

        check(name.isNotEmpty())

        val now = ExactTimeStamp.now

        val recordOf = friends.toMutableSet()

        val key = deviceDbInfo.key
        check(!recordOf.contains(key))
        recordOf.add(key)

        val remoteProject = projectsFactory.createProject(
                name,
                now,
                recordOf,
                remoteUserFactory.remoteUser,
                deviceDbInfo.userInfo,
                remoteFriendFactory
        )

        remoteUserFactory.remoteUser.addProject(remoteProject.projectKey)
        remoteFriendFactory.updateProjects(remoteProject.projectKey, friends, setOf())

        save(dataId, source)

        notifyCloud(remoteProject)
    }

    @Synchronized
    fun setProjectEndTimeStamps(
            dataId: Int,
            source: SaveService.Source,
            projectIds: Set<ProjectKey<*>>,
            removeInstances: Boolean
    ): ProjectUndoData {
        MyCrashlytics.log("DomainFactory.setProjectEndTimeStamps")

        check(projectIds.isNotEmpty())

        val now = ExactTimeStamp.now

        val projectUndoData = ProjectUndoData()

        val remoteProjects = projectIds.map { projectsFactory.getProjectForce(it) }.toSet()

        remoteProjects.forEach {
            it.requireCurrent(now)
            it.setEndExactTimeStamp(uuid, now, projectUndoData, removeInstances)
        }

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
                .map { projectsFactory.getProjectForce(it) }
                .toSet()

        remoteProjects.forEach {
            it.requireNotCurrent(now)
            it.clearEndExactTimeStamp(now)
        }

        processTaskUndoData(projectUndoData.taskUndoData, now)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)
    }

    @Synchronized
    fun setTaskImageUploaded(source: SaveService.Source, taskKey: TaskKey, imageUuid: String) {
        MyCrashlytics.log("DomainFactory.clearProjectEndTimeStamps")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val task = getTaskIfPresent(taskKey) ?: return

        if (task.getImage(deviceDbInfo) != ImageState.Local(imageUuid))
            return

        task.setImage(deviceDbInfo, ImageState.Remote(imageUuid))

        save(0, source)

        notifyCloud(task.project)
    }

    // internal

    private fun getExistingInstanceIfPresent(taskKey: TaskKey, scheduleDateTime: DateTime): Instance<*>? {
        val customTimeKey = scheduleDateTime.time
                .timePair
                .customTimeKey

        val timePair = TimePair(customTimeKey, scheduleDateTime.time.timePair.hourMinute)

        val instanceKey = InstanceKey(taskKey, scheduleDateTime.date, timePair)

        return getExistingInstanceIfPresent(instanceKey)
    }

    private fun getExistingInstanceIfPresent(instanceKey: InstanceKey) = projectsFactory.getExistingInstanceIfPresent(instanceKey)

    override fun getSharedCustomTimes(customTimeKey: CustomTimeKey.Private) = projectsFactory.sharedProjects
            .values
            .mapNotNull { it.getSharedTimeIfPresent(customTimeKey, ownerKey) }

    private fun generateInstance(taskKey: TaskKey, scheduleDateTime: DateTime): Instance<*> {
        return projectsFactory.getTaskForce(taskKey).generateInstance(scheduleDateTime)
    }

    fun getInstance(taskKey: TaskKey, scheduleDateTime: DateTime): Instance<*> {
        val existingInstance = getExistingInstanceIfPresent(taskKey, scheduleDateTime)

        return existingInstance ?: generateInstance(taskKey, scheduleDateTime)
    }

    fun getInstance(instanceKey: InstanceKey): Instance<*> {
        getExistingInstanceIfPresent(instanceKey)?.let { return it }

        val dateTime = DateTime(
                instanceKey.scheduleKey.scheduleDate,
                getTime(instanceKey.scheduleKey.scheduleTimePair)
        )

        return generateInstance(instanceKey.taskKey, dateTime)
    }

    private fun getRootInstances(
            startExactTimeStamp: ExactTimeStamp?,
            endExactTimeStamp: ExactTimeStamp,
            now: ExactTimeStamp
    ) = projectsFactory.projects
            .values
            .flatMap { it.getRootInstances(startExactTimeStamp, endExactTimeStamp, now) }

    private fun getCurrentRemoteCustomTimes(now: ExactTimeStamp) = projectsFactory.privateProject
            .customTimes
            .filter { it.current(now) }

    private fun getChildInstanceDatas(
            instance: Instance<*>,
            now: ExactTimeStamp
    ): MutableMap<InstanceKey, GroupListFragment.InstanceData> {
        return instance.getChildInstances(now)
                .map { (childInstance, taskHierarchy) ->
                    val childTask = childInstance.task

                    val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

                    val children = getChildInstanceDatas(childInstance, now)

                    val instanceData = GroupListFragment.InstanceData(
                            childInstance.done,
                            childInstance.instanceKey,
                            null,
                            childInstance.name,
                            childInstance.instanceDateTime.timeStamp,
                            childTask.current(now),
                            childInstance.isRootInstance(now),
                            isRootTask,
                            childInstance.exists(),
                            childInstance.getCreateTaskTimePair(ownerKey),
                            childTask.note,
                            children,
                            HierarchyData(taskHierarchy.taskHierarchyKey, taskHierarchy.ordinal),
                            childInstance.ordinal,
                            childInstance.getNotificationShown(localFactory),
                            childTask.getImage(deviceDbInfo)
                    )

                    children.values.forEach { it.instanceDataParent = instanceData }
                    childInstance.instanceKey to instanceData
                }
                .toMap()
                .toMutableMap()
    }

    private fun getTaskListChildTaskDatas(
            now: ExactTimeStamp,
            parentTask: Task<*>,
            excludedTaskKeys: Set<TaskKey>
    ): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> =
            parentTask.getChildTaskHierarchies(now)
                    .asSequence()
                    .filterNot { excludedTaskKeys.contains(it.childTaskKey) }
                    .map {
                        val childTask = it.childTask
                        val taskParentKey = CreateTaskViewModel.ParentKey.Task(it.childTaskKey)
                        val parentTreeData = CreateTaskViewModel.ParentTreeData(
                                childTask.name,
                                getTaskListChildTaskDatas(now, childTask, excludedTaskKeys),
                                CreateTaskViewModel.ParentKey.Task(childTask.taskKey),
                                childTask.getScheduleText(ScheduleText, now),
                                childTask.note,
                                CreateTaskViewModel.SortKey.TaskSortKey(childTask.startExactTimeStamp),
                                (childTask.project as? SharedProject)?.projectKey
                        )

                        taskParentKey to parentTreeData
                    }
                    .toList()
                    .toMap()

    private fun Task<*>.showAsParent(
            now: ExactTimeStamp,
            excludedTaskKeys: Set<TaskKey>,
            includedTaskKeys: Set<TaskKey>
    ): Boolean {
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

        parentTreeDatas.putAll(projectsFactory.privateProject
                .tasks
                .filter { it.showAsParent(now, excludedTaskKeys, includedTaskKeys) }
                .map {
                    val taskParentKey = CreateTaskViewModel.ParentKey.Task(it.taskKey)
                    val parentTreeData = CreateTaskViewModel.ParentTreeData(
                            it.name,
                            getTaskListChildTaskDatas(now, it, excludedTaskKeys),
                            taskParentKey,
                            it.getScheduleText(ScheduleText, now),
                            it.note,
                            CreateTaskViewModel.SortKey.TaskSortKey(it.startExactTimeStamp),
                            null
                    )

                    taskParentKey to parentTreeData
                }
                .toMap())

        parentTreeDatas.putAll(projectsFactory.sharedProjects
                .values
                .filter { it.current(now) }
                .map {
                    val projectParentKey = CreateTaskViewModel.ParentKey.Project(it.projectKey)

                    val users = it.users.joinToString(", ") { it.name }
                    val parentTreeData = CreateTaskViewModel.ParentTreeData(
                            it.name,
                            getProjectTaskTreeDatas(now, it, excludedTaskKeys, includedTaskKeys),
                            projectParentKey,
                            users,
                            null,
                            CreateTaskViewModel.SortKey.ProjectSortKey(it.projectKey),
                            it.projectKey
                    )

                    projectParentKey to parentTreeData
                }
                .toMap())

        return parentTreeDatas
    }

    private fun getProjectTaskTreeDatas(
            now: ExactTimeStamp,
            project: Project<*>,
            excludedTaskKeys: Set<TaskKey>,
            includedTaskKeys: Set<TaskKey>
    ): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> {
        return project.tasks
                .filter { it.showAsParent(now, excludedTaskKeys, includedTaskKeys) }
                .map {
                    val taskParentKey = CreateTaskViewModel.ParentKey.Task(it.taskKey)
                    val parentTreeData = CreateTaskViewModel.ParentTreeData(
                            it.name,
                            getTaskListChildTaskDatas(now, it, excludedTaskKeys),
                            taskParentKey,
                            it.getScheduleText(ScheduleText, now),
                            it.note,
                            CreateTaskViewModel.SortKey.TaskSortKey(it.startExactTimeStamp),
                            (it.project as? SharedProject)?.projectKey
                    )

                    taskParentKey to parentTreeData
                }
                .toMap()
    }

    private val ownerKey get() = remoteUserFactory.remoteUser.id

    override fun <T : ProjectType> convert(
            now: ExactTimeStamp,
            startingTask: Task<T>,
            projectId: ProjectKey<*>
    ): Task<*> {
        val remoteToRemoteConversion = RemoteToRemoteConversion<T>()
        val startProject = startingTask.project
        startProject.convertRemoteToRemoteHelper(now, remoteToRemoteConversion, startingTask)

        val remoteProject = projectsFactory.getProjectForce(projectId)

        for (pair in remoteToRemoteConversion.startTasks.values) {
            val remoteTask = remoteProject.copyTask(deviceDbInfo, pair.first, pair.second, now)
            remoteToRemoteConversion.endTasks[pair.first.id] = remoteTask
        }

        for (startTaskHierarchy in remoteToRemoteConversion.startTaskHierarchies) {
            val parentRemoteTask = remoteToRemoteConversion.endTasks[startTaskHierarchy.parentTaskId]!!
            val childRemoteTask = remoteToRemoteConversion.endTasks[startTaskHierarchy.childTaskId]!!

            val remoteTaskHierarchy = remoteProject.copyRemoteTaskHierarchy(
                    now,
                    startTaskHierarchy,
                    parentRemoteTask.id,
                    childRemoteTask.id
            )

            remoteToRemoteConversion.endTaskHierarchies.add(remoteTaskHierarchy)
        }

        val endData = Task.EndData(now, true)

        for (pair in remoteToRemoteConversion.startTasks.values) {
            pair.second.forEach {
                if (!it.hidden)
                    it.hide(uuid, now)
            }

            if (pair.first.getEndData() != null)
                check(pair.first.getEndData() == endData)
            else
                pair.first.setEndData(uuid, endData)
        }

        return remoteToRemoteConversion.endTasks[startingTask.id]!!
    }

    private fun joinTasks(
            newParentTask: Task<*>,
            joinTasks: List<Task<*>>,
            now: ExactTimeStamp,
            removeInstanceKeys: List<InstanceKey>
    ) {
        newParentTask.requireCurrent(now)
        check(joinTasks.size > 1)

        for (joinTask in joinTasks) {
            joinTask.requireCurrent(now)

            if (joinTask.isRootTask(now)) {
                joinTask.getCurrentSchedules(now).forEach { it.setEndExactTimeStamp(now) }
            } else {
                val taskHierarchy = joinTask.getParentTaskHierarchy(now)!!

                taskHierarchy.setEndExactTimeStamp(now)
            }

            newParentTask.addChild(joinTask, now)
        }

        removeInstanceKeys.map(::getInstance)
                .filter { it.getParentInstance(now)?.task != newParentTask && it.isVisible(now, true) }
                .forEach { it.hide(uuid, now) }
    }

    private fun getTasks() = projectsFactory.tasks.asSequence()

    private val customTimes get() = projectsFactory.remoteCustomTimes

    fun getTaskForce(taskKey: TaskKey) = projectsFactory.getTaskForce(taskKey)

    fun getTaskIfPresent(taskKey: TaskKey) = projectsFactory.getTaskIfPresent(taskKey)

    private fun getTaskListChildTaskDatas(
            parentTask: Task<*>,
            now: ExactTimeStamp,
            alwaysShow: Boolean = true,
            hierarchyExactTimeStamp: ExactTimeStamp = now
    ): List<TaskListFragment.ChildTaskData> {
        return parentTask.getChildTaskHierarchies(hierarchyExactTimeStamp)
                .asSequence()
                .sortedBy { it.ordinal }
                .map {
                    val childTask = it.childTask

                    TaskListFragment.ChildTaskData(
                            childTask.name,
                            childTask.getScheduleText(ScheduleText, hierarchyExactTimeStamp),
                            getTaskListChildTaskDatas(childTask, now, alwaysShow, hierarchyExactTimeStamp),
                            childTask.note,
                            childTask.startExactTimeStamp,
                            childTask.taskKey,
                            HierarchyData(it.taskHierarchyKey, it.ordinal),
                            childTask.getImage(deviceDbInfo),
                            childTask.current(now),
                            childTask.hasInstances(now),
                            alwaysShow
                    )
                }
                .toList()
    }

    private fun getExistingInstances() = projectsFactory.existingInstances

    private fun getGroupListChildTaskDatas(
            parentTask: Task<*>,
            now: ExactTimeStamp
    ): List<GroupListFragment.TaskData> = parentTask.getChildTaskHierarchies(now).map {
        val childTask = it.childTask

        GroupListFragment.TaskData(
                childTask.taskKey,
                childTask.name,
                getGroupListChildTaskDatas(childTask, now),
                childTask.startExactTimeStamp,
                childTask.note,
                childTask.getImage(deviceDbInfo))
    }

    private fun setIrrelevant(now: ExactTimeStamp) {
        getTasks().forEach { it.updateOldestVisible(uuid, now) }

        /*
        val instances = remoteProjectFactory.remoteProjects
                .values
                .flatMap { Irrelevant.setIrrelevant(remoteProjectFactory, it, now) }
         */

        val instances = projectsFactory.projects
                .values
                .map { it.existingInstances + it.getRootInstances(null, now.plusOne(), now) }
                .flatten()

        val irrelevantInstanceShownRecords = localFactory.instanceShownRecords
                .toMutableList()
                .apply { removeAll(instances.map { it.getShown(localFactory) }) }
        irrelevantInstanceShownRecords.forEach { it.delete() }
    }

    private fun notifyCloud(project: Project<*>) = notifyCloud(setOf(project))

    private fun notifyCloud(projects: Set<Project<*>>) {
        if (projects.isNotEmpty())
            notifyCloudPrivateFixed(projects.toMutableSet(), mutableListOf())
    }

    private fun notifyCloud(
            project: Project<*>,
            userKeys: Collection<UserKey>
    ) = notifyCloudPrivateFixed(mutableSetOf(project), userKeys.toMutableList())

    private fun notifyCloudPrivateFixed(
            projects: MutableSet<Project<*>>,
            userKeys: MutableCollection<UserKey>
    ) {
        aggregateData?.run {
            notificationProjects.addAll(projects)
            notificationUserKeys.addAll(userKeys)

            return
        }

        val remotePrivateProject = projects.singleOrNull { it is PrivateProject }

        remotePrivateProject?.let {
            projects.remove(it)

            userKeys.add(deviceDbInfo.key)
        }

        BackendNotifier.notify(projects, deviceDbInfo.deviceInfo, userKeys)
    }

    private fun updateNotifications(
            now: ExactTimeStamp,
            clear: Boolean = false,
            silent: Boolean = true,
            removedTaskKeys: List<TaskKey> = listOf(),
            sourceName: String = "other") {
        val skipSave = aggregateData != null

        Preferences.tickLog.logLineDate("updateNotifications start $sourceName, skipping? $skipSave")

        if (skipSave) {
            TickHolder.addTickData(TickData.Normal(silent, sourceName))
            return
        }

        NotificationWrapper.instance.hideTemporary(sourceName)

        val notificationInstances = if (clear)
            mapOf()
        else
            getRootInstances(null, now.plusOne(), now /* 24 hack */).filter {
                it.done == null
                        && !it.getNotified(localFactory)
                        && it.instanceDateTime.timeStamp.toExactTimeStamp() <= now
                        && !removedTaskKeys.contains(it.taskKey)
            }.associateBy { it.instanceKey }

        Preferences.tickLog.logLineHour("notification instances: " + notificationInstances.values.joinToString(", ") { it.name })

        val instanceShownPairs = localFactory.instanceShownRecords
                .filter { it.notificationShown }
                .map { Pair(it, projectsFactory.getProjectIfPresent(it.projectId)?.getTaskIfPresent(it.taskId)) }

        instanceShownPairs.filter { it.second == null }.forEach { (instanceShownRecord, _) ->
            val scheduleDate = Date(instanceShownRecord.scheduleYear, instanceShownRecord.scheduleMonth, instanceShownRecord.scheduleDay)
            val customTimeId = instanceShownRecord.scheduleCustomTimeId

            val customTimePair: Pair<String, String>?
            val hourMinute: HourMinute?
            if (!customTimeId.isNullOrEmpty()) {
                check(instanceShownRecord.scheduleHour == null)
                check(instanceShownRecord.scheduleMinute == null)

                customTimePair = Pair(instanceShownRecord.projectId, customTimeId)
                hourMinute = null
            } else {
                checkNotNull(instanceShownRecord.scheduleHour)
                checkNotNull(instanceShownRecord.scheduleMinute)

                customTimePair = null
                hourMinute = HourMinute(instanceShownRecord.scheduleHour, instanceShownRecord.scheduleMinute)
            }

            val taskKey = Pair(instanceShownRecord.projectId, instanceShownRecord.taskId)

            NotificationWrapper.instance.cancelNotification(Instance.getNotificationId(scheduleDate, customTimePair, hourMinute, taskKey))
            instanceShownRecord.notificationShown = false
        }

        val shownInstanceKeys = instanceShownPairs.filter { it.second != null }.map { (instanceShownRecord, task) ->
            val scheduleDate = Date(instanceShownRecord.scheduleYear, instanceShownRecord.scheduleMonth, instanceShownRecord.scheduleDay)
            val customTimeId = instanceShownRecord.scheduleCustomTimeId
            val project = task!!.project

            val customTimeKey: CustomTimeKey<*>?
            val hourMinute: HourMinute?
            if (!customTimeId.isNullOrEmpty()) {
                check(instanceShownRecord.scheduleHour == null)
                check(instanceShownRecord.scheduleMinute == null)

                customTimeKey = project.getCustomTime(customTimeId).key
                hourMinute = null
            } else {
                checkNotNull(instanceShownRecord.scheduleHour)
                checkNotNull(instanceShownRecord.scheduleMinute)

                customTimeKey = null
                hourMinute = HourMinute(instanceShownRecord.scheduleHour, instanceShownRecord.scheduleMinute)
            }

            val taskKey = TaskKey(project.projectKey, instanceShownRecord.taskId)
            InstanceKey(taskKey, scheduleDate, TimePair(customTimeKey, hourMinute))
        }

        val showInstanceKeys = notificationInstances.keys.filter { !shownInstanceKeys.contains(it) }

        Preferences.tickLog.logLineHour("shown instances: " + shownInstanceKeys.joinToString(", ") { getInstance(it).name })

        val hideInstanceKeys = shownInstanceKeys.filter { !notificationInstances.containsKey(it) }.toSet()

        for (showInstanceKey in showInstanceKeys)
            getInstance(showInstanceKey).setNotificationShown(localFactory, true)

        for (hideInstanceKey in hideInstanceKeys)
            getInstance(hideInstanceKey).setNotificationShown(localFactory, false)

        Preferences.tickLog.logLineHour("silent? $silent")

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
                Preferences.tickLog.logLineHour("hiding group")
                NotificationWrapper.instance.cancelNotification(0)
            } else {
                Preferences.tickLog.logLineHour("showing group")
                NotificationWrapper.instance.notifyGroup(notificationInstances.values, true, now)
            }

            for (hideInstanceKey in hideInstanceKeys) {
                val instance = getInstance(hideInstanceKey)
                Preferences.tickLog.logLineHour("hiding '" + instance.name + "'")
                NotificationWrapper.instance.cancelNotification(instance.notificationId)
            }

            for (showInstanceKey in showInstanceKeys) {
                val instance = notificationInstances.getValue(showInstanceKey)
                Preferences.tickLog.logLineHour("showing '" + instance.name + "'")
                notifyInstance(instance, silent, now)
            }

            val updateInstances = notificationInstances.values.filter { !showInstanceKeys.contains(it.instanceKey) }

            updateInstances.forEach {
                Preferences.tickLog.logLineHour("updating '" + it.name + "' " + it.instanceDateTime)
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
                .flatMap { it.getCurrentSchedules(now).asSequence() }
                .mapNotNull { it.getNextAlarm(now) }
                .min()

        if (minSchedulesTimeStamp != null && (nextAlarm == null || nextAlarm > minSchedulesTimeStamp))
            nextAlarm = minSchedulesTimeStamp

        NotificationWrapper.instance.updateAlarm(nextAlarm)

        if (nextAlarm != null)
            Preferences.tickLog.logLineHour("next tick: $nextAlarm")
    }

    private fun notifyInstance(instance: Instance<*>, silent: Boolean, now: ExactTimeStamp) = NotificationWrapper.instance.notifyInstance(deviceDbInfo, instance, silent, now)

    private fun updateInstance(instance: Instance<*>, now: ExactTimeStamp) = NotificationWrapper.instance.notifyInstance(deviceDbInfo, instance, true, now)

    private fun setInstanceNotified(instanceKey: InstanceKey) {
        getInstance(instanceKey).apply {
            setNotified(localFactory, true)
            setNotificationShown(localFactory, false)
        }
    }

    private fun getGroupListData(timeStamp: TimeStamp, now: ExactTimeStamp): GroupListFragment.DataWrapper {
        val endCalendar = timeStamp.calendar.apply { add(Calendar.MINUTE, 1) }
        val endTimeStamp = TimeStamp(endCalendar.toDateTimeSoy())

        val rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now)

        val currentInstances = rootInstances.filter { it.instanceDateTime.timeStamp.compareTo(timeStamp) == 0 }

        val customTimeDatas = getCurrentRemoteCustomTimes(now).map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes.toSortedMap()) }

        val instanceDatas = currentInstances.map { instance ->
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)

            val instanceData = GroupListFragment.InstanceData(
                    instance.done,
                    instance.instanceKey,
                    null,
                    instance.name,
                    instance.instanceDateTime.timeStamp,
                    task.current(now),
                    instance.isRootInstance(now),
                    isRootTask,
                    instance.exists(),
                    instance.getCreateTaskTimePair(ownerKey),
                    task.note,
                    children,
                    null,
                    instance.ordinal,
                    instance.getNotificationShown(localFactory),
                    task.getImage(deviceDbInfo))

            children.values.forEach { it.instanceDataParent = instanceData }

            instanceData
        }

        val dataWrapper = GroupListFragment.DataWrapper(customTimeDatas, null, listOf(), null, instanceDatas, null)

        instanceDatas.forEach { it.instanceDataParent = dataWrapper }

        return dataWrapper
    }

    private fun getGroupListData(
            instance: Instance<*>,
            task: Task<*>,
            now: ExactTimeStamp
    ): GroupListFragment.DataWrapper {
        val customTimeDatas = getCurrentRemoteCustomTimes(now).map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes.toSortedMap()) }

        val instanceDatas = instance.getChildInstances(now).map { (childInstance, taskHierarchy) ->
            val childTask = childInstance.task

            val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

            val children = getChildInstanceDatas(childInstance, now)

            val instanceData = GroupListFragment.InstanceData(
                    childInstance.done,
                    childInstance.instanceKey,
                    null,
                    childInstance.name,
                    childInstance.instanceDateTime.timeStamp,
                    childTask.current(now),
                    childInstance.isRootInstance(now),
                    isRootTask,
                    childInstance.exists(),
                    childInstance.getCreateTaskTimePair(ownerKey),
                    childTask.note,
                    children,
                    HierarchyData(taskHierarchy.taskHierarchyKey, taskHierarchy.ordinal),
                    childInstance.ordinal,
                    childInstance.getNotificationShown(localFactory),
                    childTask.getImage(deviceDbInfo)
            )

            children.values.forEach { it.instanceDataParent = instanceData }

            instanceData
        }

        val dataWrapper = GroupListFragment.DataWrapper(
                customTimeDatas,
                task.current(now),
                listOf(),
                task.note,
                instanceDatas,
                task.getImage(deviceDbInfo)
        )

        instanceDatas.forEach { it.instanceDataParent = dataWrapper }

        return dataWrapper
    }

    private fun copyTask(now: ExactTimeStamp, task: Task<*>, copyTaskKey: TaskKey) {
        val copiedTask = getTaskForce(copyTaskKey)

        copiedTask.getChildTaskHierarchies(now).forEach {
            val copiedChildTask = it.childTask
            copiedChildTask.getImage(deviceDbInfo)?.let { check(it is ImageState.Remote) }

            createChildTask(now, task, copiedChildTask.name, copiedChildTask.note, copiedChildTask.imageJson, copiedChildTask.taskKey)
        }
    }

    private fun getTime(timePair: TimePair) = timePair.customTimeKey
            ?.let { projectsFactory.getCustomTime(it) }
            ?: Time.Normal(timePair.hourMinute!!)

    class HourUndoData(val instanceDateTimes: Map<InstanceKey, DateTime>)

    class ReadTimes(start: ExactTimeStamp, read: ExactTimeStamp, stop: ExactTimeStamp) {

        val readMillis = read.long - start.long
        val instantiateMillis = stop.long - read.long
    }

    private inner class SavedFactoryException : Exception("private.isSaved == " + projectsFactory.isPrivateSaved + ", shared.isSaved == " + projectsFactory.isSharedSaved + ", user.isSaved == " + remoteUserFactory.isSaved)

    private class AggregateData {

        val notificationProjects = mutableSetOf<Project<*>>()
        val notificationUserKeys = mutableSetOf<UserKey>()
    }
}