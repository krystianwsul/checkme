package com.krystianwsul.checkme.domainmodel

import android.os.Build
import android.util.Log
import androidx.annotation.CheckResult
import androidx.core.content.pm.ShortcutManagerCompat
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager.NotificationType
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.ticks.Ticker
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.RemoteToRemoteConversion
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.SchedulerType
import com.krystianwsul.common.firebase.SchedulerTypeHolder
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*
import com.soywiz.klock.days
import com.soywiz.klock.hours
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates.observable

@Suppress("LeakingThis")
class DomainFactory(
        val localFactory: LocalFactory,
        val myUserFactory: MyUserFactory,
        val projectsFactory: ProjectsFactory,
        val friendsFactory: FriendsFactory,
        _deviceDbInfo: DeviceDbInfo,
        startTime: ExactTimeStamp.Local,
        readTime: ExactTimeStamp.Local,
        domainDisposable: CompositeDisposable,
) : PrivateCustomTime.AllRecordsSource, Task.ProjectUpdater, FactoryProvider.Domain {

    companion object {

        private const val MAX_NOTIFICATIONS = 3

        val instanceRelay = BehaviorRelay.createDefault(NullableWrapper<DomainFactory>())!!

        val nullableInstance get() = instanceRelay.value!!.value

        val instance get() = nullableInstance!!

        private val firebaseListeners = mutableListOf<Pair<(DomainFactory) -> Unit, String>>()

        var firstRun = false

        val isSaved = instanceRelay.switchMap { it.value?.isSaved ?: Observable.just(false) }
                .distinctUntilChanged()
                .replay(1)!!
                .apply { connect() }

        @CheckResult
        fun setFirebaseTickListener(source: SaveService.Source, newTickData: TickData) = completeOnDomain {
            check(MyApplication.instance.hasUserInfo)

            val domainFactory = nullableInstance

            if (domainFactory?.projectsFactory?.isSaved != false) {
                TickHolder.addTickData(newTickData)
            } else {
                val tickData = TickHolder.getTickData()

                val silent = (tickData?.silent ?: true) && newTickData.silent
                val notifyListeners = (tickData?.domainChanged ?: false) || newTickData.domainChanged

                if (notifyListeners) domainFactory.domainListenerManager.notify(NotificationType.All)

                domainFactory.updateNotificationsTick(source, silent, newTickData.source)

                if (tickData?.waiting == true) {
                    TickHolder.addTickData(newTickData)
                } else {
                    tickData?.release()
                    newTickData.release()
                }
            }
        }

        // todo scheduler change to single that emits when firebase is ready
        // todo route all external calls through here
        @CheckResult
        fun addFirebaseListener(firebaseListener: (DomainFactory) -> Unit) = completeOnDomain {
            val domainFactory = nullableInstance
            if (
                    domainFactory?.projectsFactory?.isSaved == false
                    && !domainFactory.friendsFactory.isSaved
                    && !domainFactory.myUserFactory.isSaved
            ) {
                domainFactory.throwIfSaved()
                firebaseListener(domainFactory)
            } else {
                firebaseListeners.add(Pair(firebaseListener, "other"))
            }
        }

        fun addFirebaseListener(source: String, firebaseListener: (DomainFactory) -> Unit) = runOnDomain {
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

        fun <T> syncOnDomain(action: () -> T): T {
            SchedulerTypeHolder.instance.requireScheduler()

            return DomainLocker.syncOnDomain(action)
        }

        @CheckResult
        fun <T : Any> scheduleOnDomain(action: () -> T) =
                domainCompletable().andThen(Single.fromCallable(action)).observeOn(AndroidSchedulers.mainThread())!!
    }

    var remoteReadTimes: ReadTimes
        private set

    private var aggregateData: AggregateData? = null

    val domainListenerManager = DomainListenerManager()

    var deviceDbInfo = _deviceDbInfo
        private set

    val isSaved = BehaviorRelay.createDefault(false)!!

    private val changeTypeRelay = PublishRelay.create<ChangeType>()

    init {
        Preferences.tickLog.logLineHour("DomainFactory.init")

        val now = ExactTimeStamp.Local.now

        remoteReadTimes = ReadTimes(startTime, readTime, now)

        projectsFactory.privateProject.let {
            if (it.run { !defaultTimesCreated && customTimes.isEmpty() }) {
                DefaultCustomTimeCreator.createDefaultCustomTimes(it)

                it.defaultTimesCreated = true
            }
        }

        tryNotifyListeners(
                now,
                "DomainFactory.init",
                if (firstRun) RunType.APP_START else RunType.SIGN_IN,
        )

        firstRun = false

        updateShortcuts(now)

        listOf(
                changeTypeRelay.filter { it == ChangeType.REMOTE }
                        .firstOrError()
                        .map { "remote change" },
                Single.just(Unit)
                        .delay(1, TimeUnit.MINUTES)
                        .observeOnDomain()
                        .map { "timeout" }
        ).map { it.toObservable() }
                .merge()
                .firstOrError()
                .flatMapCompletable { source -> addFirebaseListener { it.fixOffsets(source) } }
                .subscribe()
                .addTo(domainDisposable)
    }

    private fun fixOffsets(source: String) {
        MyCrashlytics.log("triggering fixing offsets from $source")

        SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

        if (projectsFactory.isSaved) throw SavedFactoryException()

        projectsFactory.projects
                .values
                .forEach { it.fixOffsets() }

        save(null, SaveService.Source.SERVICE)
    }

    val defaultProjectId by lazy { projectsFactory.privateProject.projectKey }

    // misc

    val taskCount get() = projectsFactory.taskCount
    val instanceCount get() = projectsFactory.instanceCount

    lateinit var instanceInfo: Pair<Int, Int>

    val customTimeCount get() = customTimes.size

    val instanceShownCount get() = localFactory.instanceShownRecords.size

    val uuid get() = localFactory.uuid

    var debugMode by observable(false) { _, _, _ -> domainListenerManager.notify(NotificationType.All) }

    val copiedTaskKeys = mutableMapOf<TaskKey, TaskKey>()

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
            dataId: Int?,
            source: SaveService.Source,
            forceDomainChanged: Boolean = false,
            values: MutableMap<String, Any?> = mutableMapOf(),
    ) = save(
            dataId?.let(NotificationType::Skip) ?: NotificationType.All,
            source,
            forceDomainChanged,
            values
    )

    fun save(
            notificationType: NotificationType,
            source: SaveService.Source,
            forceDomainChanged: Boolean = false,
            values: MutableMap<String, Any?> = mutableMapOf(),
    ) {
        SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

        val skipping = aggregateData != null
        Preferences.tickLog.logLineHour("DomainFactory.save: skipping? $skipping")

        if (skipping) {
            check(
                    notificationType is NotificationType.All ||
                            (notificationType is NotificationType.Skip && notificationType.dataId == 0)
            )

            return
        }

        val localChanges = localFactory.save(source)
        projectsFactory.save(values)
        myUserFactory.save(values)
        friendsFactory.save(values)

        val changes = localChanges || values.isNotEmpty()

        if (values.isNotEmpty())
            AndroidDatabaseWrapper.update(values, checkError(this, "DomainFactory.save", values))

        if (changes || forceDomainChanged) domainListenerManager.notify(notificationType)

        if (changes) updateIsSaved()
    }

    private fun updateShortcuts(now: ExactTimeStamp.Local) {
        ImageManager.prefetch(deviceDbInfo, getTasks().toList()) { updateNotifications(ExactTimeStamp.Local.now) }

        val shortcutTasks = ShortcutManager.getShortcuts()
                .map { Pair(it.value, getTaskIfPresent(it.key)) }
                .filter { it.second?.isVisible(now) == true }
                .map { Pair(it.first, it.second!!) }

        ShortcutManager.keepShortcuts(shortcutTasks.map { it.second.taskKey })

        val maxShortcuts =
                ShortcutManagerCompat.getMaxShortcutCountPerActivity(MyApplication.context) - 4

        if (maxShortcuts <= 0) return

        val shortcutDatas = shortcutTasks.sortedBy { it.first }
                .takeLast(maxShortcuts)
                .map { ShortcutQueue.ShortcutData(deviceDbInfo, it.second) }

        ShortcutQueue.updateShortcuts(shortcutDatas)
    }

    // firebase

    override fun clearUserInfo() {
        SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

        updateNotifications(ExactTimeStamp.Local.now, true)
    }

    override fun onChangeTypeEvent(changeType: ChangeType, now: ExactTimeStamp.Local) {
        MyCrashlytics.log("DomainFactory.onChangeTypeEvent")

        SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

        updateShortcuts(now)

        tryNotifyListeners(now, "DomainFactory.onChangeTypeEvent", changeType.runType)

        changeTypeRelay.accept(changeType)
    }

    override fun updateUserRecord(snapshot: Snapshot) {
        MyCrashlytics.log("DomainFactory.updateUserRecord")

        SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

        val runType = myUserFactory.onNewSnapshot(snapshot).runType

        tryNotifyListeners(ExactTimeStamp.Local.now, "DomainFactory.updateUserRecord", runType)
    }

    private fun tryNotifyListeners(now: ExactTimeStamp.Local, source: String, runType: RunType) {
        MyCrashlytics.log("DomainFactory.tryNotifyListeners $source $runType")

        if (projectsFactory.isSaved || friendsFactory.isSaved || myUserFactory.isSaved) return

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
            updateNotificationsTick(
                    now,
                    tickData.silent && !forceNotify,
                    "${tickData.source}, runType: $runType"
            )

            if (!tickData.waiting) tickData.release()
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

    fun throwIfSaved() {
        SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

        if (projectsFactory.isSaved) throw SavedFactoryException()
    }

    // sets

    fun setTaskEndTimeStamps(
            source: SaveService.Source,
            taskKeys: Set<TaskKey>,
            deleteInstances: Boolean,
            now: ExactTimeStamp.Local,
    ): TaskUndoData {
        check(taskKeys.isNotEmpty())

        fun Task<*>.getAllChildren(): List<Task<*>> = listOf(this) + getChildTaskHierarchies(now).map {
            it.childTask.getAllChildren()
        }.flatten()

        val tasks = taskKeys.map { getTaskForce(it).getAllChildren() }
                .flatten()
                .toSet()

        tasks.forEach { it.requireCurrent(now) }

        val taskUndoData = TaskUndoData()

        tasks.forEach { it.setEndData(Task.EndData(now, deleteInstances), taskUndoData) }

        val remoteProjects = tasks.map { it.project }.toSet()

        updateNotifications(now)

        save(0, source)

        notifyCloud(remoteProjects)

        return taskUndoData
    }

    fun processTaskUndoData(taskUndoData: TaskUndoData, now: ExactTimeStamp.Local) {
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

    private fun updateNotificationsTick(
            source: SaveService.Source,
            silent: Boolean,
            sourceName: String,
            domainChanged: Boolean = false,
    ) {
        MyCrashlytics.log("DomainFactory.updateNotificationsTick source: $sourceName")

        SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.Local.now

        updateNotificationsTick(now, silent, sourceName, domainChanged)

        save(0, source)
    }

    private fun updateNotificationsTick(
            now: ExactTimeStamp.Local,
            silent: Boolean,
            sourceName: String,
            domainChanged: Boolean = false,
    ) {
        updateNotifications(now, silent = silent, sourceName = sourceName, domainChanged = domainChanged)

        setIrrelevant(now)

        projectsFactory.let { localFactory.deleteInstanceShownRecords(it.taskKeys) }
    }

    override fun updateDeviceDbInfo(deviceDbInfo: DeviceDbInfo, source: SaveService.Source) {
        MyCrashlytics.log("DomainFactory.updateDeviceDbInfo")

        SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

        if (myUserFactory.isSaved || projectsFactory.isSharedSaved) throw SavedFactoryException()

        this.deviceDbInfo = deviceDbInfo

        myUserFactory.user.apply {
            name = deviceDbInfo.name
            setToken(deviceDbInfo)
        }

        projectsFactory.updateDeviceInfo(deviceDbInfo)

        save(0, source)
    }

    // internal

    override fun getSharedCustomTimes(customTimeKey: CustomTimeKey.Private) =
            projectsFactory.sharedProjects
                    .values
                    .mapNotNull { it.getSharedTimeIfPresent(customTimeKey, ownerKey) }

    fun getInstance(instanceKey: InstanceKey) =
            projectsFactory.getTaskForce(instanceKey.taskKey).getInstance(instanceKey.scheduleKey)

    fun getRootInstances(
            startExactTimeStamp: ExactTimeStamp.Offset?,
            endExactTimeStamp: ExactTimeStamp.Offset?,
            now: ExactTimeStamp.Local,
            searchCriteria: SearchCriteria? = null,
            filterVisible: Boolean = true,
            projectKey: ProjectKey<*>? = null,
    ): Sequence<Instance<*>> {
        val searchData = searchCriteria?.let { Project.SearchData(it, myUserFactory.user) }

        val projects =
                projectKey?.let { listOf(projectsFactory.getProjectForce(it)) } ?: projectsFactory.projects.values

        val instanceSequences = projects.map {
            it.getRootInstances(startExactTimeStamp, endExactTimeStamp, now, searchData, filterVisible)
        }

        return combineInstanceSequences(instanceSequences)
    }

    fun getCurrentRemoteCustomTimes(now: ExactTimeStamp.Local) = projectsFactory.privateProject
            .customTimes
            .filter { it.current(now) }

    fun instanceToGroupListData(
            instance: Instance<*>,
            now: ExactTimeStamp.Local,
            children: MutableMap<InstanceKey, GroupListDataWrapper.InstanceData>,
    ): GroupListDataWrapper.InstanceData {
        val isRootInstance = instance.isRootInstance()

        return GroupListDataWrapper.InstanceData(
                instance.done,
                instance.instanceKey,
                if (isRootInstance) instance.instanceDateTime.getDisplayText() else null,
                instance.name,
                instance.instanceDateTime.timeStamp,
                instance.instanceDateTime,
                instance.task.current(now),
                instance.canAddSubtask(now),
                instance.isRootInstance(),
                instance.getCreateTaskTimePair(ownerKey),
                instance.task.note,
                children,
                instance.task.ordinal,
                instance.getNotificationShown(localFactory),
                instance.task.getImage(deviceDbInfo),
                instance.isAssignedToMe(now, myUserFactory.user),
                instance.getProjectInfo(now),
        )
    }

    fun <T> getChildInstanceDatas(
            instance: Instance<*>,
            now: ExactTimeStamp.Local,
            mapper: (Instance<*>, ExactTimeStamp.Local, MutableMap<InstanceKey, T>) -> T,
            searchCriteria: SearchCriteria = SearchCriteria.empty,
            filterVisible: Boolean = true,
    ): MutableMap<InstanceKey, T> {
        return instance.getChildInstances()
                .asSequence()
                .filter {
                    !filterVisible || it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true))
                }
                .filterSearchCriteria(searchCriteria, now, myUserFactory.user)
                .mapNotNull { childInstance ->
                    val childTask = childInstance.task

                    val childTaskMatches = childTask.matchesQuery(searchCriteria.query)

                    /*
                    We know this instance matches SearchCriteria.showAssignedToOthers.  If it also matches the query, we
                    can skip filtering child instances, since showAssignedToOthers is meaningless for child instances.
                     */
                    val childrenQuery = if (childTaskMatches) searchCriteria.copy(query = "") else searchCriteria

                    val children = getChildInstanceDatas(childInstance, now, mapper, childrenQuery, filterVisible)

                    if (childTaskMatches || children.isNotEmpty())
                        childInstance.instanceKey to mapper(childInstance, now, children)
                    else
                        null
                }
                .toMap()
                .toMutableMap()
    }

    fun getChildInstanceDatas(
            instance: Instance<*>,
            now: ExactTimeStamp.Local,
            searchCriteria: SearchCriteria = SearchCriteria.empty,
            filterVisible: Boolean = true,
    ): MutableMap<InstanceKey, GroupListDataWrapper.InstanceData> =
            getChildInstanceDatas(instance, now, ::instanceToGroupListData, searchCriteria, filterVisible)

    val ownerKey get() = myUserFactory.user.userKey

    override fun <T : ProjectType> convert(
            now: ExactTimeStamp.Local,
            startingTask: Task<T>,
            projectId: ProjectKey<*>,
    ): Task<*> {
        val remoteToRemoteConversion = RemoteToRemoteConversion<T>()
        val startProject = startingTask.project
        startProject.convertRemoteToRemoteHelper(now, remoteToRemoteConversion, startingTask)

        val newProject = projectsFactory.getProjectForce(projectId)

        val allUpdaters = mutableListOf<(Map<String, String>) -> Any?>()

        for (pair in remoteToRemoteConversion.startTasks.values) {
            val (task, updaters) = newProject.copyTask(
                    deviceDbInfo,
                    pair.first,
                    pair.second,
                    now,
                    newProject.projectKey
            )

            remoteToRemoteConversion.endTasks[pair.first.id] = task
            remoteToRemoteConversion.copiedTaskKeys[pair.first.taskKey] = task.taskKey
            allUpdaters += updaters
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
            pair.second.forEach { if (!it.hidden) it.hide() }

            // I think this might no longer be necessary, since setEndData doesn't recurse on children
            if (pair.first.endData != null)
                check(pair.first.endData == endData)
            else
                pair.first.setEndData(endData)
        }

        val taskKeyMap = remoteToRemoteConversion.endTasks.mapValues { it.value.taskKey.taskId }

        allUpdaters.forEach { it(taskKeyMap) }

        remoteToRemoteConversion.endTasks.forEach {
            it.value
                    .existingInstances
                    .values
                    .forEach { it.addToParentInstanceHierarchyContainer() }
        }

        copiedTaskKeys.putAll(remoteToRemoteConversion.copiedTaskKeys)

        return remoteToRemoteConversion.endTasks.getValue(startingTask.id)
    }

    fun getTasks() = projectsFactory.tasks.asSequence()

    private val customTimes get() = projectsFactory.remoteCustomTimes

    fun getTaskForce(taskKey: TaskKey) = projectsFactory.getTaskForce(taskKey)

    fun getTaskIfPresent(taskKey: TaskKey) = projectsFactory.getTaskIfPresent(taskKey)

    fun getTaskListChildTaskDatas(
            parentTask: Task<*>,
            now: ExactTimeStamp.Local,
            parentHierarchyExactTimeStamp: ExactTimeStamp,
    ): List<TaskListFragment.ChildTaskData> {
        return parentTask.getChildTaskHierarchies(parentHierarchyExactTimeStamp, true)
                .map { taskHierarchy ->
                    val childTask = taskHierarchy.childTask

                    val childHierarchyExactTimeStamp =
                            childTask.getHierarchyExactTimeStamp(parentHierarchyExactTimeStamp)

                    TaskListFragment.ChildTaskData(
                            childTask.name,
                            childTask.getScheduleText(ScheduleText, childHierarchyExactTimeStamp),
                            getTaskListChildTaskDatas(
                                    childTask,
                                    now,
                                    childHierarchyExactTimeStamp,
                            ),
                            childTask.note,
                            childTask.taskKey,
                            childTask.getImage(deviceDbInfo),
                            childTask.current(now),
                            childTask.isVisible(now),
                            childTask.ordinal,
                            childTask.getProjectInfo(now),
                            childTask.isAssignedToMe(now, myUserFactory.user),
                    )
                }
    }

    private fun setIrrelevant(now: ExactTimeStamp.Local) {
        if (false) {
            val tomorrow = (DateTimeSoy.now() + 1.days).date
            val dateTimeSoy = DateTimeSoy(tomorrow, com.soywiz.klock.Time(2.hours))
            val exactTimeStamp = ExactTimeStamp.Local(dateTimeSoy)

            projectsFactory.projects
                    .values
                    .forEach {
                        val results = Irrelevant.setIrrelevant(
                                object : Project.Parent {

                                    override fun deleteProject(project: Project<*>) {
                                        TODO("Not yet implemented")
                                    }
                                },
                                it,
                                exactTimeStamp,
                                false
                        )

                        results.irrelevantExistingInstances
                                .sortedBy { it.scheduleDateTime }
                                .forEach { Log.e("asdf", "magic irrelevant instance: $it") }

                        results.irrelevantSchedules
                                .sortedBy { it.startExactTimeStamp }
                                .forEach {
                                    Log.e("asdf", "magic irrelevant schedule, schedule: $it, task: ${it.rootTask}")
                                }
                    }

            throw Exception("Irrelevant.setIrrelevant write prevented")
        }

        val instances = projectsFactory.projects
                .values
                .map { it.existingInstances + it.getRootInstances(null, now.toOffset().plusOne(), now) }
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

    fun notifyCloudPrivateFixed(
            projects: MutableSet<Project<*>>,
            userKeys: MutableCollection<UserKey>,
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
            now: ExactTimeStamp.Local,
            clear: Boolean = false,
            silent: Boolean = true,
            removedTaskKeys: List<TaskKey> = listOf(),
            sourceName: String = "other",
            domainChanged: Boolean = false,
    ) {
        val skipSave = aggregateData != null

        Preferences.tickLog.logLineDate("updateNotifications start $sourceName, skipping? $skipSave")

        if (skipSave) {
            TickHolder.addTickData(TickData.Normal(silent, sourceName, domainChanged))
            return
        }

        NotificationWrapper.instance.hideTemporary(Ticker.TICK_NOTIFICATION_ID, sourceName)

        val notificationInstances = if (clear)
            mapOf()
        else
            getRootInstances(null, now.toOffset().plusOne(), now /* 24 hack */)
                    .filter {
                        it.done == null
                                && !it.getNotified(localFactory)
                                && it.instanceDateTime.toLocalExactTimeStamp() <= now
                                && !removedTaskKeys.contains(it.taskKey)
                                && it.isAssignedToMe(now, myUserFactory.user)
                    }
                    .associateBy { it.instanceKey }

        Preferences.tickLog.logLineHour(
                "notification instances: " + notificationInstances.values.joinToString(", ") { it.name }
        )

        val instanceShownPairs = localFactory.instanceShownRecords
                .filter { it.notificationShown }
                .map { Pair(it, projectsFactory.getProjectIfPresent(it.projectId)?.getTaskIfPresent(it.taskId)) }

        instanceShownPairs.filter { it.second == null }.forEach { (instanceShownRecord, _) ->
            val scheduleDate = instanceShownRecord.run { Date(scheduleYear, scheduleMonth, scheduleDay) }
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
                hourMinute = instanceShownRecord.run { HourMinute(scheduleHour!!, scheduleMinute!!) }
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

        val shownInstanceKeys = instanceShownPairs.filter { it.second != null }
                .map { (instanceShownRecord, task) ->
                    val scheduleDate = instanceShownRecord.run { Date(scheduleYear, scheduleMonth, scheduleDay) }
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
                        hourMinute = instanceShownRecord.run { HourMinute(scheduleHour!!, scheduleMinute!!) }
                    }

                    val taskKey = TaskKey(project.projectKey, instanceShownRecord.taskId)
                    InstanceKey(taskKey, scheduleDate, TimePair(customTimeKey, hourMinute))
                }
                .toSet()

        val showInstanceKeys = notificationInstances.keys - shownInstanceKeys

        Preferences.tickLog.logLineHour("shown instances: " + shownInstanceKeys.joinToString(", ") {
            getInstance(it).name
        })

        val hideInstanceKeys = shownInstanceKeys - notificationInstances.keys

        for (showInstanceKey in showInstanceKeys)
            getInstance(showInstanceKey).setNotificationShown(localFactory, true)

        for (hideInstanceKey in hideInstanceKeys)
            getInstance(hideInstanceKey).setNotificationShown(localFactory, false)

        Preferences.tickLog.logLineHour("silent? $silent")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (notificationInstances.size > MAX_NOTIFICATIONS) { // show group
                if (shownInstanceKeys.size > MAX_NOTIFICATIONS) { // group shown
                    val silentParam =
                            if (showInstanceKeys.isNotEmpty() || hideInstanceKeys.isNotEmpty()) silent else true

                    NotificationWrapper.instance.notifyGroup(notificationInstances.values, silentParam, now)
                } else { // instances shown
                    for (shownInstanceKey in shownInstanceKeys)
                        NotificationWrapper.instance.cancelNotification(getInstance(shownInstanceKey).notificationId)

                    NotificationWrapper.instance.notifyGroup(notificationInstances.values, silent, now)
                }
            } else { // show instances
                if (shownInstanceKeys.size > MAX_NOTIFICATIONS) { // group shown
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

        if (!silent) Preferences.lastTick = now.long

        val nextAlarm = getTasks().filter { it.current(now) && it.isRootTask(now) }
                .mapNotNull { it.getNextAlarm(now, myUserFactory.user) }
                .minOrNull()
                .takeUnless { clear }

        NotificationWrapper.instance.updateAlarm(nextAlarm)

        nextAlarm?.let { Preferences.tickLog.logLineHour("next tick: $it") }
    }

    private fun notifyInstance(instance: Instance<*>, silent: Boolean, now: ExactTimeStamp.Local) =
            NotificationWrapper.instance.notifyInstance(deviceDbInfo, instance, silent, now)

    private fun updateInstance(instance: Instance<*>, now: ExactTimeStamp.Local) =
            NotificationWrapper.instance.notifyInstance(deviceDbInfo, instance, true, now)

    fun setInstanceNotified(instanceKey: InstanceKey) {
        getInstance(instanceKey).apply {
            setNotified(localFactory, true)
            setNotificationShown(localFactory, false)
        }
    }

    fun getTime(timePair: TimePair) = timePair.customTimeKey
            ?.let { projectsFactory.getCustomTime(it) }
            ?: Time.Normal(timePair.hourMinute!!)

    fun getDateTime(dateTimePair: DateTimePair) = dateTimePair.run { DateTime(date, getTime(timePair)) }

    class HourUndoData(val instanceDateTimes: Map<InstanceKey, DateTime>)

    class ReadTimes(start: ExactTimeStamp.Local, read: ExactTimeStamp.Local, stop: ExactTimeStamp.Local) {

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