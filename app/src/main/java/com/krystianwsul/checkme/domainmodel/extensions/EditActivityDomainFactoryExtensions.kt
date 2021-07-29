package com.krystianwsul.checkme.domainmodel.extensions

import android.net.Uri
import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.gui.edit.ParentScheduleManager
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.newUuid
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.MyCustomTime
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.task.*
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

    val now = ExactTimeStamp.Local.now

    val customTimeDatas = myUserFactory.user
        .customTimes
        .values
        .filter { it.notDeleted(now) }
        .associateBy { it.key }
        .toMutableMap<CustomTimeKey, Time.Custom>()
        .toCustomTimeDatas()

    return EditViewModel.MainData(null, customTimeDatas, null, null)
}

private fun DomainFactory.getCreateTaskDataSlow(
    startParameters: EditViewModel.StartParameters,
    currentParentSource: EditViewModel.CurrentParentSource,
): EditViewModel.MainData {
    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val customTimes = getCurrentRemoteCustomTimes(now).associate { it.key to it as Time.Custom }.toMutableMap()

    val taskData = (startParameters as? EditViewModel.StartParameters.Task)?.let {
        val task = getTaskForce(it.taskKey)

        val parentKey: EditViewModel.ParentKey?
        var scheduleDataWrappers: List<EditViewModel.ScheduleDataWrapper>? = null
        var assignedTo: Set<UserKey> = setOf()

        if (task.isTopLevelTask(now)) {
            val schedules = task.intervalInfo.getCurrentScheduleIntervals(now)

            customTimes += schedules.mapNotNull { it.schedule.customTimeKey }.map { it to getCustomTime(it) }

            parentKey = task.project
                .projectKey
                .let {
                    (it as? ProjectKey.Shared)?.let { EditViewModel.ParentKey.Project(it) }
                }

            if (schedules.isNotEmpty()) {
                scheduleDataWrappers = ScheduleGroup.getGroups(schedules.map { it.schedule }).map {
                    EditViewModel.ScheduleDataWrapper.fromScheduleData(it.scheduleData)
                }

                assignedTo = schedules.map { it.schedule.assignedTo }
                    .distinct()
                    .single()
            }
        } else {
            val parentTask = task.getParentTask(now)!!
            parentKey = EditViewModel.ParentKey.Task(parentTask.taskKey)
        }

        EditViewModel.TaskData(
            task.name,
            parentKey,
            scheduleDataWrappers,
            task.note,
            task.getImage(deviceDbInfo),
            task.project
                .getAssignedTo(assignedTo)
                .map { it.key }
                .toSet(),
            task.project.projectKey,
        )
    }

    val customTimeDatas = customTimes.toCustomTimeDatas()

    val showAllInstancesDialog = startParameters.showAllInstancesDialog(this, now)

    val currentParentKey: EditViewModel.ParentKey? = when (currentParentSource) {
        is EditViewModel.CurrentParentSource.None -> null
        is EditViewModel.CurrentParentSource.Set -> currentParentSource.parentKey
        is EditViewModel.CurrentParentSource.FromTask -> {
            val task = getTaskForce(currentParentSource.taskKey)

            if (task.isTopLevelTask(now)) {
                when (val projectKey = task.project.projectKey) {
                    is ProjectKey.Private -> null
                    is ProjectKey.Shared -> EditViewModel.ParentKey.Project(projectKey)
                    else -> throw UnsupportedOperationException()
                }
            } else {
                task.getParentTask(now)?.let { EditViewModel.ParentKey.Task(it.taskKey) }
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

    val currentParent: ParentScheduleManager.Parent? = when (currentParentKey) {
        is EditViewModel.ParentKey.Task -> {
            val task = getTaskForce(currentParentKey.taskKey)

            ParentScheduleManager.Parent(
                task.name,
                EditViewModel.ParentKey.Task(task.taskKey),
                mapOf(),
                task.project.projectKey,
            )
        }
        is EditViewModel.ParentKey.Project -> {
            val project = projectsFactory.sharedProjects.getValue(currentParentKey.projectId)

            ParentScheduleManager.Parent(
                project.name,
                EditViewModel.ParentKey.Project(project.projectKey),
                project.users.toUserDatas(),
                project.projectKey,
            )
        }
        null -> null
    }

    return EditViewModel.MainData(taskData, customTimeDatas, showAllInstancesDialog, currentParent)
}

fun DomainFactory.getCreateTaskParentPickerData(
    startParameters: EditViewModel.StartParameters,
): EditViewModel.ParentPickerData {
    MyCrashlytics.logMethod(this)

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val parentTreeDatas = getParentTreeDatas(now, startParameters.excludedTaskKeys)

    return EditViewModel.ParentPickerData(parentTreeDatas)
}

@CheckResult
fun DomainUpdater.createScheduleTopLevelTask(
    notificationType: DomainListenerManager.NotificationType,
    name: String,
    scheduleDatas: List<ScheduleData>,
    note: String?,
    sharedProjectParameters: EditDelegate.SharedProjectParameters?,
    imagePath: Pair<String, Uri>?,
    copyTaskKey: TaskKey? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create("createScheduleTopLevelTask") { now ->
    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())

    val finalProjectId = sharedProjectParameters?.key ?: defaultProjectKey

    val imageUuid = imagePath?.let { newUuid() }

    lateinit var task: RootTask
    trackRootTaskIds {
        task = createScheduleTopLevelTask(
            now,
            name,
            scheduleDatas.map { it to getTime(it.timePair) },
            note,
            finalProjectId,
            imageUuid,
            this,
            assignedTo = sharedProjectParameters.nonNullAssignedTo,
        )

        copyTaskKey?.let { copyTask(now, task, it) }
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath) }

    DomainUpdater.Result(
        task.toCreateResult(now),
        true,
        notificationType,
        DomainFactory.CloudParams(task.project),
    )
}.perform(this)

private fun RootTask.toCreateResult(now: ExactTimeStamp.Local) =
    getInstances(null, null, now).singleOrNull()
        ?.let { EditDelegate.CreateResult.Instance(it.instanceKey) }
        ?: EditDelegate.CreateResult.Task(taskKey)

@CheckResult
fun DomainUpdater.createChildTask(
    notificationType: DomainListenerManager.NotificationType,
    parentTaskKey: TaskKey,
    name: String,
    note: String?,
    imagePath: Pair<String, Uri>?,
    copyTaskKey: TaskKey? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create("createChildTask") { now ->
    check(name.isNotEmpty())

    val imageUuid = imagePath?.let { newUuid() }

    lateinit var childTask: RootTask
    trackRootTaskIds {
        val parentTask = convertToRoot(getTaskForce(parentTaskKey), now)
        parentTask.requireCurrent(now)

        childTask = createChildTask(
            now,
            parentTask,
            name,
            note,
            imageUuid?.let { TaskJson.Image(it, uuid) },
            copyTaskKey,
        )
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, childTask.taskKey, it, imagePath) }

    DomainUpdater.Result(
        childTask.toCreateResult(now),
        true,
        notificationType,
        DomainFactory.CloudParams(childTask.project),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.createTopLevelTask(
    notificationType: DomainListenerManager.NotificationType,
    name: String,
    note: String?,
    sharedProjectKey: ProjectKey.Shared?,
    imagePath: Pair<String, Uri>?,
    copyTaskKey: TaskKey? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create("createTopLevelTask") { now ->
    check(name.isNotEmpty())

    val finalProjectId = sharedProjectKey ?: defaultProjectKey

    val imageUuid = imagePath?.let { newUuid() }

    lateinit var task: RootTask
    trackRootTaskIds {
        task = createNoScheduleOrParentTask(
            now,
            name,
            note,
            finalProjectId,
            imageUuid,
        )

        copyTaskKey?.let { copyTask(now, task, it) }
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath) }

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
    name: String,
    scheduleDatas: List<ScheduleData>,
    note: String?,
    sharedProjectParameters: EditDelegate.SharedProjectParameters?,
    imagePath: NullableWrapper<Pair<String, Uri>>?,
): Single<TaskKey.Root> = SingleDomainUpdate.create("updateScheduleTask") { now ->
    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())

    val imageUuid = imagePath?.value?.let { newUuid() }

    val projectKey = sharedProjectParameters?.key ?: defaultProjectKey

    val originalTask = getTaskForce(taskKey)
    originalTask.requireCurrent(now)

    val originalProject = originalTask.project

    lateinit var finalTask: RootTask
    trackRootTaskIds {
        finalTask = convertAndUpdateProject(originalTask, now, projectKey)

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
            )
        }
    }

    finalTask.apply {
        setName(name, note)

        if (imagePath != null) setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, finalTask.taskKey, it, imagePath.value) }

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
    name: String,
    parentTaskKey: TaskKey,
    note: String?,
    imagePath: NullableWrapper<Pair<String, Uri>>?,
    removeInstanceKey: InstanceKey?,
    allReminders: Boolean,
): Single<TaskKey.Root> = SingleDomainUpdate.create("updateChildTask") { now ->
    check(name.isNotEmpty())

    lateinit var task: RootTask
    lateinit var originalProject: Project<*>
    lateinit var parentTask: RootTask
    trackRootTaskIds {
        task = convertToRoot(getTaskForce(taskKey), now)
        task.requireCurrent(now)

        originalProject = task.project

        parentTask = convertToRoot(getTaskForce(parentTaskKey), now)
        parentTask.requireCurrent(now)

        tailrec fun Task.hasAncestor(taskKey: TaskKey): Boolean {
            val currParentTask = getParentTask(now) ?: return false

            if (currParentTask.taskKey == taskKey) return true

            return currParentTask.hasAncestor(taskKey)
        }

        check(!parentTask.hasAncestor(taskKey))

        task.performRootIntervalUpdate {
            if (task.getParentTask(now) != parentTask) {
                if (allReminders) endAllCurrentTaskHierarchies(now)

                parentTask.addChild(this, now)
            }

            if (allReminders) {
                endAllCurrentSchedules(now)
                endAllCurrentNoScheduleOrParents(now)
            }
        }

    }

    task.setName(name, note)

    val imageUuid = imagePath?.value?.let { newUuid() }
    if (imagePath != null) task.setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })

    removeInstanceKey?.let {
        val instance = getInstance(it)

        if (instance.parentInstance?.task != parentTask
            && instance.isVisible(now, Instance.VisibilityOptions(hack24 = true))
        ) {
            instance.hide()
        }
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value) }

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
    name: String,
    note: String?,
    sharedProjectKey: ProjectKey.Shared?,
    imagePath: NullableWrapper<Pair<String, Uri>>?,
): Single<TaskKey.Root> = SingleDomainUpdate.create("updateTopLevelTask") { now ->
    check(name.isNotEmpty())

    val projectKey = sharedProjectKey ?: defaultProjectKey

    val originalTask = getTaskForce(taskKey)
    originalTask.requireCurrent(now)

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

    finalTask.setName(name, note)

    val imageUuid = imagePath?.value?.let { newUuid() }

    if (imagePath != null) finalTask.setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, finalTask.taskKey, it, imagePath.value) }

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
    name: String,
    scheduleDatas: List<ScheduleData>,
    joinables: List<EditParameters.Join.Joinable>,
    note: String?,
    sharedProjectParameters: EditDelegate.SharedProjectParameters?,
    imagePath: Pair<String, Uri>?,
    allReminders: Boolean,
): Single<TaskKey.Root> = SingleDomainUpdate.create("createScheduleJoinTopLevelTask") { now ->
    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())
    check(joinables.size > 1)

    val finalProjectKey = sharedProjectParameters?.key ?: defaultProjectKey

    val imageUuid = imagePath?.let { newUuid() }

    lateinit var newParentTask: RootTask
    trackRootTaskIds {
        val joinableMap = if (allReminders) {
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
            name,
            scheduleDatas.map { it to getTime(it.timePair) },
            note,
            finalProjectKey,
            imageUuid,
            this,
            ordinal,
            sharedProjectParameters.nonNullAssignedTo,
        )

        if (allReminders)
            joinTasks(newParentTask, joinableMap.map { it.second }, now, joinables.mapNotNull { it.instanceKey })
        else
            joinJoinables(newParentTask, joinableMap, now)
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath) }

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
    name: String,
    joinTaskKeys: List<TaskKey>,
    note: String?,
    imagePath: Pair<String, Uri>?,
    removeInstanceKeys: List<InstanceKey>,
): Single<TaskKey.Root> = SingleDomainUpdate.create("createJoinChildTask") { now ->
    check(name.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val imageUuid = imagePath?.let { newUuid() }

    lateinit var childTask: RootTask
    trackRootTaskIds {
        val parentTask = convertToRoot(getTaskForce(parentTaskKey), now)
        parentTask.requireCurrent(now)

        val joinTasks = joinTaskKeys.map { convertToRoot(getTaskForce(it), now) }

        val ordinal = joinTasks.map { it.ordinal }.minOrNull()

        childTask = createChildTask(
            now,
            parentTask,
            name,
            note,
            imageUuid?.let { TaskJson.Image(it, uuid) },
            ordinal = ordinal,
        )

        joinTasks(childTask, joinTasks, now, removeInstanceKeys)
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, childTask.taskKey, it, imagePath) }

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
    name: String,
    joinTaskKeys: List<TaskKey>,
    note: String?,
    sharedProjectKey: ProjectKey.Shared?,
    imagePath: Pair<String, Uri>?,
    removeInstanceKeys: List<InstanceKey>,
): Single<TaskKey.Root> = SingleDomainUpdate.create("createJoinTopLevelTask") { now ->
    check(name.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val initialJoinTasks = joinTaskKeys.map(::getTaskForce)

    val originalProjects = initialJoinTasks.map { it.project }

    val finalProjectId = sharedProjectKey ?: initialJoinTasks.map { it.project }
        .distinct()
        .single()
        .projectKey

    val imageUuid = imagePath?.let { newUuid() }

    lateinit var newParentTask: RootTask
    trackRootTaskIds {
        val joinTasks = joinTaskKeys.map { convertAndUpdateProject(getTaskForce(it), now, finalProjectId) }

        val ordinal = joinTasks.map { it.ordinal }.minOrNull()

        newParentTask = createNoScheduleOrParentTask(
            now,
            name,
            note,
            finalProjectId,
            imageUuid,
            ordinal,
        )

        joinTasks(newParentTask, joinTasks, now, removeInstanceKeys)
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath) }

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
): List<EditViewModel.ParentTreeData> {
    val parentTreeDatas = mutableListOf<EditViewModel.ParentTreeData>()

    parentTreeDatas += getAllTasks().asSequence()
        .filter { it.showAsParent(now, excludedTaskKeys) }
        .filter { it.isTopLevelTask(now) && (it.project as? SharedProject)?.current(now) != true }
        .map {
            EditViewModel.ParentTreeData(
                it.name,
                getTaskListChildTaskDatas(now, it, excludedTaskKeys),
                EditViewModel.ParentKey.Task(it.taskKey),
                it.getScheduleText(ScheduleText, now),
                it.note,
                EditViewModel.SortKey.TaskSortKey(it.startExactTimeStamp),
                mapOf(),
                it.project.projectKey,
            )
        }

    parentTreeDatas += projectsFactory.sharedProjects
        .values
        .asSequence()
        .filter { it.current(now) }
        .map {
            EditViewModel.ParentTreeData(
                it.name,
                getProjectTaskTreeDatas(now, it, excludedTaskKeys),
                EditViewModel.ParentKey.Project(it.projectKey),
                it.users.joinToString(", ") { it.name },
                null,
                EditViewModel.SortKey.ProjectSortKey(it.projectKey),
                it.users.toUserDatas(),
                it.projectKey,
            )
        }

    return parentTreeDatas
}

