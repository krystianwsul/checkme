package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.domainmodel.undo.UndoData
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.FilterResult
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.filterQuery
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.task.performIntervalUpdate
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.util.*

const val SEARCH_PAGE_SIZE = 20

@CheckResult
fun DomainUpdater.setTaskEndTimeStamps(
    notificationType: DomainListenerManager.NotificationType,
    taskKeys: Set<TaskKey>,
    deleteInstances: Boolean,
): Single<TaskUndoData> = SingleDomainUpdate.create("setTaskEndTimeStamps") { now ->
    val (taskUndoData, params) = setTaskEndTimeStamps(notificationType, taskKeys, deleteInstances, now)

    DomainUpdater.Result(taskUndoData, params)
}.perform(this)

@CheckResult
fun DomainUpdater.clearTaskEndTimeStamps(
    notificationType: DomainListenerManager.NotificationType,
    taskUndoData: TaskUndoData,
): Completable = CompletableDomainUpdate.create("clearTaskEndTimeStamps") { now ->
    check(taskUndoData.taskKeys.isNotEmpty())

    processTaskUndoData(taskUndoData, now)

    val remoteProjects = taskUndoData.taskKeys
        .map { getTaskForce(it.key).project }
        .toSet()

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(remoteProjects))
}.perform(this)

@CheckResult
fun DomainUpdater.setOrdinal(
    notificationType: DomainListenerManager.NotificationType,
    taskKey: TaskKey,
    ordinal: Double,
): Completable = CompletableDomainUpdate.create("setOrdinal") {
    val task = getTaskForce(taskKey)

    task.ordinal = ordinal

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(task.project))
}.perform(this)

@CheckResult
fun DomainUpdater.setInstancesNotNotified(
    notificationType: DomainListenerManager.NotificationType,
    instanceKeys: List<InstanceKey>,
): Completable = CompletableDomainUpdate.create("setInstancesNotNotified") { now ->
    instanceKeys.forEach {
        val instance = getInstance(it)
        check(instance.done == null)
        check(instance.instanceDateTime.timeStamp.toLocalExactTimeStamp() <= now)
        check(!instance.getNotificationShown(localFactory))
        check(instance.isRootInstance())

        instance.setNotified(localFactory, false)
        instance.setNotificationShown(localFactory, false)
    }

    DomainUpdater.Params(true, notificationType)
}.perform(this)

@CheckResult
fun DomainUpdater.setInstancesAddHourActivity(
    notificationType: DomainListenerManager.NotificationType,
    instanceKeys: Collection<InstanceKey>,
): Single<DomainFactory.HourUndoData> = SingleDomainUpdate.create("setInstanceAddHourActivity") { now ->
    check(instanceKeys.isNotEmpty())

    val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

    val date = Date(calendar.toDateTimeTz())
    val hourMinute = HourMinute(calendar.toDateTimeTz())

    val instances = instanceKeys.map(this::getInstance)

    val instanceDateTimes = instances.associate { it.instanceKey to it.instanceDateTime }

    instances.forEach {
        it.setInstanceDateTime(
            localFactory,
            DateTime(date, Time.Normal(hourMinute)),
            this,
            now,
        )
    }

    val remoteProjects = instances.map { it.task.project }.toSet()

    DomainUpdater.Result(
        DomainFactory.HourUndoData(instanceDateTimes),
        true,
        notificationType,
        DomainFactory.CloudParams(remoteProjects),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.undoInstancesAddHour(
    notificationType: DomainListenerManager.NotificationType,
    hourUndoData: DomainFactory.HourUndoData,
): Completable = CompletableDomainUpdate.create("setInstanceAddHourActivity") { now ->
    check(hourUndoData.instanceDateTimes.isNotEmpty())

    val instances = hourUndoData.instanceDateTimes.map { (instanceKey, instanceDateTime) ->
        getInstance(instanceKey).also {
            it.setInstanceDateTime(localFactory, instanceDateTime, this, now)
        }
    }

    val remoteProjects = instances.map { it.task.project }.toSet()

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(remoteProjects))
}.perform(this)

@CheckResult
fun DomainUpdater.setInstanceDone(
    notificationType: DomainListenerManager.NotificationType,
    instanceKey: InstanceKey,
    done: Boolean,
): Completable = CompletableDomainUpdate.create("setInstanceDone") { now ->
    val instance = getInstance(instanceKey)

    instance.setDone(localFactory, done, now)

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(instance.task.project))
}.perform(this)

@CheckResult
fun DomainUpdater.setInstanceNotified(
    notificationType: DomainListenerManager.NotificationType,
    instanceKey: InstanceKey,
): Completable = CompletableDomainUpdate.create("setInstanceNotified") {
    val instance = getInstance(instanceKey)

    Preferences.tickLog.logLineHour("DomainFactory: setting notified: ${instance.name}")
    setInstanceNotified(getInstance(instanceKey))

    DomainUpdater.Params(false, notificationType)
}.perform(this)

