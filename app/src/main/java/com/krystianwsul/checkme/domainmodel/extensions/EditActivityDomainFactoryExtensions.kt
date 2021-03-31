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
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getCreateTaskData(
        startParameters: EditViewModel.StartParameters,
        currentParentSource: EditViewModel.CurrentParentSource,
): EditViewModel.Data {
    MyCrashlytics.logMethod(this)

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val customTimes = getCurrentRemoteCustomTimes(now).associateBy {
        it.key
    }.toMutableMap<CustomTimeKey<*>, Time.Custom<*>>()

    val taskData = (startParameters as? EditViewModel.StartParameters.Task)?.let {
        val task = getTaskForce(it.taskKey)

        val parentKey: EditViewModel.ParentKey?
        var scheduleDataWrappers: List<EditViewModel.ScheduleDataWrapper>? = null
        var assignedTo: Set<UserKey> = setOf()

        if (task.isRootTask(now)) {
            val schedules = task.getCurrentScheduleIntervals(now)

            customTimes += schedules.mapNotNull { it.schedule.customTimeKey }.map {
                it to task.project.getCustomTime(it.customTimeId)
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
                        .toSet()
        )
    }

    val parentTreeDatas = getParentTreeDatas(now, startParameters.excludedTaskKeys)

    val customTimeDatas = customTimes.values.associate {
        it.key to EditViewModel.CustomTimeData(it.key, it.name, it.hourMinutes.toSortedMap())
    }

    val showAllInstancesDialog = when (startParameters) {
        is EditViewModel.StartParameters.Join -> startParameters.joinables.run {
            map { it.taskKey.projectKey }.distinct().size == 1 && any {
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

            if (task.isRootTask(now)) {
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

    return EditViewModel.Data(
            taskData,
            parentTreeDatas,
            customTimeDatas,
            showAllInstancesDialog,
            currentParent,
    )
}

@CheckResult
fun DomainUpdater.createScheduleRootTask(
        notificationType: DomainListenerManager.NotificationType,
        name: String,
        scheduleDatas: List<ScheduleData>,
        note: String?,
        sharedProjectParameters: EditDelegate.SharedProjectParameters?,
        imagePath: Pair<String, Uri>?,
        copyTaskKey: TaskKey? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.createScheduleRootTask")

    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())

    val finalProjectId = sharedProjectParameters?.key ?: defaultProjectId

    val imageUuid = imagePath?.let { newUuid() }

    val task = projectsFactory.createScheduleRootTask(
            now,
            name,
            scheduleDatas.map { it to getTime(it.timePair) },
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo,
            assignedTo = sharedProjectParameters.nonNullAssignedTo
    )

    copyTaskKey?.let { copyTask(now, task, it) }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath) }

    DomainUpdater.Result(
            task.toCreateResult(now),
            true,
            notificationType,
            DomainFactory.CloudParams(task.project),
    )
}.perform(this)

private fun <T : ProjectType> Task<T>.toCreateResult(now: ExactTimeStamp.Local) =
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
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.createChildTask")

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
            copyTaskKey
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
fun DomainUpdater.createRootTask(
        notificationType: DomainListenerManager.NotificationType,
        name: String,
        note: String?,
        sharedProjectKey: ProjectKey.Shared?,
        imagePath: Pair<String, Uri>?,
        copyTaskKey: TaskKey? = null,
): Single<EditDelegate.CreateResult> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.createRootTask")

    check(name.isNotEmpty())

    val finalProjectId = sharedProjectKey ?: defaultProjectId

    val imageUuid = imagePath?.let { newUuid() }

    val task = projectsFactory.createNoScheduleOrParentTask(
            now,
            name,
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo,
    )

    copyTaskKey?.let { copyTask(now, task, it) }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath) }

    DomainUpdater.Result(task.toCreateResult(now), true, notificationType, DomainFactory.CloudParams(task.project))
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
): Single<TaskKey> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.updateScheduleTask")

    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())

    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())

    val imageUuid = imagePath?.value?.let { newUuid() }

    val task = getTaskForce(taskKey).let {
        it.requireCurrent(now)
        it.updateProject(this, now, sharedProjectParameters?.key ?: defaultProjectId)
    }.apply {
        setName(name, note)

        endAllCurrentTaskHierarchies(now)
        endAllCurrentNoScheduleOrParents(now)

        updateSchedules(
                ownerKey,
                localFactory,
                scheduleDatas.map { it to getTime(it.timePair) },
                now,
                sharedProjectParameters.nonNullAssignedTo
        )

        if (imagePath != null) setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })
    }

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value) }

    DomainUpdater.Result(task.taskKey, true, notificationType, DomainFactory.CloudParams(task.project))
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
): Single<TaskKey> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.updateChildTask")

    check(name.isNotEmpty())

    val task = getTaskForce(taskKey)
    task.requireCurrent(now)

    val newParentTask = getTaskForce(parentTaskKey)
    newParentTask.requireCurrent(now)

    task.setName(name, note)

    tailrec fun Task<*>.hasAncestor(taskKey: TaskKey): Boolean {
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
fun DomainUpdater.updateRootTask(
        notificationType: DomainListenerManager.NotificationType,
        taskKey: TaskKey,
        name: String,
        note: String?,
        sharedProjectKey: ProjectKey.Shared?,
        imagePath: NullableWrapper<Pair<String, Uri>>?,
): Single<TaskKey> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.updateRootTask")

    check(name.isNotEmpty())

    val task = getTaskForce(taskKey).also {
        it.requireCurrent(now)
        it.updateProject(this, now, sharedProjectKey ?: defaultProjectId)
    }.apply {
        setName(name, note)

        endAllCurrentTaskHierarchies(now)
        endAllCurrentSchedules(now)
        endAllCurrentNoScheduleOrParents(now)

        setNoScheduleOrParent(now)
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
fun DomainUpdater.createScheduleJoinRootTask(
        notificationType: DomainListenerManager.NotificationType,
        name: String,
        scheduleDatas: List<ScheduleData>,
        joinables: List<EditParameters.Join.Joinable>,
        note: String?,
        sharedProjectParameters: EditDelegate.SharedProjectParameters?,
        imagePath: Pair<String, Uri>?,
        allReminders: Boolean,
): Single<TaskKey> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.createScheduleJoinRootTask")

    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())
    check(joinables.size > 1)

    val finalProjectId = sharedProjectParameters?.key ?: defaultProjectId

    val joinableTaskKeys = joinables.map { it.taskKey }

    val joinTasks = if (allReminders) {
        joinableTaskKeys.map { getTaskForce(it).updateProject(this, now, finalProjectId) }
    } else {
        check(
                joinableTaskKeys.map { it.projectKey }
                        .distinct()
                        .single() == finalProjectId
        )

        joinableTaskKeys.map { getTaskForce(it) }
    }

    val ordinal = joinTasks.map { it.ordinal }.minOrNull()

    val imageUuid = imagePath?.let { newUuid() }

    val newParentTask = projectsFactory.createScheduleRootTask(
            now,
            name,
            scheduleDatas.map { it to getTime(it.timePair) },
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo,
            ordinal,
            assignedTo = sharedProjectParameters.nonNullAssignedTo
    )

    if (allReminders)
        joinTasks(newParentTask, joinTasks, now, joinables.mapNotNull { it.instanceKey })
    else
        joinJoinables(newParentTask, joinables, now)

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
): Single<TaskKey> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.createJoinChildTask")

    check(name.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val parentTask = getTaskForce(parentTaskKey)
    parentTask.requireCurrent(now)

    check(joinTaskKeys.map { it.projectKey }.distinct().size == 1)

    val joinTasks = joinTaskKeys.map { getTaskForce(it) }

    val ordinal = joinTasks.map { it.ordinal }.minOrNull()

    val imageUuid = imagePath?.let { newUuid() }

    val childTask = parentTask.createChildTask(
            now,
            name,
            note,
            imageUuid?.let { TaskJson.Image(it, uuid) },
            ordinal
    )

    joinTasks(childTask, joinTasks, now, removeInstanceKeys)

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, childTask.taskKey, it, imagePath) }

    DomainUpdater.Result(childTask.taskKey, true, notificationType, DomainFactory.CloudParams(childTask.project))
}.perform(this)

