package com.krystianwsul.checkme.domainmodel.extensions

import android.net.Uri
import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.takeAndHasMore
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.gui.edit.ParentScheduleManager
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.newUuid
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.MyCustomTime
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getCreateTaskData(
        startParameters: EditViewModel.StartParameters,
        currentParentSource: EditViewModel.CurrentParentSource,
): EditViewModel.MainData {
    MyCrashlytics.logMethod(this)

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val customTimes = getCurrentRemoteCustomTimes(now).associate { it.key to it as Time.Custom }.toMutableMap()

    val taskData = (startParameters as? EditViewModel.StartParameters.Task)?.let {
        val task = getTaskForce(it.taskKey)

        val parentKey: EditViewModel.ParentKey?
        var scheduleDataWrappers: List<EditViewModel.ScheduleDataWrapper>? = null
        var assignedTo: Set<UserKey> = setOf()

        if (task.isTopLevelTask(now)) {
            val schedules = task.getCurrentScheduleIntervals(now)

            customTimes += schedules.mapNotNull { it.schedule.customTimeKey }.map {
                it to getCustomTime(it)
            }

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
        )
    }

    val customTimeDatas = customTimes.values.associate {
        it.key to EditViewModel.CustomTimeData(it.key, it.name, it.hourMinutes.toSortedMap(), it is MyCustomTime)
    }

    val showAllInstancesDialog = when (startParameters) {
        is EditViewModel.StartParameters.Join -> startParameters.joinables.run {
            map { (it.taskKey as TaskKey.Project).projectKey }.distinct().size == 1 && any { // todo task edit
                getTaskForce(it.taskKey).let { task ->
                    if (it.instanceKey != null) {
                        task.hasOtherVisibleInstances(now, it.instanceKey)
                    } else {
                        task.getInstances(null, null, now)
                                .filter { it.isVisible(now, Instance.VisibilityOptions()) }
                                .takeAndHasMore(1)
                                .second
                    }
                }
            }
        }
        is EditViewModel.StartParameters.Task -> null
        is EditViewModel.StartParameters.Create -> null
    }

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
    }

    val currentParent: ParentScheduleManager.Parent? = when (currentParentKey) {
        is EditViewModel.ParentKey.Task -> {
            val task = getTaskForce(currentParentKey.taskKey)

            ParentScheduleManager.Parent(
                    task.name,
                    EditViewModel.ParentKey.Task(task.taskKey),
                    mapOf(),
            )
        }
        is EditViewModel.ParentKey.Project -> {
            val project = projectsFactory.sharedProjects.getValue(currentParentKey.projectId)

            ParentScheduleManager.Parent(
                    project.name,
                    EditViewModel.ParentKey.Project(project.projectKey),
                    project.users.toUserDatas(),
            )
        }
        null -> null
    }

    return EditViewModel.MainData(
            taskData,
            customTimeDatas,
            showAllInstancesDialog,
            currentParent,
    )
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

    val finalProjectId = sharedProjectParameters?.key ?: defaultProjectId

    val imageUuid = imagePath?.let { newUuid() }

    val task = createScheduleTopLevelTask(
            now,
            name,
            scheduleDatas.map { it to getTime(it.timePair) },
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo,
            this,
            assignedTo = sharedProjectParameters.nonNullAssignedTo,
    )

    copyTaskKey?.let { copyTask(now, task as ProjectTask, it) } // todo task copy

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath) }

    DomainUpdater.Result(
            task.toCreateResult(now),
            true,
            notificationType,
            DomainFactory.CloudParams(task.project),
    )
}.perform(this)

private fun Task.toCreateResult(now: ExactTimeStamp.Local) =
        getInstances(null, null, now).singleOrNull()
                ?.let { EditDelegate.CreateResult.Instance(it.instanceKey) }
                ?: EditDelegate.CreateResult.Task(taskKey)