fun DomainUpdater.updatePhotoUrl(
    notificationType: DomainListenerManager.NotificationType,
    photoUrl: String,
): Completable = CompletableDomainUpdate.create("updatePhotoUrl") {
    DomainThreadChecker.instance.requireDomainThread()

    myUserFactory.user.photoUrl = photoUrl
    projectsFactory.updatePhotoUrl(deviceDbInfo.deviceInfo, photoUrl)

    DomainUpdater.Params(false, notificationType)
}.perform(this)

fun DomainFactory.getUnscheduledTasks(now: ExactTimeStamp.Local, projectKey: ProjectKey.Shared? = null): List<Task> {
    val tasks = projectKey?.let(projectsFactory::getProjectForce)
        ?.getAllTasks()
        ?: getAllTasks()

    return tasks.filter { it.current(now) && it.intervalInfo.isUnscheduled(now) }
}

fun DomainFactory.getGroupListChildTaskDatas(
    parentTask: Task,
    now: ExactTimeStamp.Local,
    searchCriteria: SearchCriteria? = null,
): List<GroupListDataWrapper.TaskData> = parentTask.getChildTaskHierarchies(now)
    .asSequence()
    .map { it.childTask }
    .filterQuery(searchCriteria?.search?.query ?: "") // todo expand
    .map { (childTask, filterResult) ->
        val childQuery = if (filterResult == FilterResult.MATCHES) null else searchCriteria

        GroupListDataWrapper.TaskData(
            childTask.taskKey,
            childTask.name,
            getGroupListChildTaskDatas(childTask, now, childQuery),
            childTask.startExactTimeStamp,
            childTask.note,
            childTask.getImage(deviceDbInfo),
            childTask.isAssignedToMe(now, myUserFactory.user),
            childTask.getProjectInfo(now),
            childTask.ordinal,
        )
    }
    .toList()

fun <T : Comparable<T>> DomainFactory.searchInstances(
    now: ExactTimeStamp.Local,
    searchCriteria: SearchCriteria,
    page: Int,
    projectKey: ProjectKey<*>?,
    mapper: (Instance, MutableMap<InstanceKey, T>) -> T,
): Pair<List<T>, Boolean> {
    DomainThreadChecker.instance.requireDomainThread()

    val desiredCount = (page + 1) * SEARCH_PAGE_SIZE

    val (instances, hasMore) = getRootInstances(
        null,
        null,
        now,
        searchCriteria,
        filterVisible = !debugMode,
        projectKey = projectKey,
    ).takeAndHasMore(desiredCount)

    val instanceDatas = instances.map {
        val task = it.task

        /*
        We know this instance matches SearchCriteria.showAssignedToOthers.  If it also matches the query, we
        can skip filtering child instances, since showAssignedToOthers is meaningless for child instances.
         */
        val childSearchCriteria =
            if (task.matchesQuery(
                    searchCriteria.search?.query ?: ""
                )
            ) searchCriteria.copy(search = null) else searchCriteria // todo expand

        val children = getChildInstanceDatas(it, now, mapper, childSearchCriteria, !debugMode)

        mapper(it, children)
    }

    return instanceDatas.sorted().take(desiredCount) to hasMore
}

private class AddChildToParentUndoData(
    val taskKey: TaskKey,
    val taskHierarchyKeys: List<TaskHierarchyKey>,
    val scheduleIds: List<String>,
    val noScheduleOrParentsIds: List<String>,
    val deleteTaskHierarchyKey: TaskHierarchyKey,
    val unhideInstanceKey: InstanceKey?,
) : UndoData {

    override fun undo(domainFactory: DomainFactory, now: ExactTimeStamp.Local) = domainFactory.run {
        val task = getTaskForce(taskKey)

        val initialProject = task.project

        unhideInstanceKey?.let(::getInstance)?.unhide()

        task.parentTaskHierarchies.single { it.taskHierarchyKey == deleteTaskHierarchyKey }.delete()

        noScheduleOrParentsIds.map { noScheduleOrParentsId ->
            task.noScheduleOrParents.single { it.id == noScheduleOrParentsId }
        }.forEach { it.clearEndExactTimeStamp(now) }

        scheduleIds.map { scheduleId ->
            task.schedules.single { it.id == scheduleId }
        }.forEach { it.clearEndExactTimeStamp(now) }

        taskHierarchyKeys.map { taskHierarchyKey ->
            task.parentTaskHierarchies.single { it.taskHierarchyKey == taskHierarchyKey }
        }.forEach { it.clearEndExactTimeStamp(now) }

        val finalProject = task.project

        setOf(initialProject, finalProject)
    }
}

