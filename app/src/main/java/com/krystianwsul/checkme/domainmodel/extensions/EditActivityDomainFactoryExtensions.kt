package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.UserScope
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.MyCustomTime
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.models.project.*
import com.krystianwsul.common.firebase.models.schedule.SingleSchedule
import com.krystianwsul.common.firebase.models.search.FilterResult
import com.krystianwsul.common.firebase.models.search.SearchContext
import com.krystianwsul.common.firebase.models.task.*
import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*
import io.reactivex.rxjava3.core.Single

fun UserScope.getCreateTaskData(
    startParameters: EditViewModel.StartParameters,
    currentParentSource: EditViewModel.CurrentParentSource,
): Single<DomainResult<EditViewModel.MainData>> {
    MyCrashlytics.logMethod(this)

    val mainDataSingle = if (
        startParameters is EditViewModel.StartParameters.Create &&
        startParameters.scheduleParameters is EditViewModel.ScheduleParameters.Fast &&
        currentParentSource is EditViewModel.CurrentParentSource.None
    ) {
        Single.just(getCreateTaskDataFast(startParameters.scheduleParameters))
    } else {
        domainFactorySingle.map {
            it.let { it as DomainFactory }.getCreateTaskDataSlow(startParameters, currentParentSource)
        }
    }

    return mainDataSingle.map { DomainResult.Completed(it) }
}

private fun Map<CustomTimeKey, Time.Custom>.toCustomTimeDatas() = mapValues { (customTimeKey, customTime) ->
    EditViewModel.CustomTimeData(
        customTimeKey,
        customTime.name,
        customTime.hourMinutes.toSortedMap(),
        customTime is MyCustomTime,
    )
}

private fun UserScope.getCreateTaskDataFast(
    scheduleParameters: EditViewModel.ScheduleParameters.Fast,
): EditViewModel.MainData {
    DomainThreadChecker.instance.requireDomainThread()

    val customTimeDatas = myUserFactory.user
        .customTimes
        .values
        .filter { it.notDeleted }
        .associateBy { it.key }
        .toMutableMap<CustomTimeKey, Time.Custom>()
        .toCustomTimeDatas()

    return EditViewModel.MainData(
        null,
        customTimeDatas,
        null,
        null,
        scheduleParameters.defaultScheduleOverride,
        scheduleParameters.getParentScheduleState(),
    )
}

private fun getScheduleDataWrappersAndAssignedTo(
    scheduleIntervals: List<ScheduleInterval>,
): Pair<List<ScheduleDataWrapper>, Set<UserKey>> {
    val schedules = scheduleIntervals.map { it.schedule }

    val scheduleDataWrappers = ScheduleGroup.getGroups(schedules).map(ScheduleDataWrapper::fromScheduleGroup)

    val assignedTo = schedules.map { it.assignedTo }
        .distinct()
        .singleOrEmpty()
        .orEmpty()

    return scheduleDataWrappers to assignedTo
}

// todo I think this should be getting the *current* schedule intervals
private fun Task.topLevelTaskIsSingleSchedule() = getTopLevelTask().intervalInfo
    .scheduleIntervals
    .singleOrNull()
    ?.schedule is SingleSchedule

