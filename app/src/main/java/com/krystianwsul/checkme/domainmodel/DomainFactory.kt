package com.krystianwsul.checkme.domainmodel

import androidx.annotation.CheckResult
import androidx.core.content.pm.ShortcutManagerCompat
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager.NotificationType
import com.krystianwsul.checkme.domainmodel.extensions.fixOffsets
import com.krystianwsul.checkme.domainmodel.extensions.updateNotifications
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.RemoteToRemoteConversion
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*
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
        private val databaseWrapper: DatabaseWrapper,
) : PrivateCustomTime.AllRecordsSource, Task.ProjectUpdater, FactoryProvider.Domain {

    companion object {

        val instanceRelay = BehaviorRelay.createDefault(NullableWrapper<DomainFactory>())!!

        val nullableInstance get() = instanceRelay.value!!.value

        val instance get() = nullableInstance!!

        var firstRun = false

        val isSaved = instanceRelay.switchMap { it.value?.isSaved ?: Observable.just(false) }
                .distinctUntilChanged()
                .replay(1)!!
                .apply { connect() }

        @CheckResult
        fun setFirebaseTickListener(newTickData: TickData) = completeOnDomain {
            check(MyApplication.instance.hasUserInfo)

            val domainFactory = nullableInstance

            if (domainFactory?.projectsFactory?.isSaved != false) {
                TickHolder.addTickData(newTickData)
            } else {
                val tickData = TickHolder.getTickData()

                val mergedNotifierParams = DomainUpdater.NotifierParams.merge(
                        listOfNotNull(tickData, newTickData).map { it.notifierParams }
                )!!

                val notifyListeners = (tickData?.domainChanged ?: false) || newTickData.domainChanged

                if (notifyListeners) domainFactory.domainListenerManager.notify(NotificationType.All)

                domainFactory.updateNotificationsTick(mergedNotifierParams.silent, mergedNotifierParams.sourceName)

                if (tickData?.waiting == true) {
                    TickHolder.addTickData(newTickData)
                } else {
                    tickData?.release()
                    newTickData.release()
                }
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

    var aggregateData: AggregateData? = null

    val domainListenerManager = DomainListenerManager()

    var deviceDbInfo = _deviceDbInfo

    val isSaved = BehaviorRelay.createDefault(false)!!

    private val changeTypeRelay = PublishRelay.create<ChangeType>()

    val notifier = Notifier(this, NotificationWrapper.instance)

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
                changeTypeRelay.filter { it == ChangeType.REMOTE } // todo changetype debounce
                        .firstOrError()
                        .map { "remote change" },
                Single.just(Unit)
                        .delay(1, TimeUnit.MINUTES)
                        .observeOnDomain()
                        .map { "timeout" },
        ).map { it.toObservable() }
                .merge()
                .firstOrError()
                .flatMapCompletable { AndroidDomainUpdater.fixOffsets(it) }
                .subscribe()
                .addTo(domainDisposable)
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
            notificationType: NotificationType,
            forceDomainChanged: Boolean = false,
    ) {
        DomainThreadChecker.instance.requireDomainThread()

        val skipping = aggregateData != null
        Preferences.tickLog.logLineHour("DomainFactory.save: skipping? $skipping")

        if (skipping) {
            check(notificationType is NotificationType.All)

            return
        }

        val localChanges = localFactory.save()

        val values = mutableMapOf<String, Any?>()
        projectsFactory.save(values)
        myUserFactory.save(values)
        friendsFactory.save(values)

        if (values.isNotEmpty())
            databaseWrapper.update(values, checkError(this, "DomainFactory.save", values))

        val changes = localChanges || values.isNotEmpty()

        if (changes || forceDomainChanged) domainListenerManager.notify(notificationType)
        if (changes) updateIsSaved()
    }

    private fun updateShortcuts(now: ExactTimeStamp.Local) {
        ImageManager.prefetch(deviceDbInfo, getTasks().toList()) {
            AndroidDomainUpdater.updateNotifications(DomainUpdater.NotifierParams()).subscribe()
        }

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

    override fun clearUserInfo() = AndroidDomainUpdater.updateNotifications(DomainUpdater.NotifierParams(clear = true))

    override fun onChangeTypeEvent(changeType: ChangeType, now: ExactTimeStamp.Local) {
        MyCrashlytics.log("DomainFactory.onChangeTypeEvent")

        DomainThreadChecker.instance.requireDomainThread()

        updateShortcuts(now)

        tryNotifyListeners(now, "DomainFactory.onChangeTypeEvent", changeType.runType)

        changeTypeRelay.accept(changeType)
    }

    override fun updateUserRecord(snapshot: Snapshot) {
        MyCrashlytics.log("DomainFactory.updateUserRecord")

        DomainThreadChecker.instance.requireDomainThread()

        val runType = myUserFactory.onNewSnapshot(snapshot).runType

        tryNotifyListeners(ExactTimeStamp.Local.now, "DomainFactory.updateUserRecord", runType)
    }

    private fun tryNotifyListeners(now: ExactTimeStamp.Local, source: String, runType: RunType) {
        MyCrashlytics.log("DomainFactory.tryNotifyListeners $source $runType")

        if (projectsFactory.isSaved || friendsFactory.isSaved || myUserFactory.isSaved) return

        check(aggregateData == null)

        aggregateData = AggregateData()

        Preferences.tickLog.logLineHour("DomainFactory: notifiying listeners")

        updateIsSaved()

        val copyAggregateData = aggregateData!!
        aggregateData = null

        val tickData = TickHolder.getTickData()

        fun tick(tickData: TickData, forceNotify: Boolean) {
            notifier.updateNotifications(
                    now,
                    Notifier.Params(
                            "${tickData.notifierParams.sourceName}, runType: $runType",
                            tickData.notifierParams.silent && !forceNotify,
                            tick = true,
                    ),
            )

            if (!tickData.waiting) tickData.release()
        }

        fun notify() {
            check(tickData == null)

            notifier.updateNotifications(now, Notifier.Params(source, false))
        }

        when (runType) {
            RunType.APP_START, RunType.LOCAL -> tickData?.let { tick(it, false) }
            RunType.SIGN_IN -> tickData?.let { tick(it, false) } ?: notify()
            RunType.REMOTE -> tickData?.let { tick(it, true) } ?: notify()
        }

        save(NotificationType.All, runType == RunType.REMOTE)

        copyAggregateData.run {
            if (listOf(notificationProjects, notificationUserKeys).any { it.isNotEmpty() })
                notifyCloud(CloudParams(notificationProjects, notificationUserKeys))
        }
    }

    private enum class RunType {

        APP_START, SIGN_IN, LOCAL, REMOTE
    }

    // sets

    fun setTaskEndTimeStamps(
            notificationType: NotificationType,
            taskKeys: Set<TaskKey>,
            deleteInstances: Boolean,
            now: ExactTimeStamp.Local,
    ): Pair<TaskUndoData, DomainUpdater.Params> {
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

        return taskUndoData to DomainUpdater.Params(true, notificationType, CloudParams(remoteProjects))
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

    private fun updateNotificationsTick(silent: Boolean, sourceName: String) { // todo notifier tick true
        MyCrashlytics.log("DomainFactory.updateNotificationsTick source: $sourceName")

        DomainThreadChecker.instance.requireDomainThread()

        if (projectsFactory.isSaved) throw SavedFactoryException()

        val now = ExactTimeStamp.Local.now

        notifier.updateNotifications(now, Notifier.Params(sourceName, silent, tick = true))

        save(NotificationType.All)
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
                instance.getCreateTaskTimePair(now, projectsFactory.privateProject),
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
                    newProject.projectKey,
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
                    childTask.id,
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

    data class CloudParams(val projects: Collection<Project<*>>, val userKeys: Collection<UserKey> = emptySet()) {

        constructor(project: Project<*>, userKeys: Collection<UserKey> = emptySet()) : this(setOf(project), userKeys)
    }

    fun notifyCloud(cloudParams: CloudParams) {
        val projects = cloudParams.projects.toMutableSet()
        val userKeys = cloudParams.userKeys.toMutableSet()

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

    class AggregateData {

        val notificationProjects = mutableSetOf<Project<*>>()
        val notificationUserKeys = mutableSetOf<UserKey>()
    }
}