fun addChildToParent(
    childTask: RootTask,
    parentTask: RootTask,
    now: ExactTimeStamp.Local,
    hideInstance: Instance? = null,
): UndoData {
    childTask.requireCurrent(now)

    lateinit var taskHierarchyKeys: List<TaskHierarchyKey>
    lateinit var scheduleIds: List<String>
    lateinit var noScheduleOrParentsIds: List<String>
    lateinit var deleteTaskHierarchyKey: TaskHierarchyKey

    childTask.performIntervalUpdate {
        taskHierarchyKeys = endAllCurrentTaskHierarchies(now)
        scheduleIds = endAllCurrentSchedules(now)
        noScheduleOrParentsIds = endAllCurrentNoScheduleOrParents(now)

        deleteTaskHierarchyKey = parentTask.addChild(this, now)
    }


    val unhideInstanceKey = hideInstance?.takeIf {
        it.parentInstance?.task != parentTask &&
                it.isVisible(now, Instance.VisibilityOptions(hack24 = true))
    }?.let {
        it.hide()

        it.instanceKey
    }

    return AddChildToParentUndoData(
        childTask.taskKey,
        taskHierarchyKeys,
        scheduleIds,
        noScheduleOrParentsIds,
        deleteTaskHierarchyKey,
        unhideInstanceKey,
    )
}

@CheckResult
fun DomainUpdater.undo(notificationType: DomainListenerManager.NotificationType, undoData: UndoData): Completable =
    CompletableDomainUpdate.create("undo") { now ->
        val projects = undoData.undo(this, now)
        check(projects.isNotEmpty())

        DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(projects))
    }.perform(this)

fun Project<*>.toProjectData(childTaskDatas: List<TaskListFragment.ChildTaskData>) = TaskListFragment.ProjectData(
    getDisplayName(),
    childTaskDatas,
    projectKey,
    endExactTimeStamp == null,
    startExactTimeStamp.long
)

fun Project<*>.getDisplayName() = name.takeIf { it.isNotEmpty() } ?: MyApplication.context.getString(R.string.myTasks)

@CheckResult
fun DomainUpdater.updateNotifications(notifierParams: Notifier.Params) =
    CompletableDomainUpdate.create("updateNotifications") {
        DomainUpdater.Params(notifierParams, DomainFactory.SaveParams(DomainListenerManager.NotificationType.All))
    }.perform(this)

@CheckResult
fun DomainUpdater.setFirebaseTickListener(newTickData: TickData): Completable {
    TickHolder.addTickData(newTickData)

    /**
     * this can potentially enqueue a few identical DomainUpdates, of which only the first one may actually get the
     * TickData, but I think that's harmless
     */
    return CompletableDomainUpdate.create("setFirebaseTickListener") {
        TickHolder.getTickData()
            ?.let {
                /**
                 * If this is a TickData.Normal - meaning we're not waiting for a RunType.REMOTE - then we don't
                 * need it to stick around.
                 *
                 * I don't know for sure if that's what I'm actually doing here, but I'll take my chances.
                 */

                if (!it.waiting) it.release()

                DomainUpdater.Params(
                    it.notifierParams,
                    DomainFactory.SaveParams(DomainListenerManager.NotificationType.All, it.domainChanged),
                )
            }
            ?: DomainUpdater.Params()
    }.perform(this)
}

fun <T> DomainFactory.trackProjectRootTaskIds(action: () -> T): T {
    check(ProjectRootTaskIdTracker.instance == null)

    ProjectRootTaskIdTracker.instance = object : ProjectRootTaskIdTracker {}

    fun getMap() = rootTasksFactory.rootTasks.mapValues { (_, task) -> task.project.projectKey }

    val oldMap = getMap()
    val result = action()
    val newMap = getMap()

    check(oldMap.keys.all { newMap.containsKey(it) })

    val (otherEntries, addedEntries) = newMap.entries.partition { oldMap.containsKey(it.key) }

    val changedTriples = otherEntries.map { (taskKey, newProjectKey) ->
        val oldProjectKey = oldMap.getValue(taskKey)

        Triple(taskKey, oldProjectKey, newProjectKey)
    }.filter { (_, oldProjectKey, newProjectKey) -> oldProjectKey != newProjectKey }

    val addPairs = addedEntries.map { it.key to it.value } +
            changedTriples.map { (taskKey, _, newProjectKey) -> taskKey to newProjectKey }

    val removePairs = changedTriples.map { (taskKey, oldProjectKey, _) -> taskKey to oldProjectKey }

    fun getDelegate(projectKey: ProjectKey<*>) = projectsFactory.getProjectForce(projectKey)
        .projectRecord
        .rootTaskParentDelegate

    addPairs.forEach { (taskKey, projectKey) -> getDelegate(projectKey).addRootTaskKey(taskKey) }
    removePairs.forEach { (taskKey, projectKey) -> getDelegate(projectKey).removeRootTaskKey(taskKey) }

    val allProjectKeys = addPairs.map { it.second } + removePairs.map { it.second }

    allProjectKeys.distinct().forEach {
        val rootTaskKeys = projectsFactory.getProjectForce(it)
            .getAllTasks()
            .mapNotNull { it.taskKey as? TaskKey.Root }
            .toSet()

        rootTasksFactory.updateProjectRecord(it, rootTaskKeys)
    }

    checkNotNull(ProjectRootTaskIdTracker.instance)

    ProjectRootTaskIdTracker.instance = null

    return result
}