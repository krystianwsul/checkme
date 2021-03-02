package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.domainmodel.takeAndHasMore
import com.krystianwsul.checkme.domainmodel.undo.UndoData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
import java.util.*

const val SEARCH_PAGE_SIZE = 20

fun DomainFactory.setTaskEndTimeStamps(
        source: SaveService.Source,
        taskKeys: Set<TaskKey>,
        deleteInstances: Boolean,
): TaskUndoData = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    setTaskEndTimeStamps(source, taskKeys, deleteInstances, ExactTimeStamp.Local.now)
}

fun DomainFactory.clearTaskEndTimeStamps(source: SaveService.Source, taskUndoData: TaskUndoData) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.clearTaskEndTimeStamps")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.Local.now

    processTaskUndoData(taskUndoData, now)

    updateNotifications(now)

    save(0, source)

    val remoteProjects = taskUndoData.taskKeys
            .map { getTaskForce(it).project }
            .toSet()

    notifyCloud(remoteProjects)
}

fun DomainFactory.setOrdinal(dataId: Int, taskKey: TaskKey, ordinal: Double) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setOrdinal")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.Local.now

    val task = getTaskForce(taskKey)

    task.ordinal = ordinal

    updateNotifications(now)

    save(dataId, SaveService.Source.GUI)

    notifyCloud(task.project)
}

fun DomainFactory.setInstancesNotNotified(
        dataId: Int,
        source: SaveService.Source,
        instanceKeys: List<InstanceKey>,
) = syncOnDomain {
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

    updateNotifications(now)

    save(dataId, source)
}

fun DomainFactory.setInstancesAddHourActivity(
        dataId: Int,
        source: SaveService.Source,
        instanceKeys: Collection<InstanceKey>,
): DomainFactory.HourUndoData = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")
    if (projectsFactory.isSaved) throw SavedFactoryException()

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

    updateNotifications(now)

    save(dataId, source)

    val remoteProjects = instances.map { it.task.project }.toSet()

    notifyCloud(remoteProjects)

    DomainFactory.HourUndoData(instanceDateTimes)
}

fun DomainFactory.undoInstancesAddHour(
        dataId: Int,
        source: SaveService.Source,
        hourUndoData: DomainFactory.HourUndoData,
) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.Local.now

    val instances = hourUndoData.instanceDateTimes.map { (instanceKey, instanceDateTime) ->
        getInstance(instanceKey).apply { setInstanceDateTime(localFactory, ownerKey, instanceDateTime) }
    }

    updateNotifications(now)

    save(dataId, source)

    val remoteProjects = instances.map { it.task.project }.toSet()

    notifyCloud(remoteProjects)
}

fun DomainFactory.setInstanceDone(
        notificationType: DomainListenerManager.NotificationType,
        source: SaveService.Source,
        instanceKey: InstanceKey,
        done: Boolean,
        now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): ExactTimeStamp.Local? = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setInstanceDone")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val instance = getInstance(instanceKey)

    instance.setDone(localFactory, done, now)

    updateNotifications(now)

    save(notificationType, source)

    notifyCloud(instance.task.project)

    instance.done
}

fun DomainFactory.setInstanceNotified(
        dataId: Int,
        source: SaveService.Source,
        instanceKey: InstanceKey,
) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setInstanceNotified")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val instance = getInstance(instanceKey)

    Preferences.tickLog.logLineHour("DomainFactory: setting notified: ${instance.name}")
    setInstanceNotified(instanceKey)

    save(dataId, source)
}

fun DomainFactory.updatePhotoUrl(source: SaveService.Source, photoUrl: String) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.updatePhotoUrl")
    if (myUserFactory.isSaved || projectsFactory.isSharedSaved) throw SavedFactoryException()

    myUserFactory.user.photoUrl = photoUrl
    projectsFactory.updatePhotoUrl(deviceDbInfo.deviceInfo, photoUrl)

    save(0, source)
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
): Pair<List<T>, Boolean> = syncOnDomain {
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

    instanceDatas.sorted().take(desiredCount) to hasMore
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

fun DomainFactory.undo(source: SaveService.Source, undoData: UndoData) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.undo")
    if (isSaved.value!!) throw SavedFactoryException()

    val now = ExactTimeStamp.Local.now

    val projects = undoData.undo(this, now)

    updateNotifications(now)

    save(DomainListenerManager.NotificationType.All, source)

    notifyCloud(projects)
}

fun Project<*>.toProjectData(childTaskDatas: List<TaskListFragment.ChildTaskData>) = TaskListFragment.ProjectData(
        getDisplayName(),
        childTaskDatas,
        projectKey,
        endExactTimeStamp == null,
        startExactTimeStamp.long
)

fun Project<*>.getDisplayName() = name.takeIf { it.isNotEmpty() } ?: MyApplication.context.getString(R.string.myTasks)