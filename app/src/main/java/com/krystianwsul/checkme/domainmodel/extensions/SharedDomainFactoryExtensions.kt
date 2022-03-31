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
import com.krystianwsul.checkme.gui.main.DebugFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.filterSearch
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.schedule.SingleSchedule
import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.task.performRootIntervalUpdate
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
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
): Completable = CompletableDomainUpdate.create("clearTaskEndTimeStamps") {
    check(taskUndoData.taskKeys.isNotEmpty())

    processTaskUndoData(taskUndoData)

    val remoteProjects = taskUndoData.taskKeys
        .map { getTaskForce(it.key).project }
        .toSet()

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(remoteProjects))
}.perform(this)

@CheckResult
fun DomainUpdater.setInstancesNotNotified(
    notificationType: DomainListenerManager.NotificationType,
    instanceKeys: List<InstanceKey>,
    checkConsistency: Boolean = true,
): Completable = CompletableDomainUpdate.create("setInstancesNotNotified") { now ->
    instanceKeys.forEach {
        val instance = getInstance(it)

        if (checkConsistency) {
            check(instance.done == null)
            check(instance.instanceDateTime.timeStamp.toLocalExactTimeStamp() <= now)
            check(instance.isRootInstance())
        }

        instance.setNotified(shownFactory, false)
        instance.setNotificationShown(shownFactory, false)
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
            shownFactory,
            DateTime(date, Time.Normal(hourMinute)),
            this,
            now,
        )
    }

    val remoteProjects = instances.map { it.task.project }.toSet()

    DomainUpdater.Result(
        DomainFactory.HourUndoData(instanceDateTimes, TimeStamp(date, hourMinute)),
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
            it.setInstanceDateTime(shownFactory, instanceDateTime, this, now)
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
    DebugFragment.logDone("DomainFactory.setInstanceDone start")
    val instance = getInstance(instanceKey)

    instance.setDone(shownFactory, done, now)

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(instance.task.project)).also {
        DebugFragment.logDone("DomainFactory.setInstanceDone end")
    }
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

fun DomainFactory.getUnscheduledTasks(projectKey: ProjectKey.Shared? = null): List<Task> {
    val tasks = projectKey?.let(projectsFactory::getProjectForce)
        ?.getAllDependenciesLoadedTasks()
        ?: getAllTasks()

    return tasks.filter { it.notDeleted && it.intervalInfo.isUnscheduled() }
}

fun DomainFactory.getGroupListChildTaskDatas(
    parentTask: Task,
    now: ExactTimeStamp.Local,
    searchCriteria: SearchCriteria = SearchCriteria.empty,
): List<GroupListDataWrapper.TaskData> = parentTask.getChildTasks()
    .asSequence()
    .filterSearch(searchCriteria.search)
    .map { (childTask, filterResult) ->
        val childQuery = filterResult.getChildrenSearchCriteria(searchCriteria)

        GroupListDataWrapper.TaskData(
            childTask.taskKey,
            childTask.name,
            getGroupListChildTaskDatas(childTask, now, childQuery),
            childTask.note,
            childTask.getImage(deviceDbInfo),
            childTask.getProjectInfo(),
            childTask.ordinal,
            childTask.canMigrateDescription(now),
        )
    }
    .toList()

fun <T : Comparable<T>> DomainFactory.searchInstances(
    now: ExactTimeStamp.Local,
    searchCriteria: SearchCriteria,
    page: Int,
    projectKey: ProjectKey<*>?,
    mapper: (Instance, Collection<T>) -> T,
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
        val childSearchCriteria = task.getFilterResult(searchCriteria.search).getChildrenSearchCriteria(searchCriteria)

        val children = getChildInstanceDatas(it, now, mapper, childSearchCriteria, !debugMode)

        mapper(it, children)
    }

    return instanceDatas.sorted().take(desiredCount) to hasMore
}

private class AddChildToParentUndoData(
    val taskKey: TaskKey,
    val taskHierarchyKeys: List<TaskHierarchyKey>,
    val scheduleIds: List<ScheduleId>,
    val noScheduleOrParentsIds: List<String>,
    val deleteTaskHierarchyKey: TaskHierarchyKey,
    val instanceUndoData: InstanceUndoData?,
) : UndoData {

    override fun undo(domainFactory: DomainFactory, now: ExactTimeStamp.Local) = domainFactory.run {
        val task = getTaskForce(taskKey)

        val initialProject = task.project

        instanceUndoData?.let {
            val instance by lazy { getInstance(it.instanceKey) }

            it.parentState?.let(instance::setParentState)

            if (it.unhide) instance.unhide()
        }

        trackRootTaskIds {
            task.parentTaskHierarchies.single { it.taskHierarchyKey == deleteTaskHierarchyKey }.delete()
        }

        noScheduleOrParentsIds.map { noScheduleOrParentsId ->
            task.noScheduleOrParents.single { it.id == noScheduleOrParentsId }
        }.forEach { it.clearEndExactTimeStamp() }

        scheduleIds.map { scheduleId ->
            task.schedules.single { it.id == scheduleId }
        }.forEach { it.clearEndExactTimeStamp() }

        taskHierarchyKeys.map { taskHierarchyKey ->
            task.parentTaskHierarchies.single { it.taskHierarchyKey == taskHierarchyKey }
        }.forEach { it.clearEndExactTimeStamp() }

        val finalProject = task.project

        setOf(initialProject, finalProject)
    }

    class InstanceUndoData(
        val instanceKey: InstanceKey,
        val parentState: Instance.ParentState?,
        val unhide: Boolean,
    )
}

