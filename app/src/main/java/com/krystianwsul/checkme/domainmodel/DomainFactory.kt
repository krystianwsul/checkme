package com.krystianwsul.checkme.domainmodel

import androidx.core.content.pm.ShortcutManagerCompat
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager.NotificationType
import com.krystianwsul.checkme.domainmodel.extensions.fixOffsets
import com.krystianwsul.checkme.domainmodel.extensions.migratePrivateCustomTime
import com.krystianwsul.checkme.domainmodel.extensions.updateNotifications
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
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
import com.krystianwsul.common.firebase.MyCustomTime
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
        private val getDomainUpdater: (DomainFactory) -> DomainUpdater,
) :
        PrivateCustomTime.AllRecordsSource,
        Task.ProjectUpdater,
        FactoryProvider.Domain,
        JsonTime.UserCustomTimeProvider,
        Project.CustomTimeMigrationHelper {

    companion object {

        val instanceRelay = BehaviorRelay.createDefault(NullableWrapper<DomainFactory>())!!

        val nullableInstance get() = instanceRelay.value!!.value

        val instance get() = nullableInstance!!

        var firstRun = false

        val isSaved = Observable.just(false)!! // todo find a new use for toolbar progress bar

        private val ChangeType.runType
            get() = when (this) {
                ChangeType.LOCAL -> RunType.LOCAL
                ChangeType.REMOTE -> RunType.REMOTE
            }
    }

    var remoteReadTimes: ReadTimes
        private set

    val domainListenerManager = DomainListenerManager()

    var deviceDbInfo = _deviceDbInfo

    private val changeTypeRelay = PublishRelay.create<ChangeType>()

    val notifier = Notifier(this, NotificationWrapper.instance)

    init {
        Preferences.tickLog.logLineHour("DomainFactory.init")

        val now = ExactTimeStamp.Local.now

        remoteReadTimes = ReadTimes(startTime, readTime, now)

        projectsFactory.privateProject.let {
            if (it.run { !defaultTimesCreated && customTimes.isEmpty() }) {
                DefaultCustomTimeCreator.createDefaultCustomTimes(it, myUserFactory.user)

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
                .flatMapCompletable { getDomainUpdater(this).fixOffsets(it) }
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

        val localChanges = localFactory.save()

        val values = mutableMapOf<String, Any?>()
        projectsFactory.save(values)
        myUserFactory.save(values)
        friendsFactory.save(values)

        if (values.isNotEmpty())
            databaseWrapper.update(values, checkError(this, "DomainFactory.save", values))

        val changes = localChanges || values.isNotEmpty()

        if (changes || saveParams.forceDomainChanged) {
            domainListenerManager.notify(saveParams.notificationType)

            updateShortcuts(now)
        }
    }

    private fun updateShortcuts(now: ExactTimeStamp.Local) {
        ImageManager.prefetch(deviceDbInfo, getTasks().toList()) {
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

        tryNotifyListeners("DomainFactory.onChangeTypeEvent", changeType.runType)

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
            RunType.LOCAL -> tickData?.let { tick(it, false) }
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

    // internal

    override fun getSharedCustomTimes(customTimeKey: CustomTimeKey.Project.Private) =
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

    fun getCurrentRemoteCustomTimes(now: ExactTimeStamp.Local): List<MyCustomTime> {
        val projectCustomTimes = projectsFactory.privateProject.customTimes
        val userCustomTimes = myUserFactory.user.customTimes.values

        return (projectCustomTimes + userCustomTimes).filter { it.notDeleted(now) }
    }

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
                    this,
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

            newProject.copyTaskHierarchy(now, startTaskHierarchy, parentTask.id, childTask)
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
    class HourUndoData(val instanceDateTimes: Map<InstanceKey, DateTime>)

    class ReadTimes(start: ExactTimeStamp.Local, read: ExactTimeStamp.Local, stop: ExactTimeStamp.Local) {

        val readMillis = read.long - start.long
        val instantiateMillis = stop.long - read.long
    }
}