@CheckResult
fun DomainUpdater.createChildTask(
        // todo task convert
        notificationType: DomainListenerManager.NotificationType,
        parentTaskKey: TaskKey,
        name: String,
        note: String?,
        imagePath: Pair<String, Uri>?,
        copyTaskKey: TaskKey? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create("createChildTask") { now ->
    check(name.isNotEmpty())

    val parentTask = getTaskForce(parentTaskKey)
    parentTask.requireCurrent(now)

    val imageUuid = imagePath?.let { newUuid() }

    val childTask = createChildTask(
            now,
            parentTask,
            name,
            note,
            imageUuid?.let { TaskJson.Image(it, uuid) },
            copyTaskKey,
    )

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
        // todo task convert
        notificationType: DomainListenerManager.NotificationType,
        name: String,
        note: String?,
        sharedProjectKey: ProjectKey.Shared?,
        imagePath: Pair<String, Uri>?,
        copyTaskKey: TaskKey? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create("createTopLevelTask") { now ->
    check(name.isNotEmpty())

    val finalProjectId = sharedProjectKey ?: defaultProjectId

    val imageUuid = imagePath?.let { newUuid() }

    val task = createNoScheduleOrParentTask(
            now,
            name,
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo,
    )

    copyTaskKey?.let { copyTask(now, task as ProjectTask, it) } // todo task copy

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
        // todo task convert
        notificationType: DomainListenerManager.NotificationType,
        taskKey: TaskKey,
        name: String,
        scheduleDatas: List<ScheduleData>,
        note: String?,
        sharedProjectParameters: EditDelegate.SharedProjectParameters?,
        imagePath: NullableWrapper<Pair<String, Uri>>?,
): Single<TaskKey> = SingleDomainUpdate.create("updateScheduleTask") { now ->
    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())

    val imageUuid = imagePath?.value?.let { newUuid() }

    val task = getTaskForce(taskKey).let {
        it.requireCurrent(now)
        convertAndUpdateProject(it, now, sharedProjectParameters?.key ?: defaultProjectId)
    }.apply {
        setName(name, note)

        endAllCurrentTaskHierarchies(now)
        endAllCurrentNoScheduleOrParents(now)

        updateSchedules(
                ownerKey,
                localFactory,
                scheduleDatas.map { it to getTime(it.timePair) },
                now,
                sharedProjectParameters.nonNullAssignedTo,
                this@create,
                null, // todo task edit
        )

        if (imagePath != null) setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value) }

    DomainUpdater.Result(task.taskKey, true, notificationType, DomainFactory.CloudParams(task.project))
}.perform(this)

@CheckResult
fun DomainUpdater.updateChildTask(
        // todo task convert
        notificationType: DomainListenerManager.NotificationType,
        taskKey: TaskKey,
        name: String,
        parentTaskKey: TaskKey,
        note: String?,
        imagePath: NullableWrapper<Pair<String, Uri>>?,
        removeInstanceKey: InstanceKey?,
        allReminders: Boolean,
): Single<TaskKey> = SingleDomainUpdate.create("updateChildTask") { now ->
    check(name.isNotEmpty())

    val task = getTaskForce(taskKey)
    task.requireCurrent(now)

    val newParentTask = getTaskForce(parentTaskKey)
    newParentTask.requireCurrent(now)

    task.setName(name, note)

    tailrec fun Task.hasAncestor(taskKey: TaskKey): Boolean {
        val parentTask = getParentTask(now) ?: return false

        if (parentTask.taskKey == taskKey) return true

        return parentTask.hasAncestor(taskKey)
    }

    check(!newParentTask.hasAncestor(taskKey))

    if (task.getParentTask(now) != newParentTask) {
        if (allReminders) task.endAllCurrentTaskHierarchies(now)

        newParentTask.addChild(task, now)
    }

    if (allReminders) {
        task.endAllCurrentSchedules(now)
        task.endAllCurrentNoScheduleOrParents(now)
    }

    val imageUuid = imagePath?.value?.let { newUuid() }
    if (imagePath != null) task.setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })

    removeInstanceKey?.let {
        val instance = getInstance(it)

        if (instance.parentInstance?.task != newParentTask
                && instance.isVisible(now, Instance.VisibilityOptions(hack24 = true))
        ) {
            instance.hide()
        }
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value) }

    DomainUpdater.Result(task.taskKey, true, notificationType, DomainFactory.CloudParams(task.project))
}.perform(this)

