package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.domainmodel.takeAndHasMore
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
import com.krystianwsul.common.firebase.models.*
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
): Single<TaskUndoData> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps")

    val (taskUndoData, params) =
            setTaskEndTimeStamps(notificationType, taskKeys, deleteInstances, ExactTimeStamp.Local.now)

    DomainUpdater.Result(taskUndoData, params)
}.perform(this)

@CheckResult
fun DomainUpdater.clearTaskEndTimeStamps(
        notificationType: DomainListenerManager.NotificationType,
        taskUndoData: TaskUndoData,
): Completable = CompletableDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.clearTaskEndTimeStamps")

    check(taskUndoData.taskKeys.isNotEmpty())

    processTaskUndoData(taskUndoData, now)

    val remoteProjects = taskUndoData.taskKeys
            .map { getTaskForce(it).project }
            .toSet()

    DomainUpdater.Params(now, notificationType, DomainFactory.CloudParams(remoteProjects))
}.perform(this)

@CheckResult
fun DomainUpdater.setOrdinal(
        notificationType: DomainListenerManager.NotificationType,
        taskKey: TaskKey,
        ordinal: Double,
): Completable = CompletableDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.setOrdinal")

    val task = getTaskForce(taskKey)

    task.ordinal = ordinal

    DomainUpdater.Params(now, notificationType, DomainFactory.CloudParams(task.project))
}.perform(this)

@CheckResult
fun DomainUpdater.setInstancesNotNotified(
        notificationType: DomainListenerManager.NotificationType,
        instanceKeys: List<InstanceKey>,
): Completable = CompletableDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.setInstancesNotNotified")

    instanceKeys.forEach {
        val instance = getInstance(it)
        check(instance.done == null)
        check(instance.instanceDateTime.timeStamp.toLocalExactTimeStamp() <= now)
        check(!instance.getNotificationShown(localFactory))
        check(instance.isRootInstance())

        instance.setNotified(localFactory, false)
        instance.setNotificationShown(localFactory, false)
    }

    DomainUpdater.Params(now, notificationType)
}.perform(this)

@CheckResult
fun DomainUpdater.setInstancesAddHourActivity(
        notificationType: DomainListenerManager.NotificationType,
        instanceKeys: Collection<InstanceKey>,
): Single<DomainFactory.HourUndoData> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")

    check(instanceKeys.isNotEmpty())

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

    val remoteProjects = instances.map { it.task.project }.toSet()

    DomainUpdater.Result(
            DomainFactory.HourUndoData(instanceDateTimes),
            now,
            notificationType,
            DomainFactory.CloudParams(remoteProjects),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.undoInstancesAddHour(
        notificationType: DomainListenerManager.NotificationType,
        hourUndoData: DomainFactory.HourUndoData,
): Completable = CompletableDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")

    check(hourUndoData.instanceDateTimes.isNotEmpty())

    val instances = hourUndoData.instanceDateTimes.map { (instanceKey, instanceDateTime) ->
        getInstance(instanceKey).apply { setInstanceDateTime(localFactory, ownerKey, instanceDateTime) }
    }

    val remoteProjects = instances.map { it.task.project }.toSet()

    DomainUpdater.Params(now, notificationType, DomainFactory.CloudParams(remoteProjects))
}.perform(this)

@CheckResult
fun DomainUpdater.setInstanceDone(
        notificationType: DomainListenerManager.NotificationType,
        instanceKey: InstanceKey,
        done: Boolean,
): Completable = CompletableDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.setInstanceDone")

    val instance = getInstance(instanceKey)

    instance.setDone(localFactory, done, now)

    DomainUpdater.Params(now, notificationType, DomainFactory.CloudParams(instance.task.project))
}.perform(this)

@CheckResult
fun DomainUpdater.setInstanceNotified(
        notificationType: DomainListenerManager.NotificationType,
        instanceKey: InstanceKey,
): Completable = CompletableDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.setInstanceNotified")

    val instance = getInstance(instanceKey)

    Preferences.tickLog.logLineHour("DomainFactory: setting notified: ${instance.name}")
    setInstanceNotified(instanceKey)

    DomainUpdater.Params(notificationType = notificationType)
}.perform(this)

fun DomainUpdater.updatePhotoUrl(
        notificationType: DomainListenerManager.NotificationType,
        photoUrl: String,
): Completable = CompletableDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.updatePhotoUrl")

    DomainThreadChecker.instance.requireDomainThread()

    if (myUserFactory.isSaved || projectsFactory.isSharedSaved) throw SavedFactoryException()

    myUserFactory.user.photoUrl = photoUrl
    projectsFactory.updatePhotoUrl(deviceDbInfo.deviceInfo, photoUrl)

    DomainUpdater.Params(notificationType = notificationType)
}.perform(this)

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
): Completable = CompletableDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.undo")

    val projects = undoData.undo(this, now)
    check(projects.isNotEmpty())

    DomainUpdater.Params(now, notificationType, DomainFactory.CloudParams(projects))
}.perform(this)

fun Project<*>.toProjectData(childTaskDatas: List<TaskListFragment.ChildTaskData>) = TaskListFragment.ProjectData(
        getDisplayName(),
        childTaskDatas,
        projectKey,
        endExactTimeStamp == null,
        startExactTimeStamp.long
)

fun Project<*>.getDisplayName() = name.takeIf { it.isNotEmpty() } ?: MyApplication.context.getString(R.string.myTasks)