@CheckResult
fun DomainUpdater.createJoinRootTask(
        notificationType: DomainListenerManager.NotificationType,
        name: String,
        joinTaskKeys: List<TaskKey>,
        note: String?,
        sharedProjectKey: ProjectKey.Shared?,
        imagePath: Pair<String, Uri>?,
        removeInstanceKeys: List<InstanceKey>,
): Single<TaskKey> = SingleDomainUpdate.create { now ->
    MyCrashlytics.log("DomainFactory.createJoinRootTask")

    check(name.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val finalProjectId = sharedProjectKey ?: joinTaskKeys.map { it.projectKey }
            .distinct()
            .single()

    val joinTasks = joinTaskKeys.map { getTaskForce(it).updateProject(this, now, finalProjectId) }

    val ordinal = joinTasks.map { it.ordinal }.minOrNull()

    val imageUuid = imagePath?.let { newUuid() }

    val newParentTask = projectsFactory.createNoScheduleOrParentTask(
            now,
            name,
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo,
            ordinal,
    )

    joinTasks(newParentTask, joinTasks, now, removeInstanceKeys)

    imageUuid?.let { Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath) }

    DomainUpdater.Result(
            newParentTask.taskKey,
            true,
            notificationType,
            DomainFactory.CloudParams(newParentTask.project),
    )
}.perform(this)