@CheckResult
fun DomainUpdater.updateTopLevelTask(
        // todo task convert
        notificationType: DomainListenerManager.NotificationType,
        taskKey: TaskKey,
        name: String,
        note: String?,
        sharedProjectKey: ProjectKey.Shared?,
        imagePath: NullableWrapper<Pair<String, Uri>>?,
): Single<TaskKey> = SingleDomainUpdate.create("updateTopLevelTask") { now ->
    check(name.isNotEmpty())

    val task = getTaskForce(taskKey).also {
        it.requireCurrent(now)
        convertAndUpdateProject(it, now, sharedProjectKey ?: defaultProjectId)
    }.apply {
        setName(name, note)

        endAllCurrentTaskHierarchies(now)
        endAllCurrentSchedules(now)
        endAllCurrentNoScheduleOrParents(now)

        setNoScheduleOrParent(now, null) // todo task edit
    }

    val imageUuid = imagePath?.value?.let { newUuid() }
    if (imagePath != null)
        task.setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value)
    }

    DomainUpdater.Result(task.taskKey, true, notificationType, DomainFactory.CloudParams(task.project))
}.perform(this)

@CheckResult
fun DomainUpdater.createScheduleJoinTopLevelTask(
        // todo task convert
        notificationType: DomainListenerManager.NotificationType,
        name: String,
        scheduleDatas: List<ScheduleData>,
        joinables: List<EditParameters.Join.Joinable>,
        note: String?,
        sharedProjectParameters: EditDelegate.SharedProjectParameters?,
        imagePath: Pair<String, Uri>?,
        allReminders: Boolean,
): Single<TaskKey> = SingleDomainUpdate.create("createScheduleJoinTopLevelTask") { now ->
    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())
    check(joinables.size > 1)

    val finalProjectId = sharedProjectParameters?.key ?: defaultProjectId

    val joinableTaskKeys = joinables.map { it.taskKey }

    val joinTasks = if (allReminders) {
        joinableTaskKeys.map { convertAndUpdateProject(getTaskForce(it), now, finalProjectId) }
    } else {
        check(
                joinableTaskKeys.map { (it as TaskKey.Project).projectKey } // todo task join
                        .distinct()
                        .single() == finalProjectId
        )

        joinableTaskKeys.map { getTaskForce(it) }
    }

    val ordinal = joinTasks.map { it.ordinal }.minOrNull()

    val imageUuid = imagePath?.let { newUuid() }

    val newParentTask = createScheduleTopLevelTask(
            now,
            name,
            scheduleDatas.map { it to getTime(it.timePair) },
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo,
            this,
            ordinal,
            sharedProjectParameters.nonNullAssignedTo,
    )

    if (allReminders)
        joinTasks(newParentTask as ProjectTask, joinTasks, now, joinables.mapNotNull { it.instanceKey }) // todo task join
    else
        joinJoinables(newParentTask as ProjectTask, joinables, now) // todo task join

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath) }

    DomainUpdater.Result(
            newParentTask.taskKey as TaskKey, // todo task join
            true,
            notificationType,
            DomainFactory.CloudParams(newParentTask.project),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.createJoinChildTask(
        // todo task convert
        notificationType: DomainListenerManager.NotificationType,
        parentTaskKey: TaskKey,
        name: String,
        joinTaskKeys: List<TaskKey>,
        note: String?,
        imagePath: Pair<String, Uri>?,
        removeInstanceKeys: List<InstanceKey>,
): Single<TaskKey> = SingleDomainUpdate.create("createJoinChildTask") { now ->
    check(name.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val parentTask = getTaskForce(parentTaskKey)
    parentTask.requireCurrent(now)

    check(joinTaskKeys.map { (it as TaskKey.Project).projectKey }.distinct().size == 1) // todo task join

    val joinTasks = joinTaskKeys.map { getTaskForce(it) }

    val ordinal = joinTasks.map { it.ordinal }.minOrNull()

    val imageUuid = imagePath?.let { newUuid() }

    val childTask = createChildTask(
            now,
            parentTask,
            name,
            note,
            imageUuid?.let { TaskJson.Image(it, uuid) },
            ordinal = ordinal,
    )

    joinTasks(childTask as ProjectTask, joinTasks, now, removeInstanceKeys) // todo task join

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, childTask.taskKey, it, imagePath) }

    DomainUpdater.Result(
            childTask.taskKey as TaskKey, // todo task join
            true,
            notificationType,
            DomainFactory.CloudParams(childTask.project),
    )
}.perform(this)

