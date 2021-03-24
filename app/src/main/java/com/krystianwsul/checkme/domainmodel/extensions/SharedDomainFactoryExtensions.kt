package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.domainmodel.undo.UndoData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
import io.reactivex.rxjava3.core.Single
import java.util.*

const val SEARCH_PAGE_SIZE = 20

@CheckResult
fun DomainUpdater.setTaskEndTimeStamps(
        notificationType: DomainListenerManager.NotificationType,
        taskKeys: Set<TaskKey>,
        deleteInstances: Boolean,
): Single<TaskUndoData> = updateDomainSingle {
    MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps")

    val (taskUndoData, params) =
            setTaskEndTimeStamps(notificationType, taskKeys, deleteInstances, ExactTimeStamp.Local.now)

    DomainUpdater.Result(taskUndoData, params)
}

@CheckResult
fun DomainUpdater.clearTaskEndTimeStamps(
        notificationType: DomainListenerManager.NotificationType,
        taskUndoData: TaskUndoData,
) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.clearTaskEndTimeStamps")

    check(taskUndoData.taskKeys.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    processTaskUndoData(taskUndoData, now)

    notifier.updateNotifications(now)

    save(notificationType)

    val remoteProjects = taskUndoData.taskKeys
            .map { getTaskForce(it).project }
            .toSet()

    DomainUpdater.Params(DomainFactory.CloudParams(remoteProjects))
}

@CheckResult
fun DomainUpdater.setOrdinal(
        notificationType: DomainListenerManager.NotificationType,
        taskKey: TaskKey,
        ordinal: Double,
) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.setOrdinal")

    val now = ExactTimeStamp.Local.now

    val task = getTaskForce(taskKey)

    task.ordinal = ordinal

    notifier.updateNotifications(now)

    save(notificationType)

    DomainUpdater.Params(DomainFactory.CloudParams(task.project))
}

@CheckResult
fun DomainFactory.setInstancesNotNotified(
        notificationType: DomainListenerManager.NotificationType,
        instanceKeys: List<InstanceKey>,
) = completeOnDomain {
    MyCrashlytics.log("DomainFactory.setInstancesNotNotified")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.Local.now

    instanceKeys.forEach {
        val instance = getInstance(it)
        check(instance.done == null)
        check(instance.instanceDateTime.timeStamp.toLocalExactTimeStamp() <= now)
        check(!instance.getNotificationShown(localFactory))
        check(instance.isRootInstance())

        instance.setNotified(localFactory, false)
        instance.setNotificationShown(localFactory, false)
    }

    notifier.updateNotifications(now)

    save(notificationType)
}

@CheckResult
fun DomainUpdater.setInstancesAddHourActivity(
        notificationType: DomainListenerManager.NotificationType,
        instanceKeys: Collection<InstanceKey>,
): Single<DomainFactory.HourUndoData> = updateDomainSingle {
    MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")

    check(instanceKeys.isNotEmpty())

    val now = ExactTimeStamp.Local.now
    val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

    val date = Date(calendar.toDateTimeTz())
    val hourMinute = HourMinute(calendar.toDateTimeTz())

    val instances = instanceKeys.map(this::getInstance)

    val instanceDateTimes = instances.associate { it.instanceKey to it.instanceDateTime }

    instances.forEach {
        it.setInstanceDateTime(
                localFactory,
                ownerKey,
                DateTime(date, Time.Normal(hourMinute)),
        )
    }

    notifier.updateNotifications(now)

    save(notificationType)

    val remoteProjects = instances.map { it.task.project }.toSet()

    DomainUpdater.Result(DomainFactory.HourUndoData(instanceDateTimes), DomainFactory.CloudParams(remoteProjects))
}

@CheckResult
fun DomainUpdater.undoInstancesAddHour(
        notificationType: DomainListenerManager.NotificationType,
        hourUndoData: DomainFactory.HourUndoData,
) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")

    val now = ExactTimeStamp.Local.now

    check(hourUndoData.instanceDateTimes.isNotEmpty())

    val instances = hourUndoData.instanceDateTimes.map { (instanceKey, instanceDateTime) ->
        getInstance(instanceKey).apply { setInstanceDateTime(localFactory, ownerKey, instanceDateTime) }
    }

    notifier.updateNotifications(now)

    save(notificationType)

    val remoteProjects = instances.map { it.task.project }.toSet()

    DomainUpdater.Params(DomainFactory.CloudParams(remoteProjects))
}

@CheckResult
fun DomainUpdater.setInstanceDone(
        notificationType: DomainListenerManager.NotificationType,
        instanceKey: InstanceKey,
        done: Boolean,
        now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): Single<NullableWrapper<ExactTimeStamp.Local>> = updateDomainSingle {
    MyCrashlytics.log("DomainFactory.setInstanceDone")

    val instance = getInstance(instanceKey)

    instance.setDone(localFactory, done, now)

    notifier.updateNotifications(now)

    save(notificationType)

    DomainUpdater.Result(NullableWrapper(instance.done), DomainFactory.CloudParams(instance.task.project))
}

