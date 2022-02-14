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
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.gui.edit.ParentScheduleManager
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.MyCustomTime
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.schedule.SingleSchedule
import com.krystianwsul.common.firebase.models.task.*
import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*
import io.reactivex.rxjava3.core.Single

fun UserScope.getCreateTaskData(
    startParameters: EditViewModel.StartParameters,
    currentParentSource: EditViewModel.CurrentParentSource,
): Single<DomainResult<EditViewModel.MainData>> {
    MyCrashlytics.logMethod(this)

    val mainDataSingle = if (startParameters is EditViewModel.StartParameters.Create &&
        currentParentSource is EditViewModel.CurrentParentSource.None
    ) {
        Single.just(getCreateTaskDataFast())
    } else {
        domainFactorySingle.map { (it as DomainFactory).getCreateTaskDataSlow(startParameters, currentParentSource) }
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

private fun UserScope.getCreateTaskDataFast(): EditViewModel.MainData {
    DomainThreadChecker.instance.requireDomainThread()

    val customTimeDatas = myUserFactory.user
        .customTimes
        .values
        .filter { it.notDeleted }
        .associateBy { it.key }
        .toMutableMap<CustomTimeKey, Time.Custom>()
        .toCustomTimeDatas()

    return EditViewModel.MainData(null, customTimeDatas, null, null, null)
}

private fun getScheduleDataWrappersAndAssignedTo(
    scheduleIntervals: List<ScheduleInterval>,
): Pair<List<EditViewModel.ScheduleDataWrapper>, Set<UserKey>> {
    val schedules = scheduleIntervals.map { it.schedule }

    val scheduleDataWrappers = ScheduleGroup.getGroups(schedules).map {
        EditViewModel.ScheduleDataWrapper.fromScheduleData(it.scheduleData)
    }

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

    val taskData = (startParameters as? EditViewModel.StartParameters.Task)?.let {
        val task = getTaskForce(it.taskKey)

        var scheduleDataWrappers: List<EditViewModel.ScheduleDataWrapper>? = null
        var assignedTo: Set<UserKey> = setOf()

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

        EditViewModel.TaskData(
            task.name,
            scheduleDataWrappers,
            task.note,
            task.getImage(deviceDbInfo),
            task.project
                .getAssignedTo(assignedTo)
                .map { it.key }
                .toSet(),
        )
    }

    val customTimeDatas = customTimes.toCustomTimeDatas()

    val showAllInstancesDialog = startParameters.showAllInstancesDialog(this, now)

    val currentParentKey: EditViewModel.ParentKey? = when (currentParentSource) {
        is EditViewModel.CurrentParentSource.None -> null
        is EditViewModel.CurrentParentSource.Set -> currentParentSource.parentKey
        is EditViewModel.CurrentParentSource.FromTask -> {
            val task = getTaskForce(currentParentSource.taskKey)
            val parentTask = task.parentTask

            if (parentTask == null) {
                when (val projectKey = task.project.projectKey) {
                    is ProjectKey.Private -> null
                    is ProjectKey.Shared -> EditViewModel.ParentKey.Project(projectKey)
                    else -> throw UnsupportedOperationException()
                }
            } else {
                EditViewModel.ParentKey.Task(parentTask.taskKey)
            }
        }
        is EditViewModel.CurrentParentSource.FromTasks -> {
            currentParentSource.taskKeys
                .map { getTaskForce(it).project }
                .distinct()
                .singleOrNull()
                ?.let { it as? SharedProject }
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
            val project = projectsFactory.sharedProjects.getValue(currentParentKey.projectId)

            ParentScheduleManager.Parent.Project(
                project.name,
                EditViewModel.ParentKey.Project(project.projectKey),
                project.users.toUserDatas(),
            )
        }
        null -> null
    }

    val parentTaskDescription = (startParameters as? EditViewModel.StartParameters.MigrateDescription)?.taskKey
        ?.let(::getTaskForce)
        ?.note

    return EditViewModel.MainData(taskData, customTimeDatas, showAllInstancesDialog, currentParent, parentTaskDescription)
}

fun DomainFactory.getCreateTaskParentPickerData(
    startParameters: EditViewModel.StartParameters,
): EditViewModel.ParentPickerData {
    MyCrashlytics.logMethod(this)

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val parentTreeDatas = getParentTreeDatas(now, startParameters.excludedTaskKeys, startParameters.parentInstanceKey)

    return EditViewModel.ParentPickerData(parentTreeDatas)
}

private fun DomainFactory.updateProjectOrder(task: Task?, newSharedProjectKey: ProjectKey.Shared?) {
    newSharedProjectKey?.takeIf { it != task?.project?.projectKey }?.let {
        Preferences.updateProjectOrder(
            it,
            projectsFactory.sharedProjects
                .keys
                .map { it as ProjectKey.Shared }
                .toSet(),
        )
    }
}

@CheckResult
fun DomainUpdater.createScheduleTopLevelTask(
    notificationType: DomainListenerManager.NotificationType,
    createParameters: EditDelegate.CreateParameters,
    scheduleDatas: List<ScheduleData>,
    sharedProjectParameters: EditDelegate.SharedProjectParameters?,
    copyTaskKey: TaskKey? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create("createScheduleTopLevelTask") { now ->
    check(scheduleDatas.isNotEmpty())

    updateProjectOrder(null, sharedProjectParameters?.key)

    val finalProjectId = sharedProjectParameters?.key ?: defaultProjectKey

    val image = createParameters.getImage(this)

    lateinit var task: RootTask
    trackRootTaskIds {
        task = createScheduleTopLevelTask(
            now,
            createParameters.name,
            scheduleDatas.map { it to getTime(it.timePair) },
            createParameters.note,
            finalProjectId,
            image,
            this,
            assignedTo = sharedProjectParameters.nonNullAssignedTo,
        )

        copyTaskKey?.let { copyTask(now, task, it) }
    }

    image?.upload(task.taskKey)

    DomainUpdater.Result(
        task.toCreateResult(now),
        true,
        notificationType,
        DomainFactory.CloudParams(task.project),
    )
}.perform(this)

fun RootTask.toCreateResult(now: ExactTimeStamp.Local) =
    getInstances(null, null, now).singleOrNull()
        ?.let { EditDelegate.CreateResult.Instance(it.instanceKey) }
        ?: EditDelegate.CreateResult.Task(taskKey)

@CheckResult
fun DomainUpdater.createTopLevelTask(
    notificationType: DomainListenerManager.NotificationType,
    createParameters: EditDelegate.CreateParameters,
    sharedProjectKey: ProjectKey.Shared?,
    copyTaskKey: TaskKey? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create("createTopLevelTask") { now ->
    updateProjectOrder(null, sharedProjectKey)

    val finalProjectId = sharedProjectKey ?: defaultProjectKey

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

        copyTaskKey?.let { copyTask(now, task, it) }
    }

    image?.upload(task.taskKey)

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
    sharedProjectParameters: EditDelegate.SharedProjectParameters?,
): Single<TaskKey.Root> = SingleDomainUpdate.create("updateScheduleTask") { now ->
    check(scheduleDatas.isNotEmpty())

    val image = createParameters.getImage(this)

    val projectKey = sharedProjectParameters?.key ?: defaultProjectKey

    val originalTask = getTaskForce(taskKey)
    originalTask.requireNotDeleted()

    updateProjectOrder(originalTask, sharedProjectParameters?.key)

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
                scheduleDatas.map { it to getTime(it.timePair) },
                now,
                sharedProjectParameters.nonNullAssignedTo,
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
    sharedProjectKey: ProjectKey.Shared?,
): Single<TaskKey.Root> = SingleDomainUpdate.create("updateTopLevelTask") { now ->
    val projectKey = sharedProjectKey ?: defaultProjectKey

    val originalTask = getTaskForce(taskKey)
    originalTask.requireNotDeleted()

    updateProjectOrder(originalTask, sharedProjectKey)

    val originalProject = originalTask.project

    lateinit var finalTask: RootTask
    trackRootTaskIds {
        finalTask = convertAndUpdateProject(originalTask, now, projectKey)

        finalTask.performRootIntervalUpdate {
            endAllCurrentTaskHierarchies(now)
            endAllCurrentSchedules(now)
            endAllCurrentNoScheduleOrParents(now)

            setNoScheduleOrParent(now, projectKey)
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
    sharedProjectParameters: EditDelegate.SharedProjectParameters?,
    joinAllInstances: Boolean,
): Single<TaskKey.Root> = SingleDomainUpdate.create("createScheduleJoinTopLevelTask") { now ->
    check(scheduleDatas.isNotEmpty())
    check(joinables.size > 1)

    updateProjectOrder(null, sharedProjectParameters?.key)

    val finalProjectKey = sharedProjectParameters?.key ?: defaultProjectKey

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
            scheduleDatas.map { it to getTime(it.timePair) },
            createParameters.note,
            finalProjectKey,
            image,
            this,
            ordinal,
            sharedProjectParameters.nonNullAssignedTo,
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
    sharedProjectKey: ProjectKey.Shared?,
    removeInstanceKeys: List<InstanceKey>,
): Single<TaskKey.Root> = SingleDomainUpdate.create("createJoinTopLevelTask") { now ->
    check(joinTaskKeys.size > 1)

    updateProjectOrder(null, sharedProjectKey)

    val initialJoinTasks = joinTaskKeys.map(::getTaskForce)

    val originalProjects = initialJoinTasks.map { it.project }

    val finalProjectKey = sharedProjectKey ?: initialJoinTasks.map { it.project }
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
): List<EditViewModel.ParentEntryData> {
    val parentTreeDatas = mutableListOf<EditViewModel.ParentEntryData>()

    parentTreeDatas += getAllTasks().asSequence()
        .filter { it.showAsParent(now, excludedTaskKeys) }
        .filter { it.isTopLevelTask() && (it.project as? SharedProject)?.notDeleted != true }
        .map { it.toParentEntryData(this, now, excludedTaskKeys, parentInstanceKey) }

    val projectOrder = Preferences.projectOrder

    parentTreeDatas += projectsFactory.sharedProjects
        .values
        .asSequence()
        .filter { it.notDeleted }
        .map {
            EditViewModel.ParentEntryData.Project(
                it.name,
                getProjectTaskTreeDatas(now, it, excludedTaskKeys, parentInstanceKey),
                it.projectKey,
                it.users.toUserDatas(),
                projectOrder.getOrDefault(it.projectKey, 0f),
            )
        }

    return parentTreeDatas
}

private fun DomainFactory.getProjectTaskTreeDatas(
    now: ExactTimeStamp.Local,
    project: Project<*>,
    excludedTaskKeys: Set<TaskKey>,
    parentInstanceKey: InstanceKey?,
): List<EditViewModel.ParentEntryData.Task> {
    return project.getAllDependenciesLoadedTasks()
        .filter { it.showAsParent(now, excludedTaskKeys) }
        .filter { it.isTopLevelTask() }
        .map { it.toParentEntryData(this, now, excludedTaskKeys, parentInstanceKey) }
        .toList()
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
) = EditViewModel.ParentEntryData.Task(
    name,
    domainFactory.getTaskListChildTaskDatas(now, this, excludedTaskKeys, parentInstanceKey),
    taskKey,
    getScheduleText(ScheduleText),
    note,
    EditViewModel.SortKey.TaskSortKey(startExactTimeStamp),
    project.projectKey,
    hasMultipleInstances(parentInstanceKey, now),
    topLevelTaskIsSingleSchedule(),
)

private fun DomainFactory.getTaskListChildTaskDatas(
    now: ExactTimeStamp.Local,
    parentTask: Task,
    excludedTaskKeys: Set<TaskKey>,
    parentInstanceKey: InstanceKey?,
): List<EditViewModel.ParentEntryData.Task> = parentTask.getChildTasks()
    .asSequence()
    .filter { it.showAsParent(now, excludedTaskKeys) }
    .map {
        it.toParentEntryData(
            this,
            now,
            excludedTaskKeys,
            parentInstanceKey,
        )
    }
    .toList()

private fun DomainFactory.copyTask(now: ExactTimeStamp.Local, task: RootTask, copyTaskKey: TaskKey) {
    val copiedTask = getTaskForce(copyTaskKey)

    copiedTask.getChildTasks().forEach {
        it.getImage(deviceDbInfo)?.let { check(it is ImageState.Remote) }

        createChildTask(
            now,
            task,
            it.name,
            it.note,
            it.imageJson,
            it.taskKey,
        )
    }
}

fun DomainFactory.createChildTask(
    now: ExactTimeStamp.Local,
    parentTask: RootTask,
    name: String,
    note: String?,
    imageJson: TaskJson.Image?,
    copyTaskKey: TaskKey? = null,
    ordinal: Ordinal? = null,
): RootTask {
    check(name.isNotEmpty())
    parentTask.requireNotDeleted()

    val childTask = parentTask.createChildTask(now, name, note, imageJson, ordinal)

    copyTaskKey?.let { copyTask(now, childTask, it) }

    return childTask
}

private fun Collection<ProjectUser>.toUserDatas() = associate {
    it.id to EditViewModel.UserData(it.id, it.name, it.photoUrl)
}

private val EditDelegate.SharedProjectParameters?.nonNullAssignedTo get() = this?.assignedTo.orEmpty()

fun DomainFactory.createScheduleTopLevelTask(
    now: ExactTimeStamp.Local,
    name: String,
    scheduleDatas: List<Pair<ScheduleData, Time>>,
    note: String?,
    projectKey: ProjectKey<*>,
    image: EditDelegate.CreateParameters.Image?,
    customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
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