@CheckResult
fun DomainUpdater.createJoinTopLevelTask(
        // todo task convert
        notificationType: DomainListenerManager.NotificationType,
        name: String,
        joinTaskKeys: List<TaskKey>,
        note: String?,
        sharedProjectKey: ProjectKey.Shared?,
        imagePath: Pair<String, Uri>?,
        removeInstanceKeys: List<InstanceKey>,
): Single<TaskKey> = SingleDomainUpdate.create("createJoinTopLevelTask") { now ->
    check(name.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val finalProjectId = sharedProjectKey
            ?: joinTaskKeys.map { (it as TaskKey.Project).projectKey } // todo task join
                    .distinct()
                    .single()

    val joinTasks = joinTaskKeys.map { convertAndUpdateProject(getTaskForce(it), now, finalProjectId) }

    val ordinal = joinTasks.map { it.ordinal }.minOrNull()

    val imageUuid = imagePath?.let { newUuid() }

    val newParentTask = createNoScheduleOrParentTask(
            now,
            name,
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo,
            ordinal,
    )

    joinTasks(newParentTask as ProjectTask, joinTasks, now, removeInstanceKeys) // todo task join

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath) }

    DomainUpdater.Result(
            newParentTask.taskKey as TaskKey, // todo task after edit
            true,
            notificationType,
            DomainFactory.CloudParams(newParentTask.project),
    )
}.perform(this)

private fun DomainFactory.getParentTreeDatas(
        now: ExactTimeStamp.Local,
        excludedTaskKeys: Set<TaskKey>,
): List<EditViewModel.ParentTreeData> {
    val parentTreeDatas = mutableListOf<EditViewModel.ParentTreeData>()

    parentTreeDatas += getAllTasks().asSequence()
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
        newParentTask: ProjectTask,
        joinables: List<EditParameters.Join.Joinable>,
        now: ExactTimeStamp.Local,
) {
    check(joinables.map { (it.taskKey as TaskKey.Project).projectKey }.distinct().size == 1) // todo task after edit

    val parentInstanceKey = newParentTask.getInstances(
            null,
            null,
            now,
    )
            .single()
            .instanceKey

    val parentTaskHasOtherInstances = newParentTask.hasOtherVisibleInstances(now, parentInstanceKey)

    joinables.forEach { joinable ->
        val task = getTaskForce(joinable.taskKey)

        fun addChildToParent(instance: Instance? = null) = addChildToParent(task, newParentTask, now, instance)

        when (joinable) {
            is EditParameters.Join.Joinable.Task -> addChildToParent()
            is EditParameters.Join.Joinable.Instance -> {
                val instance = task.getInstance(joinable.instanceKey.scheduleKey)

                if (parentTaskHasOtherInstances || task.hasOtherVisibleInstances(now, joinable.instanceKey)) {
                    getInstance(joinable.instanceKey).setParentState(Instance.ParentState.Parent(parentInstanceKey))
                } else {
                    addChildToParent(instance)
                }
            }
        }
    }
}

