package com.krystianwsul.checkme.domainmodel.extensions

import android.net.Uri
import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.scheduleOnDomain
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.takeAndHasMore
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.newUuid
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.SchedulerType
import com.krystianwsul.common.firebase.SchedulerTypeHolder
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getCreateTaskData(
        startParameters: EditViewModel.StartParameters,
        parentTaskKeyHint: TaskKey?,
): EditViewModel.Data {
    MyCrashlytics.logMethod(this, "parentTaskKeyHint: $parentTaskKeyHint")

    SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

    val now = ExactTimeStamp.Local.now

    val customTimes = getCurrentRemoteCustomTimes(now).associateBy {
        it.key
    }.toMutableMap<CustomTimeKey<*>, Time.Custom<*>>()

    val includeTaskKeys = listOfNotNull(parentTaskKeyHint).toMutableSet()

    fun checkHintPresent(
            task: EditViewModel.ParentKey.Task,
            parentTreeDatas: Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData>,
    ): Boolean = parentTreeDatas.containsKey(task) || parentTreeDatas.any {
        checkHintPresent(task, it.value.parentTreeDatas)
    }

    fun checkHintPresent(
            parentTreeDatas: Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData>,
    ) = parentTaskKeyHint?.let {
        checkHintPresent(EditViewModel.ParentKey.Task(it), parentTreeDatas)
    } ?: true

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
            includeTaskKeys.add(parentTask.taskKey)
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

    val parentTreeDatas = getParentTreeDatas(now, startParameters.excludedTaskKeys, includeTaskKeys)

    check(checkHintPresent(parentTreeDatas))

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

    return EditViewModel.Data(
            taskData,
            parentTreeDatas,
            customTimeDatas,
            myUserFactory.user.defaultReminder,
            showAllInstancesDialog
    )
}

@CheckResult
fun DomainFactory.createScheduleRootTask(
        source: SaveService.Source,
        name: String,
        scheduleDatas: List<ScheduleData>,
        note: String?,
        sharedProjectParameters: EditDelegate.SharedProjectParameters?,
        imagePath: Pair<String, Uri>?,
        copyTaskKey: TaskKey? = null,
        now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): Single<EditDelegate.CreateResult> = scheduleOnDomain {
    MyCrashlytics.log("DomainFactory.createScheduleRootTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

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

    updateNotifications(now)

    save(null, source)

    notifyCloud(task.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath)
    }

    task.toCreateResult(now)
}

private fun <T : ProjectType> Task<T>.toCreateResult(now: ExactTimeStamp.Local) =
        getInstances(null, null, now).singleOrNull()
                ?.let { EditDelegate.CreateResult.Instance(it.instanceKey) }
                ?: EditDelegate.CreateResult.Task(taskKey)

@CheckResult
fun DomainFactory.createChildTask(
        source: SaveService.Source,
        parentTaskKey: TaskKey,
        name: String,
        note: String?,
        imagePath: Pair<String, Uri>?,
        copyTaskKey: TaskKey? = null,
        now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): Single<EditDelegate.CreateResult> = scheduleOnDomain {
    MyCrashlytics.log("DomainFactory.createChildTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

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

    updateNotifications(now)

    save(null, source)

    notifyCloud(childTask.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, childTask.taskKey, it, imagePath)
    }

    childTask.toCreateResult(now)
}

@CheckResult
fun DomainFactory.createRootTask(
        source: SaveService.Source,
        name: String,
        note: String?,
        sharedProjectKey: ProjectKey.Shared?,
        imagePath: Pair<String, Uri>?,
        copyTaskKey: TaskKey? = null,
        now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): Single<EditDelegate.CreateResult> = scheduleOnDomain {
    MyCrashlytics.log("DomainFactory.createRootTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

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

    updateNotifications(now)

    save(null, source)

    notifyCloud(task.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath)
    }

    task.toCreateResult(now)
}

@CheckResult
fun DomainFactory.updateScheduleTask(
        source: SaveService.Source,
        taskKey: TaskKey,
        name: String,
        scheduleDatas: List<ScheduleData>,
        note: String?,
        sharedProjectParameters: EditDelegate.SharedProjectParameters?,
        imagePath: NullableWrapper<Pair<String, Uri>>?,
        now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): Single<TaskKey> = scheduleOnDomain {
    MyCrashlytics.log("DomainFactory.updateScheduleTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

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

    updateNotifications(now)

    save(null, source)

    notifyCloud(task.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value)
    }

    task.taskKey
}

@CheckResult
fun DomainFactory.updateChildTask(
        source: SaveService.Source,
        taskKey: TaskKey,
        name: String,
        parentTaskKey: TaskKey,
        note: String?,
        imagePath: NullableWrapper<Pair<String, Uri>>?,
        removeInstanceKey: InstanceKey?,
        allReminders: Boolean,
        now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): Single<TaskKey> = scheduleOnDomain {
    MyCrashlytics.log("DomainFactory.updateChildTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

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

    updateNotifications(now)

    save(null, source)

    notifyCloud(task.project) // todo image on server, purge images after this call

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value)
    }

    task.taskKey
}