private fun DomainFactory.getCreateTaskDataSlow(
    startParameters: EditViewModel.StartParameters,
    currentParentSource: EditViewModel.CurrentParentSource,
): EditViewModel.MainData {
    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val customTimes = getCurrentRemoteCustomTimes().associate { it.key to it as Time.Custom }.toMutableMap()

    val taskData: EditViewModel.TaskData?
    val defaultParentScheduleState: ParentScheduleState
    val defaultScheduleOverride: DateTimePair?

    when (startParameters) {
        is EditViewModel.StartParameters.TaskOrInstance -> {
            val task: Task
            val project: Project<*>

            var scheduleDataWrappers: List<ScheduleDataWrapper>? = null
            var assignedTo: Set<UserKey> = setOf()

            when (startParameters.copySource) {
                is EditParameters.Copy.CopySource.Task -> {
                    task = getTaskForce(startParameters.copySource.taskKey)
                    project = task.project

                    if (task.isTopLevelTask()) {
                        val schedules = task.intervalInfo.getCurrentScheduleIntervals(now)

                        customTimes += schedules.mapNotNull { it.schedule.customTimeKey }.map { it to getCustomTime(it) }

                        if (schedules.isNotEmpty()) {
                            getScheduleDataWrappersAndAssignedTo(schedules).let {
                                scheduleDataWrappers = it.first
                                assignedTo = it.second
                            }
                        }
                    }
                }
                is EditParameters.Copy.CopySource.Instance -> {
                    val instance = getInstance(startParameters.copySource.instanceKey)
                    task = instance.task
                    project = instance.getProject()

                    if (instance.isRootInstance()) {
                        instance.instanceTime
                            .let { it as? Time.Custom }
                            ?.let { customTimes += it.key to it }

                        scheduleDataWrappers = listOf(
                            ScheduleDataWrapper.Single(
                                ScheduleData.Single(instance.instanceDate, instance.instanceTime.timePair)
                            )
                        )

                        assignedTo = instance.getAssignedTo()
                            .map { it.id }
                            .toSet()
                    }
                }
            }

            taskData = task.run { EditViewModel.TaskData(name, note, getImage(deviceDbInfo)) }

            defaultParentScheduleState = startParameters.scheduleParameters.getParentScheduleState(
                project.getAssignedTo(assignedTo)
                    .map { it.key }
                    .toSet(),
                scheduleDataWrappers,
            )

            defaultScheduleOverride = null
        }
        is EditViewModel.StartParameters.Other -> {
            taskData = null

            when (val scheduleParameters = startParameters.scheduleParameters) {
                is EditViewModel.ScheduleParameters.Fast -> {
                    defaultParentScheduleState = scheduleParameters.getParentScheduleState()
                    defaultScheduleOverride = scheduleParameters.defaultScheduleOverride
                }
                is EditViewModel.ScheduleParameters.InstanceProject -> {
                    val parentInstance = getInstance(scheduleParameters.parentInstanceKey)

                    val dateTimePair = parentInstance.instanceDateTime.toDateTimePair()

                    defaultParentScheduleState = ParentScheduleState(
                        listOf(
                            ScheduleEntry(
                                ScheduleDataWrapper.Child(
                                    ScheduleData.Child(scheduleParameters.parentInstanceKey),
                                    parentInstance.name,
                                    dateTimePair,
                                )
                            )
                        )
                    )

                    defaultScheduleOverride = dateTimePair
                }
            }
        }
    }

    val customTimeDatas = customTimes.toCustomTimeDatas()

    fun Project<*>.toParentKey() = if (this is PrivateOwnedProject)
        null
    else
        EditViewModel.ParentKey.Project(projectKey)

    val currentParentKey: EditViewModel.ParentKey? = when (currentParentSource) {
        is EditViewModel.CurrentParentSource.None -> null
        is EditViewModel.CurrentParentSource.Set -> currentParentSource.parentKey
        is EditViewModel.CurrentParentSource.FromTask -> getTaskForce(currentParentSource.taskKey).let {
            it.parentTask
                ?.let { EditViewModel.ParentKey.Task(it.taskKey) }
                ?: it.project.toParentKey()
        }
        is EditViewModel.CurrentParentSource.FromInstance -> getInstance(currentParentSource.instanceKey).let {
            it.parentInstance
                ?.let { EditViewModel.ParentKey.Task(it.taskKey) }
                ?: it.getProject().toParentKey()
        }
        is EditViewModel.CurrentParentSource.FromTasks -> {
            currentParentSource.taskKeys
                .map { getTaskForce(it).project }
                .distinct()
                .singleOrNull()
                ?.let { it as? SharedOwnedProject }
                ?.let { EditViewModel.ParentKey.Project(it.projectKey) }
        }
    }

    fun SharedProject.toParent() = ParentScheduleManager.Parent.Project(
        name,
        EditViewModel.ParentKey.Project(projectKey),
        users.toUserDatas(),
    )

    val currentParent: ParentScheduleManager.Parent? = when (currentParentKey) {
        is EditViewModel.ParentKey.Task -> {
            val task = getTaskForce(currentParentKey.taskKey)

            ParentScheduleManager.Parent.Task(
                task.name,
                EditViewModel.ParentKey.Task(task.taskKey),
                task.hasMultipleInstances(startParameters.parentInstanceKey, now),
                task.getTopLevelTask().let {
                    val parent = it.project
                        .let { it as? SharedProject }
                        ?.toParent()

                    val (scheduleDataWrappers, assignedTo) =
                        getScheduleDataWrappersAndAssignedTo(it.intervalInfo.getCurrentScheduleIntervals(now))

                    Triple(parent, scheduleDataWrappers, assignedTo)
                },
                task.topLevelTaskIsSingleSchedule(),
            )
        }
        is EditViewModel.ParentKey.Project -> {
            val project = getProjectForce(currentParentKey.projectId)

            ParentScheduleManager.Parent.Project(
                project.displayName,
                EditViewModel.ParentKey.Project(project.projectKey),
                project.users.toUserDatas(),
            )
        }
        null -> null
    }

    val parentTaskDescription = (startParameters as? EditViewModel.StartParameters.MigrateDescription)?.taskKey
        ?.let(::getTaskForce)
        ?.note

    return EditViewModel.MainData(
        taskData,
        customTimeDatas,
        currentParent,
        parentTaskDescription,
        defaultScheduleOverride,
        defaultParentScheduleState,
    )
}