private fun DomainFactory.getParentTreeDatas(
        now: ExactTimeStamp.Local,
        excludedTaskKeys: Set<TaskKey>,
): Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData> {
    val parentTreeDatas = mutableMapOf<EditViewModel.ParentKey, EditViewModel.ParentTreeData>()

    parentTreeDatas += projectsFactory.privateProject
            .tasks
            .asSequence()
            .filter { it.showAsParent(now, excludedTaskKeys) }
            .filter { it.isRootTask(now) }
            .associate {
                val taskParentKey = EditViewModel.ParentKey.Task(it.taskKey)

                val parentTreeData = EditViewModel.ParentTreeData(
                        it.name,
                        getTaskListChildTaskDatas(now, it, excludedTaskKeys),
                        taskParentKey,
                        it.getScheduleText(ScheduleText, now),
                        it.note,
                        EditViewModel.SortKey.TaskSortKey(it.startExactTimeStamp),
                        null,
                        mapOf(),
                )

                taskParentKey to parentTreeData
            }

    parentTreeDatas += projectsFactory.sharedProjects
            .values
            .asSequence()
            .filter { it.current(now) }
            .associate {
                val projectParentKey = EditViewModel.ParentKey.Project(it.projectKey)

                val users = it.users.joinToString(", ") { it.name }
                val parentTreeData = EditViewModel.ParentTreeData(
                        it.name,
                        getProjectTaskTreeDatas(now, it, excludedTaskKeys),
                        projectParentKey,
                        users,
                        null,
                        EditViewModel.SortKey.ProjectSortKey(it.projectKey),
                        it.projectKey,
                        it.users.toUserDatas(),
                )

                projectParentKey to parentTreeData
            }

    return parentTreeDatas
}

private fun DomainFactory.getProjectTaskTreeDatas(
        now: ExactTimeStamp.Local,
        project: Project<*>,
        excludedTaskKeys: Set<TaskKey>,
): Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData> {
    return project.tasks
            .asSequence()
            .filter { it.showAsParent(now, excludedTaskKeys) }
            .filter { it.isRootTask(now) }
            .associate {
                val taskParentKey = EditViewModel.ParentKey.Task(it.taskKey)

                val parentTreeData = EditViewModel.ParentTreeData(
                        it.name,
                        getTaskListChildTaskDatas(now, it, excludedTaskKeys),
                        taskParentKey,
                        it.getScheduleText(ScheduleText, now),
                        it.note,
                        EditViewModel.SortKey.TaskSortKey(it.startExactTimeStamp),
                        (it.project as? SharedProject)?.projectKey,
                        mapOf(),
                )

                taskParentKey to parentTreeData
            }
}

private fun Task<*>.showAsParent(
        now: ExactTimeStamp.Local,
        excludedTaskKeys: Set<TaskKey>,
): Boolean {
    if (!current(now)) return false

    if (excludedTaskKeys.contains(taskKey)) return false

    if (!isVisible(now)) return false

    return true
}

private fun DomainFactory.joinJoinables(
        newParentTask: Task<*>,
        joinables: List<EditParameters.Join.Joinable>,
        now: ExactTimeStamp.Local,
) {
    check(joinables.map { it.taskKey.projectKey }.distinct().size == 1)

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

        fun addChildToParent(instance: Instance<*>? = null) = addChildToParent(task, newParentTask, now, instance)

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
        newParentTask: Task<*>,
        joinTasks: List<Task<*>>,
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
        parentTask: Task<*>,
        excludedTaskKeys: Set<TaskKey>,
): Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData> =
        parentTask.getChildTaskHierarchies(now)
                .asSequence()
                .map { it.childTask }
                .filter { it.showAsParent(now, excludedTaskKeys) }
                .associate { childTask ->
                    val taskParentKey = EditViewModel.ParentKey.Task(childTask.taskKey)

                    val parentTreeData = EditViewModel.ParentTreeData(
                            childTask.name,
                            getTaskListChildTaskDatas(now, childTask, excludedTaskKeys),
                            EditViewModel.ParentKey.Task(childTask.taskKey),
                            childTask.getScheduleText(ScheduleText, childTask.getHierarchyExactTimeStamp(now)),
                            childTask.note,
                            EditViewModel.SortKey.TaskSortKey(childTask.startExactTimeStamp),
                            (childTask.project as? SharedProject)?.projectKey,
                            mapOf(),
                    )

                    taskParentKey to parentTreeData
                }

private fun DomainFactory.copyTask(now: ExactTimeStamp.Local, task: Task<*>, copyTaskKey: TaskKey) {
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

private fun <T : ProjectType> DomainFactory.createChildTask(
        now: ExactTimeStamp.Local,
        parentTask: Task<T>,
        name: String,
        note: String?,
        imageJson: TaskJson.Image?,
        copyTaskKey: TaskKey? = null,
): Task<T> {
    check(name.isNotEmpty())
    parentTask.requireCurrent(now)

    val childTask = parentTask.createChildTask(now, name, note, imageJson)

    copyTaskKey?.let { copyTask(now, childTask, it) }

    return childTask
}

private fun Collection<ProjectUser>.toUserDatas() = associate {
    it.id to EditViewModel.UserData(it.id, it.name, it.photoUrl)
}

private val EditDelegate.SharedProjectParameters?.nonNullAssignedTo get() = this?.assignedTo.orEmpty()