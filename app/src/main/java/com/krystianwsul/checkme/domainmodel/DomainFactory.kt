package com.krystianwsul.checkme.domainmodel

import android.os.Build
import androidx.core.content.pm.ShortcutManagerCompat
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.checkError
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
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
import io.reactivex.Observable
import java.util.*

@Suppress("LeakingThis")
class DomainFactory(
    val localFactory: LocalFactory,
    val myUserFactory: MyUserFactory,
    val projectsFactory: ProjectsFactory,
    val friendsFactory: FriendsFactory,
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
            if (domainFactory?.projectsFactory?.isSaved == false && !domainFactory.friendsFactory.isSaved) {
                domainFactory.checkSave()
                firebaseListener(domainFactory)
            } else {
                firebaseListeners.add(Pair(firebaseListener, "other"))
            }
        }

        @Synchronized
        fun addFirebaseListener(source: String, firebaseListener: (DomainFactory) -> Unit) {
            val domainFactory = nullableInstance
            if (domainFactory?.projectsFactory?.isSaved == false && !domainFactory.friendsFactory.isSaved) {
                Preferences.tickLog.logLineHour("running firebaseListener $source")
                firebaseListener(domainFactory)
            } else {
                Preferences.tickLog.logLineHour("queuing firebaseListener $source")
                Preferences.tickLog.logLineHour(
                    "listeners before: " + firebaseListeners.joinToString(
                        "; "
                    ) { it.second })
                firebaseListeners.add(Pair(firebaseListener, source))
                Preferences.tickLog.logLineHour(
                    "listeners after: " + firebaseListeners.joinToString(
                        "; "
                    ) { it.second })
            }
        }

        private val ChangeType.runType
            get() = when (this) {
                ChangeType.LOCAL -> RunType.LOCAL
                ChangeType.REMOTE -> RunType.REMOTE
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

        tryNotifyListeners(
            now,
            "DomainFactory.init",
            if (firstRun) RunType.APP_START else RunType.SIGN_IN
        )

        firstRun = false

        updateShortcuts(now)
    }

    val defaultProjectId by lazy { projectsFactory.privateProject.projectKey }

    // misc

    val taskCount get() = projectsFactory.taskCount
    val instanceCount get() = projectsFactory.instanceCount

    lateinit var instanceInfo: Pair<Int, Int>

    val customTimeCount get() = customTimes.size

    val instanceShownCount get() = localFactory.instanceShownRecords.size

    val uuid get() = localFactory.uuid

    private fun updateIsSaved() {
        val oldSaved = isSaved.value!!
        val newSaved = projectsFactory.isSaved || myUserFactory.isSaved || friendsFactory.isSaved
        isSaved.accept(newSaved)

        if (newSaved || oldSaved) {
            val savedList =
                projectsFactory.savedList + myUserFactory.savedList + friendsFactory.savedList
            val entry = savedList.toMutableList()
                .apply { add(0, "saved managers:") }
                .joinToString("\n")
            Preferences.saveLog.logLineHour(entry, true)
        }
    }

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
        myUserFactory.save(values)
        friendsFactory.save(values)

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

        val maxShortcuts =
            ShortcutManagerCompat.getMaxShortcutCountPerActivity(MyApplication.instance) - 4
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
    override fun onChangeTypeEvent(changeType: ChangeType, now: ExactTimeStamp) {
        MyCrashlytics.log("DomainFactory.onChangeTypeEvent")

        updateShortcuts(now)

        tryNotifyListeners(now, "DomainFactory.onChangeTypeEvent", changeType.runType)
    }

    @Synchronized
    override fun updateUserRecord(snapshot: Snapshot) {
        MyCrashlytics.log("DomainFactory.updateUserRecord")

        val runType = myUserFactory.onNewSnapshot(snapshot).runType

        tryNotifyListeners(ExactTimeStamp.now, "DomainFactory.updateUserRecord", runType)
    }

    private fun tryNotifyListeners(now: ExactTimeStamp, source: String, runType: RunType) {
        MyCrashlytics.log("DomainFactory.tryNotifyListeners $source $runType")

        if (projectsFactory.isSaved || friendsFactory.isSaved || myUserFactory.isSaved)
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
    fun getShowGroupData(timeStamp: TimeStamp): ShowGroupViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowGroupData")

        val now = ExactTimeStamp.now

        val date = timeStamp.date
        val dayOfWeek = date.dayOfWeek
        val hourMinute = timeStamp.hourMinute

        val time =
            getCurrentRemoteCustomTimes(now).firstOrNull { it.getHourMinute(dayOfWeek) == hourMinute }
                ?: Time.Normal(hourMinute)

        val displayText = DateTime(date, time).getDisplayText()

        return ShowGroupViewModel.Data(displayText, getGroupListData(timeStamp, now))
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
            GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
        }

        val instanceDatas = instances.map { instance ->
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)

            val instanceData = GroupListDataWrapper.InstanceData(
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
                instance.task.ordinal,
                instance.getNotificationShown(localFactory),
                task.getImage(deviceDbInfo),
                instance.isRepeatingGroupChild(now)
            )

            children.values.forEach { it.instanceDataParent = instanceData }

            instanceData
        }

        val dataWrapper = GroupListDataWrapper(
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
    fun getDrawerData(): DrawerViewModel.Data {
        MyCrashlytics.log("DomainFactory.getDrawerData")

        return myUserFactory.user.run { DrawerViewModel.Data(name, email, photoUrl) }
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

        val friends = friendsFactory.friends

        val userListDatas = friends.map {
            FriendListViewModel.UserListData(
                it.name,
                it.email,
                it.userKey,
                it.photoUrl,
                it.userWrapper
            )
        }.toMutableSet()

        return FriendListViewModel.Data(userListDatas)
    }

    @Synchronized
    fun getShowProjectData(projectId: ProjectKey.Shared?): ShowProjectViewModel.Data {
        MyCrashlytics.log("DomainFactory.getShowProjectData")

        val friendDatas = friendsFactory.friends
            .map { ShowProjectViewModel.UserListData(it.name, it.email, it.userKey, it.photoUrl) }
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

        return SettingsViewModel.Data(myUserFactory.user.defaultReminder)
    }

    // sets

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

        instance.setInstanceDateTime(
            localFactory,
            ownerKey,
            DateTime(date, Time.Normal(hourMinute)),
            now
        )
        instance.setNotificationShown(localFactory, false)

        updateNotifications(now, sourceName = "setInstanceAddHourService ${instance.name}")

        save(0, source)

        notifyCloud(instance.project)
    }

    @Synchronized
    fun setInstanceNotificationDone(source: SaveService.Source, instanceKey: InstanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotificationDone")
        if (projectsFactory.isSaved) throw SavedFactoryException()

        val instance = getInstance(instanceKey)
        Preferences.tickLog.logLineHour("DomainFactory: setting ${instance.name} done")

        val now = ExactTimeStamp.now

        instance.setDone(localFactory, true, now)
        instance.setNotificationShown(localFactory, false)

        updateNotifications(now, sourceName = "setInstanceNotificationDone ${instance.name}")

        save(0, source)

        notifyCloud(instance.project)
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

    fun <T : ProjectType> createChildTask(
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

    fun setTaskEndTimeStamps(
        source: SaveService.Source,
        taskKeys: Set<TaskKey>,
        deleteInstances: Boolean,
        now: ExactTimeStamp
    ): TaskUndoData {
        check(taskKeys.isNotEmpty())

        val tasks = taskKeys.map { getTaskForce(it) }.toMutableSet()
        tasks.forEach { it.requireCurrent(now) }

        val taskUndoData = TaskUndoData()

        tasks.forEach { it.setEndData(Task.EndData(now, deleteInstances), taskUndoData) }

        val remoteProjects = tasks.map { it.project }.toSet()

        updateNotifications(now)

        save(0, source)

        notifyCloud(remoteProjects)

        return taskUndoData
    }

    fun processTaskUndoData(taskUndoData: TaskUndoData, now: ExactTimeStamp) {
        taskUndoData.taskKeys
            .map { getTaskForce(it) }
            .forEach {
                it.requireNotCurrent(now)

                it.clearEndExactTimeStamp(now)
            }

        taskUndoData.taskHierarchyKeys
            .asSequence()
            .map { projectsFactory.getTaskHierarchy(it) }
            .forEach { it.clearEndExactTimeStamp(now) }

        taskUndoData.scheduleIds
            .asSequence()
            .map { projectsFactory.getSchedule(it) }
            .forEach { it.clearEndExactTimeStamp(now) }
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
        check(!friendsFactory.isSaved)

        keys.forEach { myUserFactory.user.removeFriend(it) }

        save(0, source)
    }

    @Synchronized
    fun addFriend(source: SaveService.Source, userKey: UserKey, userWrapper: UserWrapper) {
        MyCrashlytics.log("DomainFactory.addFriend")
        check(!myUserFactory.isSaved)

        myUserFactory.user.addFriend(userKey)
        friendsFactory.addFriend(userKey, userWrapper)

        save(0, source)
    }

    @Synchronized
    fun addFriends(source: SaveService.Source, userMap: Map<UserKey, UserWrapper>) {
        MyCrashlytics.log("DomainFactory.addFriends")
        check(!myUserFactory.isSaved)

        userMap.forEach {
            myUserFactory.user.addFriend(it.key)
            friendsFactory.addFriend(it.key, it.value)
        }

        save(0, source)
    }

    @Synchronized
    override fun updateDeviceDbInfo(deviceDbInfo: DeviceDbInfo, source: SaveService.Source) {
        MyCrashlytics.log("DomainFactory.updateDeviceDbInfo")
        if (myUserFactory.isSaved || projectsFactory.isSharedSaved) throw SavedFactoryException()

        this.deviceDbInfo = deviceDbInfo

        myUserFactory.user.setToken(deviceDbInfo)
        projectsFactory.updateDeviceInfo(deviceDbInfo)

        save(0, source)
    }

    @Synchronized
    fun updatePhotoUrl(source: SaveService.Source, photoUrl: String) {
        MyCrashlytics.log("DomainFactory.updatePhotoUrl")
        if (myUserFactory.isSaved || projectsFactory.isSharedSaved) throw SavedFactoryException()

        myUserFactory.user.photoUrl = photoUrl
        projectsFactory.updatePhotoUrl(deviceDbInfo.deviceInfo, photoUrl)

        save(0, source)
    }

    @Synchronized
    fun updateDefaultReminder(dataId: Int, source: SaveService.Source, defaultReminder: Boolean) {
        MyCrashlytics.log("DomainFactory.updateDefaultReminder")
        if (myUserFactory.isSaved) throw SavedFactoryException()

        myUserFactory.user.defaultReminder = defaultReminder

        save(dataId, source)
    }

    @Synchronized
    fun updateDefaultTab(source: SaveService.Source, defaultTab: Int) {
        MyCrashlytics.log("DomainFactory.updateDefaultTab")
        if (myUserFactory.isSaved) throw SavedFactoryException()

        myUserFactory.user.defaultTab = defaultTab

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
        remoteProject.updateUsers(
            addedFriends.map { friendsFactory.getFriend(it) }.toSet(),
            removedFriends
        )

        friendsFactory.updateProjects(projectId, addedFriends, removedFriends)

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProject, removedFriends)
    }

    @Synchronized
    fun createProject(
        dataId: Int,
        source: SaveService.Source,
        name: String,
        friends: Set<UserKey>
    ) {
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
            myUserFactory.user,
            deviceDbInfo.userInfo,
            friendsFactory
        )

        myUserFactory.user.addProject(remoteProject.projectKey)
        friendsFactory.updateProjects(remoteProject.projectKey, friends, setOf())

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
            it.setEndExactTimeStamp(now, projectUndoData, removeInstances)
        }

        updateNotifications(now)

        save(dataId, source)

        notifyCloud(remoteProjects)

        return projectUndoData
    }

    @Synchronized
    fun clearProjectEndTimeStamps(
        dataId: Int,
        source: SaveService.Source,
        projectUndoData: ProjectUndoData
    ) {
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

    private fun getExistingInstanceIfPresent(
        taskKey: TaskKey,
        scheduleDateTime: DateTime
    ): Instance<*>? {
        val customTimeKey = scheduleDateTime.time
            .timePair
            .customTimeKey

        val timePair = TimePair(customTimeKey, scheduleDateTime.time.timePair.hourMinute)

        val instanceKey = InstanceKey(taskKey, scheduleDateTime.date, timePair)

        return getExistingInstanceIfPresent(instanceKey)
    }

    private fun getExistingInstanceIfPresent(instanceKey: InstanceKey) =
        projectsFactory.getExistingInstanceIfPresent(instanceKey)

    override fun getSharedCustomTimes(customTimeKey: CustomTimeKey.Private) =
        projectsFactory.sharedProjects
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

    fun getRootInstances(
        startExactTimeStamp: ExactTimeStamp?,
        endExactTimeStamp: ExactTimeStamp,
        now: ExactTimeStamp
    ) = projectsFactory.projects
        .values
        .flatMap { it.getRootInstances(startExactTimeStamp, endExactTimeStamp, now) }

    fun getCurrentRemoteCustomTimes(now: ExactTimeStamp) = projectsFactory.privateProject
        .customTimes
        .filter { it.current(now) }

    fun getChildInstanceDatas(
        instance: Instance<*>,
        now: ExactTimeStamp
    ): MutableMap<InstanceKey, GroupListDataWrapper.InstanceData> {
        return instance.getChildInstances(now)
            .associate { (childInstance, taskHierarchy) ->
                val childTask = childInstance.task

                val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

                val children = getChildInstanceDatas(childInstance, now)

                val instanceData = GroupListDataWrapper.InstanceData(
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
                    taskHierarchy.taskHierarchyKey,
                    childTask.ordinal,
                    childInstance.getNotificationShown(localFactory),
                    childTask.getImage(deviceDbInfo),
                    childInstance.isRepeatingGroupChild(now)
                )

                children.values.forEach { it.instanceDataParent = instanceData }
                childInstance.instanceKey to instanceData
            }
            .toMutableMap()
    }

    private fun getTaskListChildTaskDatas(
        now: ExactTimeStamp,
        parentTask: Task<*>,
        excludedTaskKeys: Set<TaskKey>
    ): Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData> =
        parentTask.getChildTaskHierarchies(now)
            .asSequence()
            .filterNot { excludedTaskKeys.contains(it.childTaskKey) }
            .associate {
                val childTask = it.childTask
                val taskParentKey = EditViewModel.ParentKey.Task(it.childTaskKey)

                val parentTreeData = EditViewModel.ParentTreeData(
                    childTask.name,
                    getTaskListChildTaskDatas(now, childTask, excludedTaskKeys),
                    EditViewModel.ParentKey.Task(childTask.taskKey),
                    childTask.getScheduleText(ScheduleText, now),
                    childTask.note,
                    EditViewModel.SortKey.TaskSortKey(childTask.startExactTimeStamp),
                    (childTask.project as? SharedProject)?.projectKey
                )

                taskParentKey to parentTreeData
            }

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

        if (excludedTaskKeys.contains(taskKey))
            return false

        if (includedTaskKeys.contains(taskKey)) { // todo this doesn't account for a parent that isn't a root instance
            check(isVisible(now, true))

            return true
        }

        if (!isVisible(now, false))
            return false

        return true
    }

    fun getParentTreeDatas(
        now: ExactTimeStamp,
        excludedTaskKeys: Set<TaskKey>,
        includedTaskKeys: Set<TaskKey>
    ): Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData> {
        val parentTreeDatas = mutableMapOf<EditViewModel.ParentKey, EditViewModel.ParentTreeData>()

        parentTreeDatas += projectsFactory.privateProject
            .tasks
            .asSequence()
            .filter { it.showAsParent(now, excludedTaskKeys, includedTaskKeys) }
            .associate {
                val taskParentKey = EditViewModel.ParentKey.Task(it.taskKey)
                val parentTreeData = EditViewModel.ParentTreeData(
                    it.name,
                    getTaskListChildTaskDatas(now, it, excludedTaskKeys),
                    taskParentKey,
                    it.getScheduleText(ScheduleText, now),
                    it.note,
                    EditViewModel.SortKey.TaskSortKey(it.startExactTimeStamp),
                    null
                )

                taskParentKey to parentTreeData
            }

        parentTreeDatas += projectsFactory.sharedProjects
            .values
            .asSequence()
            .filter { it.current(now) }
            .associate {
                val projectParentKey = EditViewModel.ParentKey.Project(it.projectKey)

                val users = it.users.joinToString(", ") { it.name }
                val parentTreeData = EditViewModel.ParentTreeData(
                    it.name,
                    getProjectTaskTreeDatas(now, it, excludedTaskKeys, includedTaskKeys),
                    projectParentKey,
                    users,
                    null,
                    EditViewModel.SortKey.ProjectSortKey(it.projectKey),
                    it.projectKey
                )

                projectParentKey to parentTreeData
            }

        return parentTreeDatas
    }

    private fun getProjectTaskTreeDatas(
        now: ExactTimeStamp,
        project: Project<*>,
        excludedTaskKeys: Set<TaskKey>,
        includedTaskKeys: Set<TaskKey>
    ): Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData> {
        return project.tasks
            .asSequence()
            .filter { it.showAsParent(now, excludedTaskKeys, includedTaskKeys) }
            .associate {
                val taskParentKey = EditViewModel.ParentKey.Task(it.taskKey)
                val parentTreeData = EditViewModel.ParentTreeData(
                    it.name,
                    getTaskListChildTaskDatas(now, it, excludedTaskKeys),
                    taskParentKey,
                    it.getScheduleText(ScheduleText, now),
                    it.note,
                    EditViewModel.SortKey.TaskSortKey(it.startExactTimeStamp),
                    (it.project as? SharedProject)?.projectKey
                )

                taskParentKey to parentTreeData
            }
    }

    val ownerKey get() = myUserFactory.user.userKey

    override fun <T : ProjectType> convert(
        now: ExactTimeStamp,
        startingTask: Task<T>,
        projectId: ProjectKey<*>
    ): Task<*> {
        val remoteToRemoteConversion = RemoteToRemoteConversion<T>()
        val startProject = startingTask.project
        startProject.convertRemoteToRemoteHelper(now, remoteToRemoteConversion, startingTask)

        val newProject = projectsFactory.getProjectForce(projectId)

        for (pair in remoteToRemoteConversion.startTasks.values) {
            val task = newProject.copyTask(deviceDbInfo, pair.first, pair.second, now)
            remoteToRemoteConversion.endTasks[pair.first.id] = task
        }

        for (startTaskHierarchy in remoteToRemoteConversion.startTaskHierarchies) {
            val parentTask =
                remoteToRemoteConversion.endTasks.getValue(startTaskHierarchy.parentTaskId)
            val childTask =
                remoteToRemoteConversion.endTasks.getValue(startTaskHierarchy.childTaskId)

            val taskHierarchy = newProject.copyTaskHierarchy(
                now,
                startTaskHierarchy,
                parentTask.id,
                childTask.id
            )

            remoteToRemoteConversion.endTaskHierarchies.add(taskHierarchy)
        }

        val endData = Task.EndData(now, true)

        for (pair in remoteToRemoteConversion.startTasks.values) {
            pair.second.forEach {
                if (!it.hidden)
                    it.hide(now)
            }

            // I think this might no longer be necessary, since setEndData doesn't recurse on children
            if (pair.first.getEndData() != null)
                check(pair.first.getEndData() == endData)
            else
                pair.first.setEndData(endData)
        }

        return remoteToRemoteConversion.endTasks.getValue(startingTask.id)
    }

    fun joinTasks(
        newParentTask: Task<*>,
        joinTasks: List<Task<*>>,
        now: ExactTimeStamp,
        removeInstanceKeys: List<InstanceKey>,
        allReminders: Boolean = true
    ) {
        newParentTask.requireCurrent(now)
        check(joinTasks.size > 1)

        for (joinTask in joinTasks) {
            joinTask.requireCurrent(now)

            if (allReminders) {
                joinTask.endAllCurrentTaskHierarchies(now)
                joinTask.endAllCurrentSchedules(now)
                joinTask.endAllCurrentNoScheduleOrParents(now)
            }

            newParentTask.addChild(joinTask, now)
        }

        removeInstanceKeys.map(::getInstance)
            .filter {
                it.getParentInstance(now)?.first?.task != newParentTask && it.isVisible(
                    now,
                    true
                )
            }
            .forEach { it.hide(now) }
    }

    fun getTasks() = projectsFactory.tasks.asSequence()

    private val customTimes get() = projectsFactory.remoteCustomTimes

    fun getTaskForce(taskKey: TaskKey) = projectsFactory.getTaskForce(taskKey)

    fun getTaskIfPresent(taskKey: TaskKey) = projectsFactory.getTaskIfPresent(taskKey)

    fun getTaskListChildTaskDatas(
        parentTask: Task<*>,
        now: ExactTimeStamp,
        alwaysShow: Boolean = true,
        hierarchyExactTimeStamp: ExactTimeStamp = now,
        groups: Boolean = false
    ): List<TaskListFragment.ChildTaskData> {
        return parentTask.getChildTaskHierarchies(hierarchyExactTimeStamp, groups)
            .map { taskHierarchy ->
                val childTask = taskHierarchy.childTask

                TaskListFragment.ChildTaskData(
                    childTask.name,
                    childTask.getScheduleText(ScheduleText, hierarchyExactTimeStamp),
                    getTaskListChildTaskDatas(
                        childTask,
                        now,
                        alwaysShow,
                        hierarchyExactTimeStamp,
                        groups
                    ),
                    childTask.note,
                    childTask.startExactTimeStamp,
                    childTask.taskKey,
                    taskHierarchy.taskHierarchyKey,
                    childTask.getImage(deviceDbInfo),
                    childTask.current(now),
                    alwaysShow,
                    childTask.ordinal
                )
            }
    }

    private fun getExistingInstances() = projectsFactory.existingInstances

    fun getGroupListChildTaskDatas(
        parentTask: Task<*>,
        now: ExactTimeStamp
    ): List<GroupListDataWrapper.TaskData> = parentTask.getChildTaskHierarchies(now).map {
        val childTask = it.childTask

        GroupListDataWrapper.TaskData(
            childTask.taskKey,
            childTask.name,
            getGroupListChildTaskDatas(childTask, now),
            childTask.startExactTimeStamp,
            childTask.note,
            childTask.getImage(deviceDbInfo)
        )
    }

    private fun setIrrelevant(now: ExactTimeStamp) {
        if (false) {
            Irrelevant.setIrrelevant(
                object : Project.Parent {

                    override fun deleteProject(project: Project<*>) {
                        TODO("Not yet implemented")
                    }
                },
                projectsFactory.privateProject,
                now
            )

            throw Exception()
        }

        val instances = projectsFactory.projects
            .values
            .map { it.existingInstances + it.getRootInstances(null, now.plusOne(), now) }
            .flatten()

        val irrelevantInstanceShownRecords = localFactory.instanceShownRecords
            .toMutableList()
            .apply { removeAll(instances.map { it.getShown(localFactory) }) }
        irrelevantInstanceShownRecords.forEach { it.delete() }
    }

    fun notifyCloud(project: Project<*>) = notifyCloud(setOf(project))

    fun notifyCloud(projects: Set<Project<*>>) {
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

    fun updateNotifications(
        now: ExactTimeStamp,
        clear: Boolean = false,
        silent: Boolean = true,
        removedTaskKeys: List<TaskKey> = listOf(),
        sourceName: String = "other"
    ) {
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

        Preferences.tickLog.logLineHour(
            "notification instances: " + notificationInstances.values.joinToString(
                ", "
            ) { it.name })

        val instanceShownPairs = localFactory.instanceShownRecords
            .filter { it.notificationShown }
            .map {
                Pair(
                    it,
                    projectsFactory.getProjectIfPresent(it.projectId)?.getTaskIfPresent(it.taskId)
                )
            }

        instanceShownPairs.filter { it.second == null }.forEach { (instanceShownRecord, _) ->
            val scheduleDate = Date(
                instanceShownRecord.scheduleYear,
                instanceShownRecord.scheduleMonth,
                instanceShownRecord.scheduleDay
            )
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
                hourMinute =
                    HourMinute(instanceShownRecord.scheduleHour, instanceShownRecord.scheduleMinute)
            }

            val taskKey = Pair(instanceShownRecord.projectId, instanceShownRecord.taskId)

            NotificationWrapper.instance.cancelNotification(
                Instance.getNotificationId(
                    scheduleDate,
                    customTimePair,
                    hourMinute,
                    taskKey
                )
            )
            instanceShownRecord.notificationShown = false
        }

        val shownInstanceKeys =
            instanceShownPairs.filter { it.second != null }.map { (instanceShownRecord, task) ->
                val scheduleDate = Date(
                    instanceShownRecord.scheduleYear,
                    instanceShownRecord.scheduleMonth,
                    instanceShownRecord.scheduleDay
                )
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
                    hourMinute = HourMinute(
                        instanceShownRecord.scheduleHour,
                        instanceShownRecord.scheduleMinute
                    )
                }

                val taskKey = TaskKey(project.projectKey, instanceShownRecord.taskId)
                InstanceKey(taskKey, scheduleDate, TimePair(customTimeKey, hourMinute))
            }

        val showInstanceKeys = notificationInstances.keys.filter { !shownInstanceKeys.contains(it) }

        Preferences.tickLog.logLineHour("shown instances: " + shownInstanceKeys.joinToString(", ") {
            getInstance(
                it
            ).name
        })

        val hideInstanceKeys =
            shownInstanceKeys.filter { !notificationInstances.containsKey(it) }.toSet()

        for (showInstanceKey in showInstanceKeys)
            getInstance(showInstanceKey).setNotificationShown(localFactory, true)

        for (hideInstanceKey in hideInstanceKeys)
            getInstance(hideInstanceKey).setNotificationShown(localFactory, false)

        Preferences.tickLog.logLineHour("silent? $silent")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (notificationInstances.size > TickJobIntentService.MAX_NOTIFICATIONS) { // show group
                if (shownInstanceKeys.size > TickJobIntentService.MAX_NOTIFICATIONS) { // group shown
                    val silentParam =
                        if (showInstanceKeys.isNotEmpty() || hideInstanceKeys.isNotEmpty()) silent else true

                    NotificationWrapper.instance.notifyGroup(
                        notificationInstances.values,
                        silentParam,
                        now
                    )
                } else { // instances shown
                    for (shownInstanceKey in shownInstanceKeys)
                        NotificationWrapper.instance.cancelNotification(getInstance(shownInstanceKey).notificationId)

                    NotificationWrapper.instance.notifyGroup(
                        notificationInstances.values,
                        silent,
                        now
                    )
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

            val updateInstances =
                notificationInstances.values.filter { !showInstanceKeys.contains(it.instanceKey) }

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

    private fun notifyInstance(instance: Instance<*>, silent: Boolean, now: ExactTimeStamp) =
        NotificationWrapper.instance.notifyInstance(deviceDbInfo, instance, silent, now)

    private fun updateInstance(instance: Instance<*>, now: ExactTimeStamp) =
        NotificationWrapper.instance.notifyInstance(deviceDbInfo, instance, true, now)

    private fun setInstanceNotified(instanceKey: InstanceKey) {
        getInstance(instanceKey).apply {
            setNotified(localFactory, true)
            setNotificationShown(localFactory, false)
        }
    }

    private fun getGroupListData(timeStamp: TimeStamp, now: ExactTimeStamp): GroupListDataWrapper {
        val endCalendar = timeStamp.calendar.apply { add(Calendar.MINUTE, 1) }
        val endTimeStamp = TimeStamp(endCalendar.toDateTimeSoy())

        val rootInstances =
            getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now)

        val currentInstances =
            rootInstances.filter { it.instanceDateTime.timeStamp.compareTo(timeStamp) == 0 }

        val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
            GroupListDataWrapper.CustomTimeData(
                it.name,
                it.hourMinutes.toSortedMap()
            )
        }

        val instanceDatas = currentInstances.map { instance ->
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)

            val instanceData = GroupListDataWrapper.InstanceData(
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
                task.ordinal,
                instance.getNotificationShown(localFactory),
                task.getImage(deviceDbInfo),
                instance.isRepeatingGroupChild(now)
            )

            children.values.forEach { it.instanceDataParent = instanceData }

            instanceData
        }

        val dataWrapper =
            GroupListDataWrapper(customTimeDatas, null, listOf(), null, instanceDatas, null)

        instanceDatas.forEach { it.instanceDataParent = dataWrapper }

        return dataWrapper
    }

    fun getGroupListData(
        instance: Instance<*>,
        task: Task<*>,
        now: ExactTimeStamp
    ): GroupListDataWrapper {
        val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
            GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
        }

        val instanceDatas = instance.getChildInstances(now).map { (childInstance, taskHierarchy) ->
            val childTask = childInstance.task

            val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

            val children = getChildInstanceDatas(childInstance, now)

            val instanceData = GroupListDataWrapper.InstanceData(
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
                taskHierarchy.taskHierarchyKey,
                childTask.ordinal,
                childInstance.getNotificationShown(localFactory),
                childTask.getImage(deviceDbInfo),
                childInstance.isRepeatingGroupChild(now)
            )

            children.values.forEach { it.instanceDataParent = instanceData }

            instanceData
        }

        val dataWrapper = GroupListDataWrapper(
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

    fun copyTask(now: ExactTimeStamp, task: Task<*>, copyTaskKey: TaskKey) {
        val copiedTask = getTaskForce(copyTaskKey)

        copiedTask.getChildTaskHierarchies(now).forEach {
            val copiedChildTask = it.childTask
            copiedChildTask.getImage(deviceDbInfo)?.let { check(it is ImageState.Remote) }

            createChildTask(
                now,
                task,
                copiedChildTask.name,
                copiedChildTask.note,
                copiedChildTask.imageJson,
                copiedChildTask.taskKey
            )
        }
    }

    fun getTime(timePair: TimePair) = timePair.customTimeKey
        ?.let { projectsFactory.getCustomTime(it) }
        ?: Time.Normal(timePair.hourMinute!!)

    class HourUndoData(val instanceDateTimes: Map<InstanceKey, DateTime>)

    class ReadTimes(start: ExactTimeStamp, read: ExactTimeStamp, stop: ExactTimeStamp) {

        val readMillis = read.long - start.long
        val instantiateMillis = stop.long - read.long
    }

    inner class SavedFactoryException :
        Exception("private.isSaved == " + projectsFactory.isPrivateSaved + ", shared.isSaved == " + projectsFactory.isSharedSaved + ", user.isSaved == " + myUserFactory.isSaved)

    private class AggregateData {

        val notificationProjects = mutableSetOf<Project<*>>()
        val notificationUserKeys = mutableSetOf<UserKey>()
    }
}