fun DomainFactory.getCreateTaskParentPickerData(
    startParameters: EditViewModel.StartParameters,
    searchCriteria: SearchCriteria,
): EditViewModel.ParentPickerData {
    MyCrashlytics.logMethod(this)

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val searchContext = SearchContext.startSearch(searchCriteria, now, myUserFactory.user)

    val forcedProject = startParameters.let { it as? EditViewModel.StartParameters.TaskOrInstance }
        ?.copySource
        ?.taskKey
        ?.let { getTaskForce(it) }
        ?.takeIf { it.isTopLevelTask() }
        ?.project
        ?.takeUnless { it is PrivateOwnedProject }

    val parentTreeDatas = getParentTreeDatas(
        now,
        startParameters.excludedTaskKeys,
        startParameters.parentInstanceKey,
        searchContext,
        forcedProject,
    )

    return EditViewModel.ParentPickerData(parentTreeDatas)
}

private fun DomainFactory.updateProjectOrder(task: Task?, newProjectKey: ProjectKey<*>?) {
    val sharedProjectKey = newProjectKey as? ProjectKey.Shared ?: return

    if (getProjectForce(newProjectKey) !is OwnedProject<*>) return

    sharedProjectKey.takeIf { it != task?.project?.projectKey }?.let {
        Preferences.updateProjectOrder(
            it,
            projectsFactory.sharedProjects
                .keys
                .toSet(),
        )
    }
}

