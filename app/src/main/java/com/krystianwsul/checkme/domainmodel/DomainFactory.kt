package com.krystianwsul.checkme.domainmodel

import androidx.core.content.pm.ShortcutManagerCompat
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager.NotificationType
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ProjectToRootConversion
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.MyCustomTime
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.customtime.PrivateCustomTime
import com.krystianwsul.common.firebase.models.customtime.SharedCustomTime
import com.krystianwsul.common.firebase.models.filterSearchCriteria
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.*
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import java.util.concurrent.TimeUnit

@Suppress("LeakingThis")
class DomainFactory(
    val shownFactory: Instance.ShownFactory,
    val myUserFactory: MyUserFactory,
    val projectsFactory: ProjectsFactory,
    val friendsFactory: FriendsFactory,
    _deviceDbInfo: DeviceDbInfo,
    private val startTime: ExactTimeStamp.Local,
    readTime: ExactTimeStamp.Local,
    domainDisposable: CompositeDisposable,
    private val databaseWrapper: DatabaseWrapper,
    val rootTasksFactory: RootTasksFactory,
    val notificationStorage: FactoryProvider.NotificationStorage,
    val domainListenerManager: DomainListenerManager,
    private val getDomainUpdater: (DomainFactory) -> DomainUpdater,
) :
    FactoryProvider.Domain,
    JsonTime.UserCustomTimeProvider,
    Project.CustomTimeMigrationHelper {

    companion object {

        val instanceRelay = BehaviorRelay.createDefault(NullableWrapper<DomainFactory>())

        val nullableInstance get() = instanceRelay.value!!.value

        val instance get() = nullableInstance!!

        var firstRun = false

        val isSaved = instanceRelay.switchMap {
            it.value
                ?.isWaitingForTasks
                ?: Observable.just(true)
        }
    }

    var remoteReadTimes: ReadTimes
        private set

    var changeTypeDelay: Long? = null
        private set

    var finishedWaiting: Long? = null
        private set

    var deviceDbInfo = _deviceDbInfo

    private val changeTypeRelay = PublishRelay.create<ChangeType>()

    val notifier = Notifier(this, NotificationWrapper.instance)

    val converter = Converter()

    val isWaitingForTasks = BehaviorRelay.create<Boolean>()

    init {
        Preferences.tickLog.logLineHour("DomainFactory.init start")

        val now = ExactTimeStamp.Local.now

        remoteReadTimes = ReadTimes(startTime, readTime, now)

        projectsFactory.privateProject.let {
            if (!it.defaultTimesCreated) DefaultCustomTimeCreator.createDefaultCustomTimes(myUserFactory.user)

            it.defaultTimesCreated = true
        }

        tryNotifyListeners(
            "DomainFactory.init",
            if (firstRun) RunType.APP_START else RunType.SIGN_IN,
        )

        firstRun = false

        updateShortcuts(now)
        HasInstancesStore.update(this, now)

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
            .flatMapCompletable { getDomainUpdater(this).fixOffsetsAndCustomTimes(it) }
            .subscribe()
            .addTo(domainDisposable)

        isWaitingForTasks.filter { it }
            .firstOrError()
            .subscribe { _ -> finishedWaiting = ExactTimeStamp.Local.now.long - startTime.long }
            .addTo(domainDisposable)
    }

    val defaultProjectKey by lazy { projectsFactory.privateProject.projectKey }

    // misc

    fun getAllTasks() = projectsFactory.allDependenciesLoadedTasks

    val taskCount get() = getAllTasks().size
    val instanceCount get() = getAllTasks().map { it.existingInstances.size }.sum()

    lateinit var instanceInfo: Pair<Int, Int>

    val customTimeCount get() = projectsFactory.privateProject.customTimes.size + myUserFactory.user.customTimes.size

    val instanceShownCount get() = notificationStorage.instanceShownMap.size

    val uuid get() = deviceDbInfo.uuid

    var debugMode = false

    val copiedTaskKeys = mutableMapOf<TaskKey, TaskKey>()

    data class SaveParams(val notificationType: NotificationType, val forceDomainChanged: Boolean = false) {

        companion object {

            fun merge(saveParamsList: List<SaveParams>): SaveParams? {
                if (saveParamsList.size < 2) return saveParamsList.singleOrEmpty()

                return SaveParams(
                    NotificationType.merge(saveParamsList.map { it.notificationType })!!,
                    saveParamsList.any { it.forceDomainChanged },
                )
            }
        }
    }

    fun save(saveParams: SaveParams, now: ExactTimeStamp.Local) {
        DomainThreadChecker.instance.requireDomainThread()

        Preferences.tickLog.logLineHour("DomainFactory.save")

        val notificationChanges = notificationStorage.save()

        val values = mutableMapOf<String, Any?>()
        projectsFactory.save(values)
        myUserFactory.save(values)
        friendsFactory.save(values)
        rootTasksFactory.save(values)

        if (values.isNotEmpty())
            databaseWrapper.update(values, checkError(this, "DomainFactory.save", values))

        val changes = notificationChanges || values.isNotEmpty()

        if (changes || saveParams.forceDomainChanged) {
            domainListenerManager.notify(saveParams.notificationType)

            updateShortcuts(now)
        }
    }

    private fun updateShortcuts(now: ExactTimeStamp.Local) {
        ImageManager.prefetch(deviceDbInfo, getAllTasks()) {
            getDomainUpdater(this).updateNotifications(Notifier.Params()).subscribe()
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

    override fun clearUserInfo() = getDomainUpdater(this).updateNotifications(Notifier.Params(clear = true))

    override fun onChangeTypeEvent(changeType: ChangeType, now: ExactTimeStamp.Local) {
        MyCrashlytics.log("DomainFactory.onChangeTypeEvent $changeType")

        check(changeType == ChangeType.REMOTE)

        DomainThreadChecker.instance.requireDomainThread()

        if (changeTypeDelay == null) changeTypeDelay = now.long - startTime.long

        updateShortcuts(now)
        HasInstancesStore.update(this, now)

        tryNotifyListeners("DomainFactory.onChangeTypeEvent", RunType.REMOTE)

        changeTypeRelay.accept(changeType)
    }

    private fun tryNotifyListeners(source: String, runType: RunType) {
        MyCrashlytics.log("DomainFactory.tryNotifyListeners $source $runType")

        Preferences.tickLog.logLineHour("DomainFactory: notifying listeners")

        val tickData = TickHolder.getTickData()

        fun tick(tickData: TickData, forceNotify: Boolean): Notifier.Params {
            if (!tickData.waiting) tickData.release()

            return Notifier.Params(
                "${tickData.notifierParams.sourceName}, runType: $runType",
                tickData.notifierParams.silent && !forceNotify,
                tick = true,
            )
        }

        fun notify(): Notifier.Params {
            check(tickData == null)

            return Notifier.Params(source, false)
        }

        /**
         * todo there's a logical mistake here: TickData.Lock gets set in order to wait for any possible remote changes.
         * But, after it's set, it triggers notification updates for RunType.LOCAL as well.
         *
         * I think that TickData.Lock should trigger updateNotifications once for the initial tick, and a second time
         * only for RunType.REMOTE.
         *
         * For that matter, it's not like a single run from RunType.REMOTE is guaranteed to be the specific update we're
         * waiting for.
         */

        val notifyParams = when (runType) {
            RunType.APP_START ->
                tickData?.let { tick(it, false) } ?: Notifier.Params("$source, runType: $runType", true)
            RunType.SIGN_IN -> tickData?.let { tick(it, false) } ?: notify()
            RunType.REMOTE -> tickData?.let { tick(it, true) } ?: notify()
        }

        getDomainUpdater(this).performDomainUpdate(
            CompletableDomainUpdate("tryNotifyListeners", runType.highPriority) {
                DomainUpdater.Params(
                    notifyParams,
                    SaveParams(NotificationType.All, runType == RunType.REMOTE),
                )
            }
        )

        updateIsWaitingForTasks()
    }

    fun updateIsWaitingForTasks() {
        isWaitingForTasks.accept(waitingProjectTasks().any() || waitingProjects().any() || waitingRootTasks().any())
    }

    fun waitingProjectTasks() = projectsFactory.projects
        .values
        .asSequence()
        .flatMap { it.projectTasks }
        .filter { !it.dependenciesLoaded }

    fun waitingProjects() = projectsFactory.projects
        .values
        .asSequence()
        .filter {
            val requiredTaskKeys = it.projectRecord
                .rootTaskParentDelegate
                .rootTaskKeys

            !rootTasksFactory.rootTasks
                .keys
                .containsAll(requiredTaskKeys)
        }

    fun waitingProjectDetails() = projectsFactory.projects
        .values
        .associateWith {
            val requiredTaskKeys = it.projectRecord
                .rootTaskParentDelegate
                .rootTaskKeys

            requiredTaskKeys - rootTasksFactory.rootTasks.keys
        }
        .filterValues { it.isNotEmpty() }

    fun waitingRootTasks() = rootTasksFactory.rootTasks
        .values
        .asSequence()
        .filter { !it.dependenciesLoaded }

    private enum class RunType(val highPriority: Boolean = true) {

        APP_START, SIGN_IN, REMOTE(false)
    }

    // sets

    fun setTaskEndTimeStamps(
        notificationType: NotificationType,
        taskKeys: Set<TaskKey>,
        deleteInstances: Boolean,
        now: ExactTimeStamp.Local,
    ): Pair<TaskUndoData, DomainUpdater.Params> {
        check(taskKeys.isNotEmpty())

        fun Task.getAllChildren(): List<Task> = listOf(this) + getChildTasks().map { it.getAllChildren() }.flatten()

        val tasks = taskKeys.map { getTaskForce(it).getAllChildren() }
            .flatten()
            .toSet()

        tasks.forEach { it.requireNotDeleted() }

        val projects = tasks.map { it.project }.toSet()

        val taskUndoData = TaskUndoData()

        tasks.forEach {
            it.performIntervalUpdate { setEndData(Task.EndData(now, deleteInstances), taskUndoData) }
        }

        return taskUndoData to DomainUpdater.Params(true, notificationType, CloudParams(projects))
    }

    fun processTaskUndoData(taskUndoData: TaskUndoData) {
        taskUndoData.taskKeys
            .forEach { (taskKey, scheduleIds) ->
                val task = getTaskForce(taskKey)
                task.requireDeleted()

                task.performIntervalUpdate {
                    clearEndExactTimeStamp()

                    scheduleIds.forEach { scheduleId ->
                        task.schedules.single { it.id == scheduleId }.clearEndExactTimeStamp()
                    }
                }
            }

        taskUndoData.taskHierarchyKeys
            .asSequence()
            .map(::getTaskHierarchy)
            .forEach { it.clearEndExactTimeStamp() }
    }

    private fun getTaskHierarchy(taskHierarchyKey: TaskHierarchyKey): TaskHierarchy {
        return when (taskHierarchyKey) {
            is TaskHierarchyKey.Project -> projectsFactory.getProjectForce(taskHierarchyKey.projectId)
                .getProjectTaskHierarchy(taskHierarchyKey.taskHierarchyId)
            is TaskHierarchyKey.Nested -> getTaskForce(taskHierarchyKey.childTaskKey)
                .getNestedTaskHierarchy(taskHierarchyKey.taskHierarchyId)
            else -> throw UnsupportedOperationException()
        }
    }

    // internal

    fun getInstance(instanceKey: InstanceKey) =
        getTaskForce(instanceKey.taskKey).getInstance(instanceKey.instanceScheduleKey)

    fun getRootInstances(
        startExactTimeStamp: ExactTimeStamp.Offset?,
        endExactTimeStamp: ExactTimeStamp.Offset?,
        now: ExactTimeStamp.Local,
        searchCriteria: SearchCriteria? = null,
        filterVisible: Boolean = true,
        projectKey: ProjectKey<*>? = null,
    ): Sequence<Instance> {
        val searchData = searchCriteria?.let { Project.SearchData(it, myUserFactory.user) }

        val projects =
            projectKey?.let { listOf(projectsFactory.getProjectForce(it)) } ?: projectsFactory.projects.values

        val instanceSequences = projects.map {
            it.getRootInstances(startExactTimeStamp, endExactTimeStamp, now, searchData, filterVisible)
        }

        return combineInstanceSequences(instanceSequences)
    }

    fun getCurrentRemoteCustomTimes(): List<MyCustomTime> {
        val projectCustomTimes = projectsFactory.privateProject.customTimes
        val userCustomTimes = myUserFactory.user.customTimes.values

        return (projectCustomTimes + userCustomTimes).filter { it.notDeleted }
    }

    fun instanceToGroupListData(
        instance: Instance,
        now: ExactTimeStamp.Local,
        childInstanceDescriptors: Collection<GroupTypeFactory.InstanceDescriptor>,
    ): GroupTypeFactory.InstanceDescriptor {
        val (notDoneInstanceDescriptors, doneInstanceDescriptors) = childInstanceDescriptors.splitDone()

        val instanceData = GroupListDataWrapper.InstanceData(
            instance.done,
            instance.instanceKey,
            instance.name,
            instance.instanceDateTime.timeStamp,
            instance.instanceDate,
            instance.task.notDeleted,
            instance.canAddSubtask(now),
            instance.canMigrateDescription(now),
            instance.getCreateTaskTimePair(projectsFactory.privateProject, myUserFactory.user),
            instance.task.note,
            newMixedInstanceDataCollection(notDoneInstanceDescriptors, GroupTypeFactory.SingleBridge.CompareBy.ORDINAL),
            doneInstanceDescriptors.toDoneSingleBridges(),
            instance.ordinal,
            instance.task.getImage(deviceDbInfo),
            instance.isAssignedToMe(myUserFactory.user),
            instance.getProject().projectKey as? ProjectKey.Shared,
            instance.parentInstance?.instanceKey,
            instance.taskHasOtherVisibleInstances(now),
        )

        return GroupTypeFactory.InstanceDescriptor(
            instanceData,
            instance.instanceDateTime.toDateTimePair(),
            instance.groupByProject,
            instance,
        )
    }

    fun <T> getChildInstanceDatas(
        instance: Instance,
        now: ExactTimeStamp.Local,
        mapper: (Instance, Collection<T>) -> T,
        searchCriteria: SearchCriteria = SearchCriteria.empty,
        filterVisible: Boolean = true,
    ): Collection<T> {
        return instance.getChildInstances()
            .asSequence()
            .filter {
                !filterVisible || it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true))
            }
            .filterSearchCriteria(searchCriteria, now, myUserFactory.user)
            .mapNotNull { childInstance ->
                val childTask = childInstance.task

                val childTaskMatches = childTask.matchesSearch(searchCriteria.search)

                /*
                We know this instance matches SearchCriteria.showAssignedToOthers.  If it also matches the query, we
                can skip filtering child instances, since showAssignedToOthers is meaningless for child instances.
                 */
                val childrenQuery = if (childTaskMatches) searchCriteria.copy(search = null) else searchCriteria

                val children = getChildInstanceDatas(childInstance, now, mapper, childrenQuery, filterVisible)

                if (childTaskMatches || children.isNotEmpty())
                    mapper(childInstance, children)
                else
                    null
            }
            .toList()
    }

    fun getChildInstanceDatas(
        instance: Instance,
        now: ExactTimeStamp.Local,
        searchCriteria: SearchCriteria = SearchCriteria.empty,
        filterVisible: Boolean = true,
    ) = getChildInstanceDatas<GroupTypeFactory.InstanceDescriptor>(
        instance,
        now,
        { childInstance, children -> instanceToGroupListData(childInstance, now, children) },
        searchCriteria,
        filterVisible,
    ).splitDone()

    private val ownerKey get() = myUserFactory.user.userKey

    fun getTaskIfPresent(taskKey: TaskKey): Task? {
        return when (taskKey) {
            is TaskKey.Project -> projectsFactory.getTaskIfPresent(taskKey)
            is TaskKey.Root -> rootTasksFactory.getRootTaskIfPresent(taskKey)
            else -> throw UnsupportedOperationException()
        }
    }

    fun getTaskForce(taskKey: TaskKey) = getTaskIfPresent(taskKey)!!

    fun getTaskListChildTaskDatas(
        parentTask: Task,
        now: ExactTimeStamp.Local,
        includeProjectInfo: Boolean = true,
    ): List<TaskListFragment.ChildTaskData> {
        return parentTask.getChildTasks().map { childTask ->
            TaskListFragment.ChildTaskData(
                childTask.name,
                childTask.getScheduleText(ScheduleText),
                getTaskListChildTaskDatas(childTask, now, includeProjectInfo),
                childTask.note,
                childTask.taskKey,
                childTask.getImage(deviceDbInfo),
                childTask.notDeleted,
                childTask.isVisible(now),
                childTask.canMigrateDescription(now),
                childTask.ordinal,
                childTask.getProjectInfo(),
                childTask.isAssignedToMe(myUserFactory.user),
            )
            }
    }

    data class CloudParams(val projects: Collection<Project<*>>, val userKeys: Collection<UserKey> = emptySet()) {

        constructor(project: Project<*>, userKeys: Collection<UserKey> = emptySet()) : this(setOf(project), userKeys)

        constructor(vararg projects: Project<*>) : this(projects.toSet())
    }

    fun notifyCloud(cloudParams: CloudParams) {
        val projects = cloudParams.projects.toMutableSet()
        val userKeys = cloudParams.userKeys.toMutableSet()

        val remotePrivateProject = projects.singleOrNull { it is PrivateProject }

        remotePrivateProject?.let {
            projects.remove(it)

            userKeys.add(deviceDbInfo.key)
        }

        BackendNotifier.notify(projects, deviceDbInfo.deviceInfo, userKeys)
    }

    fun setInstanceNotified(instance: Instance) {
        instance.setNotified(shownFactory, true)
        instance.setNotificationShown(shownFactory, false)
    }

    fun getTime(timePair: TimePair) = timePair.customTimeKey
        ?.let(::getCustomTime)
        ?: Time.Normal(timePair.hourMinute!!)

    fun getDateTime(dateTimePair: DateTimePair) = dateTimePair.run { DateTime(date, getTime(timePair)) }

    override fun tryGetUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User? {
        val provider = if (userCustomTimeKey.userKey == deviceDbInfo.key) myUserFactory.user else friendsFactory

        return provider.tryGetUserCustomTime(userCustomTimeKey)
    }

    fun getCustomTime(customTimeKey: CustomTimeKey): Time.Custom {
        return when (customTimeKey) {
            is CustomTimeKey.Project<*> -> projectsFactory.getCustomTime(customTimeKey)
            is CustomTimeKey.User -> getUserCustomTime(customTimeKey)
            else -> throw UnsupportedOperationException() // compilation
        }
    }

    fun tryGetTask(taskKey: TaskKey): Task? {
        return when (taskKey) {
            is TaskKey.Root -> rootTasksFactory.getRootTaskIfPresent(taskKey)
            is TaskKey.Project -> projectsFactory.getTaskIfPresent(taskKey)
        }
    }

    override fun tryMigrateProjectCustomTime(
        customTime: Time.Custom.Project<*>,
        now: ExactTimeStamp.Local,
    ): Time.Custom.User? {
        val privateCustomTime = when (customTime) {
            is PrivateCustomTime -> customTime
            is SharedCustomTime -> {
                if (customTime.ownerKey == ownerKey) {
                    val privateCustomTimeKey = CustomTimeKey.Project.Private(
                        ownerKey.toPrivateProjectKey(),
                        customTime.privateKey!!,
                    )

                    projectsFactory.privateProject.tryGetProjectCustomTime(privateCustomTimeKey)
                } else {
                    null
                }
            }
            else -> throw UnsupportedOperationException()
        } ?: return null

        myUserFactory.user
            .customTimes
            .values
            .filter { it.customTimeRecord.privateCustomTimeId == privateCustomTime.id } // this could go south if two users migrate the same time simultaneously
            .singleOrEmpty()
            ?.let { return it }

        return migratePrivateCustomTime(privateCustomTime, now)
    }

    fun newMixedInstanceDataCollection(
        instanceDescriptors: Collection<GroupTypeFactory.InstanceDescriptor>,
        compareBy: GroupTypeFactory.SingleBridge.CompareBy,
        groupingMode: GroupType.GroupingMode = GroupType.GroupingMode.None,
        showDisplayText: Boolean = true,
        includeProjectDetails: Boolean = true,
    ) = MixedInstanceDataCollection(
        instanceDescriptors,
        myUserFactory.user,
        groupingMode,
        showDisplayText,
        includeProjectDetails,
        compareBy,
    )

    // this shouldn't use DateTime, since that leaks Time.Custom which is a model object
    class HourUndoData(val instanceDateTimes: Map<InstanceKey, DateTime>, val newTimeStamp: TimeStamp)

    class ReadTimes(start: ExactTimeStamp.Local, read: ExactTimeStamp.Local, stop: ExactTimeStamp.Local) {

        val readMillis = read.long - start.long
        val instantiateMillis = stop.long - read.long
    }

    inner class Converter {

        fun convertToRoot(
            now: ExactTimeStamp.Local,
            startTask: ProjectTask,
            newProjectKey: ProjectKey<*>,
        ): RootTask {
            val projectToRootConversion = ProjectToRootConversion()
            convertProjectToRootHelper(now, projectToRootConversion, startTask)

            val newProject = projectsFactory.getProjectForce(newProjectKey)

            for (pair in projectToRootConversion.startTasks.values) {
                val task = copyTask(
                    pair.first,
                    now,
                    newProject,
                    this@DomainFactory,
                )

                projectToRootConversion.endTasks[pair.first.taskKey] = task
                projectToRootConversion.copiedTaskKeys[pair.first.taskKey] = task.taskKey
            }

            for (startTaskHierarchy in projectToRootConversion.startTaskHierarchies.values) {
                val parentTask =
                    projectToRootConversion.endTasks.getValue(startTaskHierarchy.parentTaskKey as TaskKey.Project)
                val childTask = projectToRootConversion.endTasks.getValue(startTaskHierarchy.childTaskKey as TaskKey.Project)

                childTask.performRootIntervalUpdate { copyParentNestedTaskHierarchy(now, startTaskHierarchy, parentTask.id) }

                ProjectRootTaskIdTracker.checkTracking()
            }

            val endData = Task.EndData(now, true)

            for (pair in projectToRootConversion.startTasks.values) {
                pair.second.forEach { if (!it.hidden) it.hide() }

                // I think this might no longer be necessary, since setEndData doesn't recurse on children
                if (pair.first.endData != null) {
                    check(pair.first.endData == endData)
                } else {
                    pair.first.performIntervalUpdate { setEndData(endData) }
                }
            }

            copiedTaskKeys.putAll(projectToRootConversion.copiedTaskKeys)

            return projectToRootConversion.endTasks.getValue(startTask.taskKey)
        }

        private fun convertProjectToRootHelper(
            now: ExactTimeStamp.Local,
            projectToRootConversion: ProjectToRootConversion,
            startTask: ProjectTask,
        ) {
            if (projectToRootConversion.startTasks.containsKey(startTask.taskKey)) return

            projectToRootConversion.startTasks[startTask.taskKey] = Pair(
                startTask,
                startTask.existingInstances
                    .values
                    .filter {
                        listOf(
                            it.scheduleDateTime,
                            it.instanceDateTime,
                        ).maxOrNull()!!.toLocalExactTimeStamp() >= now
                    },
            )

            val childTaskHierarchies = startTask.getChildTaskHierarchies()
            val parentTaskHierarchies = startTask.parentTaskHierarchies

            val taskHierarchyMap = (childTaskHierarchies + parentTaskHierarchies).associateBy { it.taskHierarchyKey }
            val newTaskHierarchyMap = taskHierarchyMap - projectToRootConversion.startTaskHierarchies.keys

            projectToRootConversion.startTaskHierarchies.putAll(newTaskHierarchyMap)

            newTaskHierarchyMap.values
                .flatMap { listOf(it.parentTask, it.childTask) }
                .forEach {
                    it.requireNotDeleted()

                    convertProjectToRootHelper(now, projectToRootConversion, it as ProjectTask)
                }
        }

        private fun copyTask(
            oldTask: ProjectTask,
            now: ExactTimeStamp.Local,
            newProject: Project<*>,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        ): RootTask {
            val (ordinalDouble, ordinalString) = oldTask.ordinal.toFields()

            val newTask = rootTasksFactory.newTask(
                RootTaskJson(
                    oldTask.name,
                    now.long,
                    now.offset,
                    oldTask.note,
                    ordinal = ordinalDouble,
                    ordinalString = ordinalString,
                )
            )

            val currentSchedules = oldTask.intervalInfo.getCurrentScheduleIntervals(now).map { it.schedule }

            newTask.performRootIntervalUpdate {
                if (currentSchedules.isNotEmpty()) {
                    newTask.copySchedules(
                        now,
                        currentSchedules,
                        customTimeMigrationHelper,
                        oldTask.project.projectKey,
                        newProject.projectKey,
                    )
                } else if (oldTask.isTopLevelTask()) {
                    setNoScheduleOrParent(now, newProject.projectKey)
                }
            }

            return newTask
        }
    }
}