fun addChildToParent(
    childTask: RootTask,
    parentTask: RootTask,
    now: ExactTimeStamp.Local,
    hideInstance: Instance? = null,
    allReminders: Boolean = true,
): UndoData? {
    childTask.requireNotDeleted()

    val parentTaskData = childTask.parentTaskData

    return if (parentTaskData?.first != parentTask) {
        fun setParentViaTaskHierarchy(): AddChildToParentUndoData {
            lateinit var taskHierarchyKeys: List<TaskHierarchyKey>
            lateinit var scheduleIds: List<ScheduleId>
            lateinit var noScheduleOrParentsIds: List<String>
            lateinit var deleteTaskHierarchyKey: TaskHierarchyKey

            childTask.performRootIntervalUpdate {
                if (allReminders) {
                    taskHierarchyKeys = endAllCurrentTaskHierarchies(now)
                    scheduleIds = endAllCurrentSchedules(now)
                    noScheduleOrParentsIds = endAllCurrentNoScheduleOrParents(now)
                } else {
                    taskHierarchyKeys = listOf()
                    scheduleIds = listOf()
                    noScheduleOrParentsIds = listOf()
                }

                deleteTaskHierarchyKey = parentTask.addChild(this, now)
            }

            val instanceUndoData = hideInstance?.let {
                val previousParentState = it.parentState
                    .takeIf { it != Instance.ParentState.Unset }
                    .also { hideInstance.setParentState(Instance.ParentState.Unset) }

                val unhide = if (it.parentInstance?.task != parentTask &&
                    it.isVisible(now, Instance.VisibilityOptions(hack24 = true))
                ) {
                    it.hide()

                    true
                } else {
                    false
                }

                AddChildToParentUndoData.InstanceUndoData(it.instanceKey, previousParentState, unhide)
            }

            return AddChildToParentUndoData(
                childTask.taskKey,
                taskHierarchyKeys,
                scheduleIds,
                noScheduleOrParentsIds,
                deleteTaskHierarchyKey,
                instanceUndoData,
            )
        }

        val singleSchedule = parentTaskData?.second
            ?: childTask.intervalInfo
                .getCurrentScheduleIntervals(now)
                .singleOrNull()
                ?.let { it.schedule as? SingleSchedule }

        if (singleSchedule != null) {
            // hierarchy hack
            val singleParentInstance = parentTask.getInstances(null, null, now)
                .filter { it.isVisible(now, Instance.VisibilityOptions()) }
                .singleOrNull()

            if (singleParentInstance != null) {
                singleSchedule.getInstance(childTask).let { singleInstance ->
                    hideInstance?.let { check(it == singleInstance) }

                    val undoData = SetInstanceParentUndoData(singleInstance.instanceKey, singleInstance.parentState)

                    singleInstance.setParentState(singleParentInstance.instanceKey)

                    undoData
                }
            } else {
                setParentViaTaskHierarchy()
            }
        } else {
            setParentViaTaskHierarchy()
        }
    } else {
        null
    }
}

@CheckResult
fun DomainUpdater.undo(notificationType: DomainListenerManager.NotificationType, undoData: UndoData): Completable =
    CompletableDomainUpdate.create("undo") { now ->
        val projects = undoData.undo(this, now)
        check(projects.isNotEmpty())

        DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(projects))
    }.perform(this)

fun Project<*>.toEntryDatas(
    childTaskDatas: List<TaskListFragment.ChildTaskData>,
    showProjects: Boolean,
): List<TaskListFragment.EntryData> {
    if (childTaskDatas.isEmpty()) return emptyList()

    return if (showProjects) {
        listOf(
            TaskListFragment.ProjectData(
                getDisplayName(),
                childTaskDatas,
                projectKey,
                endExactTimeStamp == null,
                startExactTimeStamp.long,
            )
        )
    } else {
        childTaskDatas
    }
}

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

fun <T> DomainFactory.trackRootTaskIds(action: () -> T): T =
    ProjectRootTaskIdTracker.trackRootTaskIds(
        { rootTasksFactory.rootTasks },
        { projectsFactory.projects },
        rootTasksFactory,
        action,
    )

fun Collection<GroupTypeFactory.InstanceDescriptor>.splitDone() = partition { it.instanceData.done == null }

fun List<GroupTypeFactory.InstanceDescriptor>.toDoneSingleBridges(
    showDisplayText: Boolean = true,
    includeProjectDetails: Boolean = true,
) = map { GroupTypeFactory.SingleBridge.createDone(it, showDisplayText, includeProjectDetails) }

class SetInstanceParentUndoData(
    private val instanceKey: InstanceKey,
    private val parentState: Instance.ParentState,
) : UndoData {

    override fun undo(
        domainFactory: DomainFactory,
        now: ExactTimeStamp.Local,
    ) = domainFactory.getInstance(instanceKey).let {
        val initialProject = it.task.project

        domainFactory.trackRootTaskIds { it.setParentState(parentState) }

        val finalProject = it.task.project

        setOf(initialProject, finalProject)
    }
}