@CheckResult
fun DomainFactory.updateRootTask(
        source: SaveService.Source,
        taskKey: TaskKey,
        name: String,
        note: String?,
        sharedProjectKey: ProjectKey.Shared?,
        imagePath: NullableWrapper<Pair<String, Uri>>?,
        now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): Single<TaskKey> = scheduleOnDomain {
    MyCrashlytics.log("DomainFactory.updateRootTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

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

    updateNotifications(now)

    save(null, source)

    notifyCloud(task.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value)
    }

    task.taskKey
}

@CheckResult
fun DomainFactory.createScheduleJoinRootTask(
        source: SaveService.Source,
        name: String,
        scheduleDatas: List<ScheduleData>,
        joinables: List<EditParameters.Join.Joinable>,
        note: String?,
        sharedProjectParameters: EditDelegate.SharedProjectParameters?,
        imagePath: Pair<String, Uri>?,
        allReminders: Boolean,
        now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): Single<TaskKey> = scheduleOnDomain {
    MyCrashlytics.log("DomainFactory.createScheduleJoinRootTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

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

    updateNotifications(now)

    save(null, source)

    notifyCloud(newParentTask.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath)
    }

    newParentTask.taskKey
}

@CheckResult
fun DomainFactory.createJoinChildTask(
        source: SaveService.Source,
        parentTaskKey: TaskKey,
        name: String,
        joinTaskKeys: List<TaskKey>,
        note: String?,
        imagePath: Pair<String, Uri>?,
        removeInstanceKeys: List<InstanceKey>,
): Single<TaskKey> = scheduleOnDomain {
    MyCrashlytics.log("DomainFactory.createJoinChildTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(name.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val now = ExactTimeStamp.Local.now

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

    updateNotifications(now)

    save(null, source)

    notifyCloud(childTask.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, childTask.taskKey, it, imagePath)
    }

    childTask.taskKey
}

@CheckResult
fun DomainFactory.createJoinRootTask(
        source: SaveService.Source,
        name: String,
        joinTaskKeys: List<TaskKey>,
        note: String?,
        sharedProjectKey: ProjectKey.Shared?,
        imagePath: Pair<String, Uri>?,
        removeInstanceKeys: List<InstanceKey>,
): Single<TaskKey> = scheduleOnDomain {
    MyCrashlytics.log("DomainFactory.createJoinRootTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(name.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val now = ExactTimeStamp.Local.now

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

    updateNotifications(now)

    save(null, source)

    notifyCloud(newParentTask.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath)
    }

    newParentTask.taskKey
}

private fun DomainFactory.getParentTreeDatas(
        now: ExactTimeStamp.Local,
        excludedTaskKeys: Set<TaskKey>,
        includedTaskKeys: Set<TaskKey>,
): Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData> {
    val parentTreeDatas = mutableMapOf<EditViewModel.ParentKey, EditViewModel.ParentTreeData>()

    parentTreeDatas += projectsFactory.privateProject
            .tasks
            .asSequence()
            .filter { it.showAsParent(now, excludedTaskKeys, includedTaskKeys) }
            .filter { it.isRootTask(now) }
            .associate {
                val taskParentKey = EditViewModel.ParentKey.Task(it.taskKey)

                val parentTreeData = EditViewModel.ParentTreeData(
                        it.name,
                        getTaskListChildTaskDatas(now, it, excludedTaskKeys, includedTaskKeys),
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
                        getProjectTaskTreeDatas(now, it, excludedTaskKeys, includedTaskKeys),
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
        includedTaskKeys: Set<TaskKey>,
): Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData> {
    return project.tasks
            .asSequence()
            .filter { it.showAsParent(now, excludedTaskKeys, includedTaskKeys) }
            .filter { it.isRootTask(now) }
            .associate {
                val taskParentKey = EditViewModel.ParentKey.Task(it.taskKey)

                val parentTreeData = EditViewModel.ParentTreeData(
                        it.name,
                        getTaskListChildTaskDatas(now, it, excludedTaskKeys, includedTaskKeys),
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
        includedTaskKeys: Set<TaskKey>,
): Boolean {
    check(excludedTaskKeys.intersect(includedTaskKeys).isEmpty())

    if (!current(now)) {
        check(!includedTaskKeys.contains(taskKey))

        return false
    }

    if (excludedTaskKeys.contains(taskKey)) return false

    if (includedTaskKeys.contains(taskKey)) {
        check(this.isVisible(now, true)) // exception for editing task that's child of done task

        return true
    }

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
        includedTaskKeys: Set<TaskKey>,
): Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData> =
        parentTask.getChildTaskHierarchies(now)
                .asSequence()
                .map { it.childTask }
                .filter { it.showAsParent(now, excludedTaskKeys, includedTaskKeys) }
                .associate { childTask ->
                    val taskParentKey = EditViewModel.ParentKey.Task(childTask.taskKey)

                    val parentTreeData = EditViewModel.ParentTreeData(
                            childTask.name,
                            getTaskListChildTaskDatas(now, childTask, excludedTaskKeys, includedTaskKeys),
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