@CheckResult
fun DomainUpdater.createScheduleTopLevelTask(
    notificationType: DomainListenerManager.NotificationType,
    createParameters: EditDelegate.CreateParameters,
    scheduleDatas: List<ScheduleData>,
    projectParameters: EditDelegate.ProjectParameters?,
    copySource: EditParameters.Copy.CopySource? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create("createScheduleTopLevelTask") { now ->
    check(scheduleDatas.isNotEmpty())

    updateProjectOrder(null, projectParameters?.key)

    val finalProjectId = projectParameters?.key ?: defaultProjectKey

    val image = createParameters.getImage(this)

    lateinit var task: RootTask
    trackRootTaskIds {
        task = createScheduleTopLevelTask(
            now,
            createParameters.name,
            scheduleDatas,
            createParameters.note,
            finalProjectId,
            image,
            this,
            assignedTo = projectParameters.nonNullAssignedTo,
        )

        copySource?.let { copyTaskOrInstance(now, task, it) }
    }

    image?.upload(task.taskKey)

    logTaskPresent(task)

    DomainUpdater.Result(
        task.toCreateResult(now),
        true,
        notificationType,
        DomainFactory.CloudParams(task.project),
    )
}.perform(this)

fun DomainFactory.logTaskPresent(task: RootTask) {
    MyCrashlytics.log("taskKey ${task.taskKey} present? " + (getTaskIfPresent(task.taskKey) != null))
}

fun RootTask.toCreateResult(now: ExactTimeStamp.Local) =
    getInstances(null, null, now).singleOrNull()
        ?.let { EditDelegate.CreateResult.Instance(it.instanceKey) }
        ?: EditDelegate.CreateResult.Task(taskKey)

@CheckResult
fun DomainUpdater.createTopLevelTask(
    notificationType: DomainListenerManager.NotificationType,
    createParameters: EditDelegate.CreateParameters,
    projectKey: ProjectKey<*>?,
    copySource: EditParameters.Copy.CopySource? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create("createTopLevelTask") { now ->
    updateProjectOrder(null, projectKey)

    val finalProjectId = projectKey ?: defaultProjectKey

    val image = createParameters.getImage(this)

    lateinit var task: RootTask
    trackRootTaskIds {
        task = createNoScheduleOrParentTask(
            now,
            createParameters.name,
            createParameters.note,
            finalProjectId,
            image,
        )

        copySource?.let { copyTaskOrInstance(now, task, it) }
    }

    image?.upload(task.taskKey)

    logTaskPresent(task)

    DomainUpdater.Result(
        task.toCreateResult(now),
        true,
        notificationType,
        DomainFactory.CloudParams(task.project),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.updateScheduleTask(
    notificationType: DomainListenerManager.NotificationType,
    taskKey: TaskKey,
    createParameters: EditDelegate.CreateParameters,
    scheduleDatas: List<ScheduleData>,
    projectParameters: EditDelegate.ProjectParameters?,
): Single<TaskKey.Root> = SingleDomainUpdate.create("updateScheduleTask") { now ->
    check(scheduleDatas.isNotEmpty())

    val image = createParameters.getImage(this)

    val projectKey = projectParameters?.key ?: defaultProjectKey

    val originalTask = getTaskForce(taskKey)
    originalTask.requireNotDeleted()

    updateProjectOrder(originalTask, projectParameters?.key)

    val originalProject = originalTask.project

    lateinit var finalTask: RootTask
    trackRootTaskIds {
        finalTask = convertAndUpdateProject(originalTask, now, projectKey)

        /*
        Not the prettiest way to do this, but if we're editing a child task to make it a top-level task, we try to carry
        over the previous instance instead of creating a new one
         */
        val parentSingleSchedule = finalTask.parentTask
            ?.getTopLevelTask()
            ?.intervalInfo
            ?.getCurrentScheduleIntervals(now)
            ?.singleOrNull()
            ?.schedule
            ?.let { it as? SingleSchedule }

        finalTask.performRootIntervalUpdate {
            endAllCurrentTaskHierarchies(now)
            endAllCurrentNoScheduleOrParents(now)

            updateSchedules(
                shownFactory,
                scheduleDatas,
                now,
                projectParameters.nonNullAssignedTo,
                this@create,
                projectKey,
                parentSingleSchedule,
            )
        }
    }

    finalTask.apply {
        setName(createParameters.name, createParameters.note)

        image?.let { setImage(deviceDbInfo, ImageState.Local(it.uuid)) }
    }

    image?.upload(finalTask.taskKey)

    DomainUpdater.Result(
        finalTask.taskKey,
        true,
        notificationType,
        DomainFactory.CloudParams(originalProject, finalTask.project),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.updateChildTask(
    notificationType: DomainListenerManager.NotificationType,
    taskKey: TaskKey,
    createParameters: EditDelegate.CreateParameters,
    parentTaskKey: TaskKey,
    removeInstanceKey: InstanceKey?,
    allReminders: Boolean = true,
): Single<TaskKey.Root> = SingleDomainUpdate.create("updateChildTask") { now ->
    lateinit var task: RootTask
    lateinit var originalProject: Project<*>
    lateinit var parentTask: RootTask
    trackRootTaskIds {
        task = convertToRoot(getTaskForce(taskKey), now)
        task.requireNotDeleted()

        originalProject = task.project

        parentTask = convertToRoot(getTaskForce(parentTaskKey), now)
        parentTask.requireNotDeleted()

        tailrec fun Task.hasAncestor(taskKey: TaskKey): Boolean {
            val currParentTask = this.parentTask ?: return false

            if (currParentTask.taskKey == taskKey) return true

            return currParentTask.hasAncestor(taskKey)
        }

        check(!parentTask.hasAncestor(taskKey))

        addChildToParent(task, parentTask, now, removeInstanceKey?.let(::getInstance), allReminders)
    }

    task.setName(createParameters.name, createParameters.note)

    val image = createParameters.getImage(this)

    image?.let { task.setImage(deviceDbInfo, ImageState.Local(it.uuid)) }

    image?.upload(task.taskKey)

    DomainUpdater.Result(
        task.taskKey,
        true,
        notificationType,
        DomainFactory.CloudParams(originalProject, task.project),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.updateTopLevelTask(
    notificationType: DomainListenerManager.NotificationType,
    taskKey: TaskKey,
    createParameters: EditDelegate.CreateParameters,
    projectKey: ProjectKey<*>?,
): Single<TaskKey.Root> = SingleDomainUpdate.create("updateTopLevelTask") { now ->
    val finalProjectKey = projectKey ?: defaultProjectKey

    val originalTask = getTaskForce(taskKey)
    originalTask.requireNotDeleted()

    updateProjectOrder(originalTask, finalProjectKey)

    val originalProject = originalTask.project

    lateinit var finalTask: RootTask
    trackRootTaskIds {
        finalTask = convertAndUpdateProject(originalTask, now, finalProjectKey)

        finalTask.performRootIntervalUpdate {
            endAllCurrentTaskHierarchies(now)
            endAllCurrentSchedules(now)
            endAllCurrentNoScheduleOrParents(now)

            setNoScheduleOrParent(now, finalProjectKey)
        }
    }

    finalTask.setName(createParameters.name, createParameters.note)

    val image = createParameters.getImage(this)

    image?.let {
        finalTask.setImage(deviceDbInfo, ImageState.Local(it.uuid))

        it.upload(finalTask.taskKey)
    }

    DomainUpdater.Result(
        finalTask.taskKey,
        true,
        notificationType,
        DomainFactory.CloudParams(originalProject, finalTask.project),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.createScheduleJoinTopLevelTask(
    notificationType: DomainListenerManager.NotificationType,
    createParameters: EditDelegate.CreateParameters,
    scheduleDatas: List<ScheduleData>,
    joinables: List<EditParameters.Join.Joinable>,
    projectParameters: EditDelegate.ProjectParameters?,
    joinAllInstances: Boolean,
): Single<TaskKey.Root> = SingleDomainUpdate.create("createScheduleJoinTopLevelTask") { now ->
    check(scheduleDatas.isNotEmpty())
    check(joinables.size > 1)

    updateProjectOrder(null, projectParameters?.key)

    val finalProjectKey = projectParameters?.key ?: defaultProjectKey

    val image = createParameters.getImage(this)

    lateinit var newParentTask: RootTask
    trackRootTaskIds {
        val joinableMap = if (joinAllInstances) {
            /**
             * I don't think the project updated is needed anymore, since that will happen with the new taskHierarchy records
             * anyway
             */
            joinables.map { it to convertAndUpdateProject(getTaskForce(it.taskKey), now, finalProjectKey) }
        } else {
            joinables.map { it to convertToRoot(getTaskForce(it.taskKey), now) }
        }

        val ordinal = joinableMap.map { it.second.ordinal }.minOrNull()

        newParentTask = createScheduleTopLevelTask(
            now,
            createParameters.name,
            scheduleDatas,
            createParameters.note,
            finalProjectKey,
            image,
            this,
            ordinal,
            projectParameters.nonNullAssignedTo,
        )

        if (joinAllInstances)
            joinTasks(newParentTask, joinableMap.map { it.second }, now, joinables.mapNotNull { it.instanceKey })
        else
            joinJoinables(newParentTask, joinableMap, now)
    }

    image?.upload(newParentTask.taskKey)

    DomainUpdater.Result(
        newParentTask.taskKey,
        true,
        notificationType,
        DomainFactory.CloudParams(newParentTask.project),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.createJoinChildTask(
    notificationType: DomainListenerManager.NotificationType,
    parentTaskKey: TaskKey,
    createParameters: EditDelegate.CreateParameters,
    joinables: List<EditParameters.Join.Joinable>,
    joinAllInstances: Boolean,
): Single<TaskKey.Root> = SingleDomainUpdate.create("createJoinChildTask") { now ->
    check(joinables.size > 1)

    val image = createParameters.getImage(this)

    lateinit var childTask: RootTask
    trackRootTaskIds {
        val parentTask = convertToRoot(getTaskForce(parentTaskKey), now)
        parentTask.requireNotDeleted()

        val joinableMap = if (joinAllInstances) {
            /**
             * I don't think the project updated is needed anymore, since that will happen with the new taskHierarchy records
             * anyway
             */
            joinables.map { it to convertAndUpdateProject(getTaskForce(it.taskKey), now, parentTask.project.projectKey) }
        } else {
            joinables.map { it to convertToRoot(getTaskForce(it.taskKey), now) }
        }

        val ordinal = joinableMap.map { it.second.ordinal }.minOrNull()

        childTask = createChildTask(
            now,
            parentTask,
            createParameters.name,
            createParameters.note,
            image?.json,
            ordinal = ordinal,
        )

        if (joinAllInstances)
            joinTasks(childTask, joinableMap.map { it.second }, now, joinables.mapNotNull { it.instanceKey })
        else
            joinJoinables(childTask, joinableMap, now)
    }

    image?.upload(childTask.taskKey)

    DomainUpdater.Result(
        childTask.taskKey,
        true,
        notificationType,
        DomainFactory.CloudParams(childTask.project),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.createJoinTopLevelTask(
    notificationType: DomainListenerManager.NotificationType,
    createParameters: EditDelegate.CreateParameters,
    joinTaskKeys: List<TaskKey>,
    projectKey: ProjectKey<*>?,
    removeInstanceKeys: List<InstanceKey>,
): Single<TaskKey.Root> = SingleDomainUpdate.create("createJoinTopLevelTask") { now ->
    check(joinTaskKeys.size > 1)

    updateProjectOrder(null, projectKey)

    val initialJoinTasks = joinTaskKeys.map(::getTaskForce)

    val originalProjects = initialJoinTasks.map { it.project }

    val finalProjectKey = projectKey ?: initialJoinTasks.map { it.project }
        .distinct()
        .single()
        .projectKey

    val image = createParameters.getImage(this)

    lateinit var newParentTask: RootTask
    trackRootTaskIds {
        val joinTasks = joinTaskKeys.map { convertAndUpdateProject(getTaskForce(it), now, finalProjectKey) }

        val ordinal = joinTasks.map { it.ordinal }.minOrNull()

        newParentTask = createNoScheduleOrParentTask(
            now,
            createParameters.name,
            createParameters.note,
            finalProjectKey,
            image,
            ordinal,
        )

        joinTasks(newParentTask, joinTasks, now, removeInstanceKeys)
    }

    image?.upload(newParentTask.taskKey)

    DomainUpdater.Result(
        newParentTask.taskKey,
        true,
        notificationType,
        DomainFactory.CloudParams(originalProjects + newParentTask.project),
    )
}.perform(this)

private fun DomainFactory.getParentTreeDatas(
    now: ExactTimeStamp.Local,
    excludedTaskKeys: Set<TaskKey>,
    parentInstanceKey: InstanceKey?,
    searchContext: SearchContext,
    forcedProject: Project<*>?,
): List<EditViewModel.ParentEntryData> {
    val parentTreeDatas = mutableListOf<EditViewModel.ParentEntryData>()

    parentTreeDatas += searchContext.search {
        getAllTasks().asSequence()
            .filter { it.showAsParent(now, excludedTaskKeys) }
            .filter { it.isTopLevelTask() && (it.project as? SharedOwnedProject)?.notDeleted != true }
            .filterSearchCriteria()
            .map { (task, filterResult) ->
                task.toParentEntryData(
                    this@getParentTreeDatas,
                    now,
                    excludedTaskKeys,
                    parentInstanceKey,
                    getChildrenSearchContext(filterResult),
                    filterResult,
                )
            }
    }

    val projectOrder = Preferences.projectOrder

    parentTreeDatas += projectsFactory.sharedProjects
        .values
        .asSequence()
        .filter { it.notDeleted }
        .let {
            if (forcedProject != null) {
                it + forcedProject
            } else {
                it
            }
        }
        .distinct()
        .map { project ->
            EditViewModel.ParentEntryData.Project(
                project.displayName,
                getProjectTaskTreeDatas(
                    now,
                    project,
                    excludedTaskKeys,
                    parentInstanceKey,
                    searchContext,
                ),
                project.projectKey,
                project.users.toUserDatas(),
                projectOrder.getOrDefault(project.projectKey, 0f),
            )
        }

    return parentTreeDatas
}

private fun DomainFactory.getProjectTaskTreeDatas(
    now: ExactTimeStamp.Local,
    project: Project<*>,
    excludedTaskKeys: Set<TaskKey>,
    parentInstanceKey: InstanceKey?,
    searchContext: SearchContext,
): List<EditViewModel.ParentEntryData.Task> {
    return when (project) {
        is OwnedProject<*> -> {
            check(project is SharedOwnedProject)

            searchContext.search {
                project.getAllDependenciesLoadedTasks()
                    .asSequence()
                    .filter { it.showAsParent(now, excludedTaskKeys) }
                    .filter { it.isTopLevelTask() }
                    .filterSearchCriteria()
                    .map { (task, filterResult) ->
                        task.toParentEntryData(
                            this@getProjectTaskTreeDatas,
                            now,
                            excludedTaskKeys,
                            parentInstanceKey,
                            getChildrenSearchContext(filterResult),
                            filterResult,
                        )
                    }
                    .toList()
            }
        }
        is ForeignProject<*> -> emptyList()
    }
}

private fun Task.showAsParent(now: ExactTimeStamp.Local, excludedTaskKeys: Set<TaskKey>): Boolean {
    if (!notDeleted) return false

    if (excludedTaskKeys.contains(taskKey)) return false

    if (!isVisible(now)) return false

    return true
}

fun DomainFactory.migrateInstanceScheduleKey(
    task: RootTask,
    instanceScheduleKey: InstanceScheduleKey,
    now: ExactTimeStamp.Local,
): InstanceScheduleKey {
    val originalTime = getTime(instanceScheduleKey.scheduleTimePair)

    val migratedTime = task.getOrCopyTime(
        instanceScheduleKey.scheduleDate.dayOfWeek,
        originalTime,
        this,
        now,
    )

    return InstanceScheduleKey(instanceScheduleKey.scheduleDate, migratedTime.timePair)
}

private fun DomainFactory.joinJoinables(
    newParentTask: RootTask,
    joinableMap: List<Pair<EditParameters.Join.Joinable, RootTask>>,
    now: ExactTimeStamp.Local,
) {
    ProjectRootTaskIdTracker.checkTracking()

    val parentInstanceKey = newParentTask.getInstances(null, null, now)
        .single()
        .instanceKey

    joinableMap.forEach { (joinable, task) ->
        when (joinable) {
            is EditParameters.Join.Joinable.Task -> addChildToParent(task, newParentTask, now)
            is EditParameters.Join.Joinable.Instance -> {
                val migratedInstanceScheduleKey =
                    migrateInstanceScheduleKey(task, joinable.instanceKey.instanceScheduleKey, now)

                task.getInstance(migratedInstanceScheduleKey).setParentState(Instance.ParentState.Parent(parentInstanceKey))
            }
        }
    }
}

private fun DomainFactory.joinTasks(
    newParentTask: RootTask,
    joinTasks: List<RootTask>,
    now: ExactTimeStamp.Local,
    removeInstanceKeys: List<InstanceKey>,
) {
    newParentTask.requireNotDeleted()
    check(joinTasks.size > 1)

    // todo this would be a lot easier if I paired them immediately
    val unusedRemoveInstanceKeys = removeInstanceKeys.toMutableList()

    joinTasks.forEach { task ->
        val removeInstance = removeInstanceKeys.filter { it.taskKey == task.taskKey }
            .singleOrEmpty()
            ?.let {
                unusedRemoveInstanceKeys -= it

                getInstance(it)
            }

        addChildToParent(task, newParentTask, now, removeInstance)
    }

    check(unusedRemoveInstanceKeys.isEmpty())
}

private fun Task.hasMultipleInstances(parentInstanceKey: InstanceKey?, now: ExactTimeStamp.Local) =
    parentInstanceKey?.takeIf { it.taskKey == taskKey }?.let { hasOtherVisibleInstances(now, it) }

private fun Task.toParentEntryData(
    domainFactory: DomainFactory,
    now: ExactTimeStamp.Local,
    excludedTaskKeys: Set<TaskKey>,
    parentInstanceKey: InstanceKey?,
    searchContext: SearchContext,
    filterResult: FilterResult,
) = EditViewModel.ParentEntryData.Task(
    name,
    domainFactory.getTaskListChildTaskDatas(now, this, excludedTaskKeys, parentInstanceKey, searchContext),
    taskKey,
    getScheduleText(ScheduleText),
    note,
    EditViewModel.SortKey.TaskSortKey(startExactTimeStamp),
    project.projectKey,
    hasMultipleInstances(parentInstanceKey, now),
    topLevelTaskIsSingleSchedule(),
    filterResult.matchesSearch,
)

private fun DomainFactory.getTaskListChildTaskDatas(
    now: ExactTimeStamp.Local,
    parentTask: Task,
    excludedTaskKeys: Set<TaskKey>,
    parentInstanceKey: InstanceKey?,
    searchContext: SearchContext,
): List<EditViewModel.ParentEntryData.Task> = searchContext.search {
    parentTask.getChildTasks()
        .asSequence()
        .filter { it.showAsParent(now, excludedTaskKeys) }
        .filterSearchCriteria()
        .map { (task, filterResult) ->
            task.toParentEntryData(
                this@getTaskListChildTaskDatas,
                now,
                excludedTaskKeys,
                parentInstanceKey,
                getChildrenSearchContext(filterResult),
                filterResult,
            )
        }
        .toList()
}

private fun DomainFactory.copyTaskOrInstance(
    now: ExactTimeStamp.Local,
    parentTask: RootTask,
    copySource: EditParameters.Copy.CopySource,
) {
    val childPairs = when (copySource) {
        is EditParameters.Copy.CopySource.Task ->
            getTaskForce(copySource.taskKey).getChildTasks().map { it to EditParameters.Copy.CopySource.Task(it.taskKey) }
        is EditParameters.Copy.CopySource.Instance -> getInstance(copySource.instanceKey).getChildInstances()
            .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true)) }
            .map { it.task to EditParameters.Copy.CopySource.Instance(it.instanceKey) }
    }

    childPairs.forEach { (childTask, copySource) ->
        childTask.getImage(deviceDbInfo)?.let { check(it is ImageState.Remote) }

        createChildTask(
            now,
            parentTask,
            childTask.name,
            childTask.note,
            childTask.imageJson,
            copySource,
        )
    }
}

fun DomainFactory.createChildTask(
    now: ExactTimeStamp.Local,
    parentTask: RootTask,
    name: String,
    note: String?,
    imageJson: TaskJson.Image?,
    copySource: EditParameters.Copy.CopySource? = null,
    ordinal: Ordinal? = null,
): RootTask {
    check(name.isNotEmpty())
    parentTask.requireNotDeleted()

    val childTask = parentTask.createChildTask(now, name, note, imageJson, ordinal)

    copySource?.let { copyTaskOrInstance(now, childTask, it) }

    return childTask
}

private fun Collection<ProjectUser>.toUserDatas() = associate {
    it.id to EditViewModel.UserData(it.id, it.name, it.photoUrl)
}

private val EditDelegate.ProjectParameters?.nonNullAssignedTo get() = this?.assignedTo.orEmpty()

fun DomainFactory.createScheduleTopLevelTask(
    now: ExactTimeStamp.Local,
    name: String,
    scheduleDatas: List<ScheduleData>,
    note: String?,
    projectKey: ProjectKey<*>,
    image: EditDelegate.CreateParameters.Image?,
    customTimeMigrationHelper: OwnedProject.CustomTimeMigrationHelper,
    ordinal: Ordinal? = null,
    assignedTo: Set<UserKey> = setOf(),
) = createRootTask(now, image, name, note, ordinal).apply {
    createSchedules(now, scheduleDatas, assignedTo, customTimeMigrationHelper, projectKey)
}

private fun DomainFactory.createNoScheduleOrParentTask(
    now: ExactTimeStamp.Local,
    name: String,
    note: String?,
    projectKey: ProjectKey<*>,
    image: EditDelegate.CreateParameters.Image?,
    ordinal: Ordinal? = null,
) = createRootTask(now, image, name, note, ordinal).apply {
    performRootIntervalUpdate { setNoScheduleOrParent(now, projectKey) }
}

private fun DomainFactory.createRootTask(
    now: ExactTimeStamp.Local,
    image: EditDelegate.CreateParameters.Image?,
    name: String,
    note: String?,
    ordinal: Ordinal?,
): RootTask {
    return rootTasksFactory.createTask(
        now,
        image?.json,
        name,
        note,
        ordinal,
    )
}

private fun DomainFactory.convertAndUpdateProject(
    task: Task,
    now: ExactTimeStamp.Local,
    projectKey: ProjectKey<*>,
): RootTask {
    val isTopLevelTask = task.isTopLevelTask()

    return when (task) {
        is RootTask -> task.updateProject(projectKey)
        is ProjectTask -> converter.convertToRoot(now, task, projectKey)
    }.also {
        // this function is may be a no-op for child tasks
        if (isTopLevelTask) check(it.project.projectKey == projectKey)
    }
}

fun DomainFactory.convertToRoot(task: Task, now: ExactTimeStamp.Local): RootTask {
    if (task is RootTask) return task

    return converter.convertToRoot(now, task as ProjectTask, task.project.projectKey)
}