private fun DomainFactory.getProjectTaskTreeDatas(
    now: ExactTimeStamp.Local,
    project: Project<*>,
    excludedTaskKeys: Set<TaskKey>,
): List<EditViewModel.ParentTreeData> {
    return project.getAllTasks()
        .filter { it.showAsParent(now, excludedTaskKeys) }
        .filter { it.isTopLevelTask(now) }
        .map {
            EditViewModel.ParentTreeData(
                it.name,
                getTaskListChildTaskDatas(now, it, excludedTaskKeys),
                EditViewModel.ParentKey.Task(it.taskKey),
                it.getScheduleText(ScheduleText, now),
                it.note,
                EditViewModel.SortKey.TaskSortKey(it.startExactTimeStamp),
                mapOf(),
                it.project.projectKey,
            )
        }
        .toList()
}

private fun Task.showAsParent(now: ExactTimeStamp.Local, excludedTaskKeys: Set<TaskKey>): Boolean {
    if (!current(now)) return false

    if (excludedTaskKeys.contains(taskKey)) return false

    if (!isVisible(now)) return false

    return true
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

    val parentTaskHasOtherInstances = newParentTask.hasOtherVisibleInstances(now, parentInstanceKey)

    joinableMap.forEach { (joinable, task) ->
        fun addChildToParent(instance: Instance? = null) = addChildToParent(task, newParentTask, now, instance)

        when (joinable) {
            is EditParameters.Join.Joinable.Task -> addChildToParent()
            is EditParameters.Join.Joinable.Instance -> {
                val migratedScheduleKey = joinable.instanceKey
                    .scheduleKey
                    .run {
                        val originalTime = getTime(scheduleTimePair)

                        val migratedTime = task.getOrCopyTime(
                            scheduleDate.dayOfWeek,
                            originalTime,
                            this@joinJoinables,
                            now,
                        )

                        ScheduleKey(scheduleDate, migratedTime.timePair)
                    }

                val instance = task.getInstance(migratedScheduleKey)

                if (parentTaskHasOtherInstances || task.hasOtherVisibleInstances(now, joinable.instanceKey)) {
                    instance.setParentState(Instance.ParentState.Parent(parentInstanceKey))
                } else {
                    addChildToParent(instance)
                }
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
    newParentTask.requireCurrent(now)
    check(joinTasks.size > 1)

    joinTasks.forEach { addChildToParent(it, newParentTask, now) }

    removeInstanceKeys.map(::getInstance)
        .filter {
            it.parentInstance?.task != newParentTask &&
                    it.isVisible(now, Instance.VisibilityOptions(hack24 = true))
        }
        .forEach { it.hide() }
}

private fun DomainFactory.getTaskListChildTaskDatas(
    now: ExactTimeStamp.Local,
    parentTask: Task,
    excludedTaskKeys: Set<TaskKey>,
): List<EditViewModel.ParentTreeData> =
    parentTask.getChildTaskHierarchies(now)
        .asSequence()
        .map { it.childTask }
        .filter { it.showAsParent(now, excludedTaskKeys) }
        .map { childTask ->
            EditViewModel.ParentTreeData(
                childTask.name,
                getTaskListChildTaskDatas(now, childTask, excludedTaskKeys),
                EditViewModel.ParentKey.Task(childTask.taskKey),
                childTask.getScheduleText(ScheduleText, childTask.getHierarchyExactTimeStamp(now)),
                childTask.note,
                EditViewModel.SortKey.TaskSortKey(childTask.startExactTimeStamp),
                mapOf(),
                childTask.project.projectKey,
            )
        }
        .toList()

private fun DomainFactory.copyTask(now: ExactTimeStamp.Local, task: RootTask, copyTaskKey: TaskKey) {
    val copiedTask = getTaskForce(copyTaskKey)

    copiedTask.getChildTaskHierarchies(now).forEach {
        val copiedChildTask = it.childTask
        copiedChildTask.getImage(deviceDbInfo)?.let { check(it is ImageState.Remote) }

        createChildTask(
            now,
            task,
            copiedChildTask.name,
            copiedChildTask.note,
            copiedChildTask.imageJson,
            copiedChildTask.taskKey,
        )
    }
}

private fun DomainFactory.createChildTask(
    now: ExactTimeStamp.Local,
    parentTask: RootTask,
    name: String,
    note: String?,
    imageJson: TaskJson.Image?,
    copyTaskKey: TaskKey? = null,
    ordinal: Double? = null,
): RootTask {
    check(name.isNotEmpty())
    parentTask.requireCurrent(now)

    val childTask = parentTask.createChildTask(now, name, note, imageJson, ordinal)

    copyTaskKey?.let { copyTask(now, childTask, it) }

    return childTask
}

private fun Collection<ProjectUser>.toUserDatas() = associate {
    it.id to EditViewModel.UserData(it.id, it.name, it.photoUrl)
}

private val EditDelegate.SharedProjectParameters?.nonNullAssignedTo get() = this?.assignedTo.orEmpty()

private fun DomainFactory.createScheduleTopLevelTask(
    now: ExactTimeStamp.Local,
    name: String,
    scheduleDatas: List<Pair<ScheduleData, Time>>,
    note: String?,
    projectKey: ProjectKey<*>,
    imageUuid: String?,
    customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
    ordinal: Double? = null,
    assignedTo: Set<UserKey> = setOf(),
) = createRootTask(now, imageUuid, name, note, ordinal).apply {
    createSchedules(now, scheduleDatas, assignedTo, customTimeMigrationHelper, projectKey)
}

private fun DomainFactory.getTaskJsonImage(imageUuid: String) = TaskJson.Image(imageUuid, deviceDbInfo.uuid)

private fun DomainFactory.createNoScheduleOrParentTask(
    now: ExactTimeStamp.Local,
    name: String,
    note: String?,
    projectKey: ProjectKey<*>,
    imageUuid: String?,
    ordinal: Double? = null,
) = createRootTask(now, imageUuid, name, note, ordinal).apply {
    performRootIntervalUpdate { setNoScheduleOrParent(now, projectKey) }
}

private fun DomainFactory.createRootTask(
    now: ExactTimeStamp.Local,
    imageUuid: String?,
    name: String,
    note: String?,
    ordinal: Double?,
): RootTask {
    return rootTasksFactory.createTask(
        now,
        imageUuid?.let(::getTaskJsonImage),
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
    return when (task) {
        is RootTask -> task.updateProject(projectKey)
        is ProjectTask -> converter.convertToRoot(now, task, projectKey)
    }
}

private fun DomainFactory.convertToRoot(task: Task, now: ExactTimeStamp.Local): RootTask {
    if (task is RootTask) return task

    return converter.convertToRoot(now, task as ProjectTask, task.project.projectKey)
}