private fun DomainFactory.joinTasks(
        newParentTask: ProjectTask,
        joinTasks: List<Task>,
        now: ExactTimeStamp.Local,
        removeInstanceKeys: List<InstanceKey>,
) {
    newParentTask.requireCurrent(now)
    check(joinTasks.size > 1)

    joinTasks.forEach { addChildToParent(it, newParentTask, now) }

    removeInstanceKeys.map(::getInstance)
            .filter {
                it.parentInstance?.task != newParentTask
                        && it.isVisible(now, Instance.VisibilityOptions(hack24 = true))
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
                    )
                }
                .toList()

private fun DomainFactory.copyTask(now: ExactTimeStamp.Local, task: ProjectTask, copyTaskKey: TaskKey) {
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
                copiedChildTask.taskKey
        )
    }
}

private fun DomainFactory.createChildTask(
        now: ExactTimeStamp.Local,
        parentTask: Task,
        name: String,
        note: String?,
        imageJson: TaskJson.Image?,
        copyTaskKey: TaskKey? = null,
        ordinal: Double? = null,
): Task {
    check(name.isNotEmpty())
    parentTask.requireCurrent(now)

    val childTask = parentTask.createChildTask(now, name, note, imageJson, ordinal)

    copyTaskKey?.let { copyTask(now, childTask as ProjectTask, it) } // todo task copy

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
        deviceDbInfo: DeviceDbInfo,
        customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        ordinal: Double? = null,
        assignedTo: Set<UserKey> = setOf(),
): Task {
    return if (Task.WRITE_ROOT_TASKS) {
        createRootTask(now, imageUuid, name, note, ordinal, projectKey).apply {
            createSchedules(deviceDbInfo.key, now, scheduleDatas, assignedTo, customTimeMigrationHelper, projectKey)

            projectsFactory.getProjectForce(projectKey).addRootTask(taskKey)
        }
    } else {
        projectsFactory.createScheduleTopLevelTask(
                now,
                name,
                scheduleDatas,
                note,
                projectKey,
                imageUuid,
                deviceDbInfo,
                customTimeMigrationHelper,
                ordinal,
                assignedTo,
        )
    }
}

private fun DomainFactory.getTaskJsonImage(imageUuid: String) = TaskJson.Image(imageUuid, deviceDbInfo.uuid)

private fun DomainFactory.createNoScheduleOrParentTask(
        now: ExactTimeStamp.Local,
        name: String,
        note: String?,
        projectKey: ProjectKey<*>,
        imageUuid: String?,
        deviceDbInfo: DeviceDbInfo,
        ordinal: Double? = null,
): Task {
    return if (Task.WRITE_ROOT_TASKS) {
        createRootTask(now, imageUuid, name, note, ordinal, projectKey).apply {
            setNoScheduleOrParent(now, projectKey)

            projectsFactory.getProjectForce(projectKey).addRootTask(taskKey)
        }
    } else {
        projectsFactory.createNoScheduleOrParentTask(
                now,
                name,
                note,
                projectKey,
                imageUuid,
                deviceDbInfo,
                ordinal,
        )
    }
}

private fun DomainFactory.createRootTask(
        now: ExactTimeStamp.Local,
        imageUuid: String?,
        name: String,
        note: String?,
        ordinal: Double?,
        projectKey: ProjectKey<*>,
): RootTask {
    val task = rootTasksFactory.createTask(
            now,
            imageUuid?.let(::getTaskJsonImage),
            name,
            note,
            ordinal,
    )

    projectsFactory.getProjectForce(projectKey).addRootTask(task.taskKey)

    return task
}

private fun DomainFactory.convertAndUpdateProject(
        task: Task,
        now: ExactTimeStamp.Local,
        projectKey: ProjectKey<*>,
): Task {
    return if (Task.WRITE_ROOT_TASKS) {
        when (task) {
            is RootTask -> task.updateProject(this, now, projectKey)
            is ProjectTask -> converter.convertToRoot(now, task, projectKey)
            else -> throw UnsupportedOperationException()
        }
    } else {
        if (task.project.projectKey == projectKey) return task

        task.updateProject(this, now, projectKey)
    }
}