@CheckResult
fun DomainFactory.setInstanceNotified(
        notificationType: DomainListenerManager.NotificationType,
        instanceKey: InstanceKey,
) = completeOnDomain {
    MyCrashlytics.log("DomainFactory.setInstanceNotified")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val instance = getInstance(instanceKey)

    Preferences.tickLog.logLineHour("DomainFactory: setting notified: ${instance.name}")
    setInstanceNotified(instanceKey)

    save(notificationType)
}

fun DomainFactory.updatePhotoUrl(notificationType: DomainListenerManager.NotificationType, photoUrl: String) {
    MyCrashlytics.log("DomainFactory.updatePhotoUrl")

    DomainThreadChecker.instance.requireDomainThread()

    if (myUserFactory.isSaved || projectsFactory.isSharedSaved) throw SavedFactoryException()

    myUserFactory.user.photoUrl = photoUrl
    projectsFactory.updatePhotoUrl(deviceDbInfo.deviceInfo, photoUrl)

    save(notificationType)
}

fun DomainFactory.getUnscheduledTasks(now: ExactTimeStamp.Local) =
        getTasks().filter { it.current(now) && it.isUnscheduled(now) }

fun DomainFactory.getGroupListChildTaskDatas(
        parentTask: Task<*>,
        now: ExactTimeStamp.Local,
        searchCriteria: SearchCriteria? = null,
): List<GroupListDataWrapper.TaskData> = parentTask.getChildTaskHierarchies(now)
        .asSequence()
        .map { it.childTask }
        .filterQuery(searchCriteria?.query)
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
        mapper: (Instance<*>, ExactTimeStamp.Local, MutableMap<InstanceKey, T>) -> T,
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
                if (task.matchesQuery(searchCriteria.query)) searchCriteria.copy(query = "") else searchCriteria

        val children = getChildInstanceDatas(it, now, mapper, childSearchCriteria, !debugMode)

        mapper(it, now, children)
    }

    return instanceDatas.sorted().take(desiredCount) to hasMore
}

private class AddChildToParentUndoData(
        val taskKey: TaskKey,
        val taskHierarchyKeys: List<TaskHierarchyKey>,
        val scheduleIds: List<ScheduleId>,
        val noScheduleOrParentsIds: List<String>,
        val deleteTaskHierarchyKey: TaskHierarchyKey,
        val unhideInstanceKey: InstanceKey?,
) : UndoData {

    override fun undo(domainFactory: DomainFactory, now: ExactTimeStamp.Local) = domainFactory.run {
        val task = getTaskForce(taskKey)

        unhideInstanceKey?.let(::getInstance)?.unhide()

        task.parentTaskHierarchies.single { it.taskHierarchyKey == deleteTaskHierarchyKey }.delete()

        noScheduleOrParentsIds.map { noScheduleOrParentsId ->
            task.noScheduleOrParents.single { it.id == noScheduleOrParentsId }
        }.forEach { it.clearEndExactTimeStamp(now) }

        scheduleIds.map { scheduleId ->
            task.schedules.single { it.scheduleId == scheduleId }
        }.forEach { it.clearEndExactTimeStamp(now) }

        taskHierarchyKeys.map { taskHierarchyKey ->
            task.parentTaskHierarchies.single { it.taskHierarchyKey == taskHierarchyKey }
        }.forEach { it.clearEndExactTimeStamp(now) }

        setOf(task.project)
    }
}

fun addChildToParent(
        childTask: Task<*>,
        parentTask: Task<*>,
        now: ExactTimeStamp.Local,
        hideInstance: Instance<*>? = null,
): UndoData {
    childTask.requireCurrent(now)

    val taskHierarchyKeys = childTask.endAllCurrentTaskHierarchies(now)
    val scheduleIds = childTask.endAllCurrentSchedules(now)
    val noScheduleOrParentsIds = childTask.endAllCurrentNoScheduleOrParents(now)

    val deleteTaskHierarchyKey = parentTask.addChild(childTask, now)

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
fun DomainUpdater.undo(
        notificationType: DomainListenerManager.NotificationType,
        undoData: UndoData,
) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.undo")

    val now = ExactTimeStamp.Local.now

    val projects = undoData.undo(this, now)
    check(projects.isNotEmpty())

    notifier.updateNotifications(now)

    save(notificationType)

    DomainUpdater.Params(DomainFactory.CloudParams(projects))
}

fun Project<*>.toProjectData(childTaskDatas: List<TaskListFragment.ChildTaskData>) = TaskListFragment.ProjectData(
        getDisplayName(),
        childTaskDatas,
        projectKey,
        endExactTimeStamp == null,
        startExactTimeStamp.long
)

fun Project<*>.getDisplayName() = name.takeIf { it.isNotEmpty() } ?: MyApplication.context.getString(R.string.myTasks)