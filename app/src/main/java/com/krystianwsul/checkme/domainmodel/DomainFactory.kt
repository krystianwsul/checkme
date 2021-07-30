package com.krystianwsul.checkme.domainmodel

import androidx.core.content.pm.ShortcutManagerCompat
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager.NotificationType
import com.krystianwsul.checkme.domainmodel.extensions.fixOffsetsAndCustomTimes
import com.krystianwsul.checkme.domainmodel.extensions.migratePrivateCustomTime
import com.krystianwsul.checkme.domainmodel.extensions.updateNotifications
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.LoadStatus
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.*
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
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates.observable

@Suppress("LeakingThis")
class DomainFactory(
    val shownFactory: Instance.ShownFactory,
    val myUserFactory: MyUserFactory,
    val projectsFactory: ProjectsFactory,
    val friendsFactory: FriendsFactory,
    _deviceDbInfo: DeviceDbInfo,
    startTime: ExactTimeStamp.Local,
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

        val instanceRelay = BehaviorRelay.createDefault(NullableWrapper<DomainFactory>())!!

        val nullableInstance get() = instanceRelay.value!!.value

        val instance get() = nullableInstance!!

        var firstRun = false

        val isSaved = LoadStatus.isLoadingObservable
    }

    var remoteReadTimes: ReadTimes
        private set

    var deviceDbInfo = _deviceDbInfo

    private val changeTypeRelay = PublishRelay.create<ChangeType>()

    val notifier = Notifier(this, NotificationWrapper.instance)

    val converter = Converter()

    init {
        Preferences.tickLog.logLineHour("DomainFactory.init")

        val now = ExactTimeStamp.Local.now

        remoteReadTimes = ReadTimes(startTime, readTime, now)

        projectsFactory.privateProject.let {
            if (it.run { !defaultTimesCreated && customTimes.isEmpty() }) {
                DefaultCustomTimeCreator.createDefaultCustomTimes(myUserFactory.user)

                it.defaultTimesCreated = true
            }
        }

        tryNotifyListeners(
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
            .flatMapCompletable { getDomainUpdater(this).fixOffsetsAndCustomTimes(it) }
            .subscribe()
            .addTo(domainDisposable)
    }

    val defaultProjectKey by lazy { projectsFactory.privateProject.projectKey }

    // misc

    fun getAllTasks() = projectsFactory.allTasks

    val taskCount get() = getAllTasks().size
    val instanceCount get() = getAllTasks().map { it.existingInstances.size }.sum()

    lateinit var instanceInfo: Pair<Int, Int>

    val customTimeCount get() = projectsFactory.privateProject.customTimes.size + myUserFactory.user.customTimes.size

    val instanceShownCount get() = notificationStorage.instanceShownMap.size

    val uuid get() = deviceDbInfo.uuid

    var debugMode by observable(false) { _, _, _ -> domainListenerManager.notify(NotificationType.All) }

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

        updateShortcuts(now)

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
            CompletableDomainUpdate.create("tryNotifyListeners") {
                DomainUpdater.Params(
                    notifyParams,
                    SaveParams(NotificationType.All, runType == RunType.REMOTE),
                )
            }
        )
    }

    private enum class RunType {

        APP_START, SIGN_IN, REMOTE
    }

    // sets

    fun setTaskEndTimeStamps(
        notificationType: NotificationType,
        taskKeys: Set<TaskKey>,
        deleteInstances: Boolean,
        now: ExactTimeStamp.Local,
    ): Pair<TaskUndoData, DomainUpdater.Params> {
        check(taskKeys.isNotEmpty())

        fun Task.getAllChildren(): List<Task> = listOf(this) + getChildTaskHierarchies(now).map {
            it.childTask.getAllChildren()
        }.flatten()

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

    fun processTaskUndoData(taskUndoData: TaskUndoData, now: ExactTimeStamp.Local) {
        taskUndoData.taskKeys
            .forEach { (taskKey, scheduleIds) ->
                val task = getTaskForce(taskKey)
                task.requireNotCurrent(now)

                task.performIntervalUpdate {
                    clearEndExactTimeStamp(now)

                    scheduleIds.forEach { scheduleId ->
                        task.schedules.single { it.id == scheduleId }.clearEndExactTimeStamp(now)
                    }
                }
            }

        taskUndoData.taskHierarchyKeys
            .asSequence()
            .map(::getTaskHierarchy)
            .forEach { it.clearEndExactTimeStamp(now) }
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

    fun getInstance(instanceKey: InstanceKey) = getTaskForce(instanceKey.taskKey).getInstance(instanceKey.scheduleKey)

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

        return (projectCustomTimes + userCustomTimes).filter { it.notDeleted() }
    }

    fun instanceToGroupListData(
        instance: Instance,
        now: ExactTimeStamp.Local,
        children: MutableMap<InstanceKey, GroupListDataWrapper.InstanceData>,
        includeProjectInfo: Boolean = true,
    ): GroupListDataWrapper.InstanceData {
        val isRootInstance = instance.isRootInstance()

        return GroupListDataWrapper.InstanceData(
            instance.done,
            instance.instanceKey,
            if (isRootInstance) instance.instanceDateTime.getDisplayText() else null,
            instance.name,
            instance.instanceDateTime.timeStamp,
            instance.instanceDateTime,
            instance.task.notDeleted(),
            instance.canAddSubtask(now),
            instance.isRootInstance(),
            instance.getCreateTaskTimePair(projectsFactory.privateProject),
            instance.task.note,
            children,
            instance.task.ordinal,
            instance.getNotificationShown(shownFactory),
            instance.task.getImage(deviceDbInfo),
            instance.isAssignedToMe(now, myUserFactory.user),
            instance.getProjectInfo(now, includeProjectInfo),
            instance.getProject().projectKey as? ProjectKey.Shared,
        )
    }

    fun <T> getChildInstanceDatas(
        instance: Instance,
        now: ExactTimeStamp.Local,
        mapper: (Instance, MutableMap<InstanceKey, T>) -> T,
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

                val childTaskMatches = childTask.matchesSearch(searchCriteria.search)

                /*
                We know this instance matches SearchCriteria.showAssignedToOthers.  If it also matches the query, we
                can skip filtering child instances, since showAssignedToOthers is meaningless for child instances.
                 */
                val childrenQuery = if (childTaskMatches) searchCriteria.copy(search = null) else searchCriteria

                val children = getChildInstanceDatas(childInstance, now, mapper, childrenQuery, filterVisible)

                if (childTaskMatches || children.isNotEmpty())
                    childInstance.instanceKey to mapper(childInstance, children)
                else
                    null
            }
            .toMap()
            .toMutableMap()
    }

    fun getChildInstanceDatas(
        instance: Instance,
        now: ExactTimeStamp.Local,
        searchCriteria: SearchCriteria = SearchCriteria.empty,
        filterVisible: Boolean = true,
        includeProjectInfo: Boolean = true,
    ): MutableMap<InstanceKey, GroupListDataWrapper.InstanceData> =
        getChildInstanceDatas(
            instance,
            now,
            { childInstance, children -> instanceToGroupListData(childInstance, now, children, includeProjectInfo) },
            searchCriteria,
            filterVisible,
        )

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
        parentHierarchyExactTimeStamp: ExactTimeStamp,
        includeProjectInfo: Boolean = true,
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
                        includeProjectInfo,
                    ),
                    childTask.note,
                    childTask.taskKey,
                    childTask.getImage(deviceDbInfo),
                    childTask.notDeleted(),
                    childTask.isVisible(now),
                    childTask.ordinal,
                    childTask.getProjectInfo(now, includeProjectInfo),
                    childTask.isAssignedToMe(now, myUserFactory.user),
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

    override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
        val provider = if (userCustomTimeKey.userKey == deviceDbInfo.key) myUserFactory.user else friendsFactory

        return provider.getUserCustomTime(userCustomTimeKey)
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

                    getCustomTime(privateCustomTimeKey) as PrivateCustomTime
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

                projectToRootConversion.endTasks[pair.first.id] = task
                projectToRootConversion.copiedTaskKeys[pair.first.taskKey] = task.taskKey
            }

            for (startTaskHierarchy in projectToRootConversion.startTaskHierarchies.values) {
                val parentTask = projectToRootConversion.endTasks.getValue(startTaskHierarchy.parentTaskId)

                val childTask = projectToRootConversion.endTasks.getValue(startTaskHierarchy.childTaskId)

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

            return projectToRootConversion.endTasks.getValue(startTask.id)
        }

        private fun convertProjectToRootHelper(
            now: ExactTimeStamp.Local,
            projectToRootConversion: ProjectToRootConversion,
            startTask: ProjectTask,
        ) {
            if (projectToRootConversion.startTasks.containsKey(startTask.id)) return

            projectToRootConversion.startTasks[startTask.id] = Pair(
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

            val childTaskHierarchies = startTask.getChildTaskHierarchies(now)
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
            val newTask = rootTasksFactory.newTask(
                RootTaskJson(
                    oldTask.name,
                    now.long,
                    now.offset,
                    oldTask.note,
                    ordinal = oldTask.ordinal,
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
                } else if (oldTask.isTopLevelTask(now)) {
                    setNoScheduleOrParent(now, newProject.projectKey)
                }
            }

            return newTask
        }
    }
}