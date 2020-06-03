package com.krystianwsul.checkme.domainmodel.extensions

import android.net.Uri
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.newUuid
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

@Synchronized
fun DomainFactory.getCreateTaskData(
        startParameters: EditViewModel.StartParameters,
        parentTaskKeyHint: TaskKey?
): EditViewModel.Data {
    MyCrashlytics.logMethod(this, "parentTaskKeyHint: $parentTaskKeyHint")

    val now = ExactTimeStamp.now

    val customTimes = getCurrentRemoteCustomTimes(now).associateBy {
        it.key
    }.toMutableMap<CustomTimeKey<*>, Time.Custom<*>>()

    val includeTaskKeys = listOfNotNull(parentTaskKeyHint).toMutableSet()

    fun checkHintPresent(
            task: EditViewModel.ParentKey.Task,
            parentTreeDatas: Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData>
    ): Boolean = parentTreeDatas.containsKey(task) || parentTreeDatas.any {
        checkHintPresent(task, it.value.parentTreeDatas)
    }

    fun checkHintPresent(
            parentTreeDatas: Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData>
    ) = parentTaskKeyHint?.let {
        checkHintPresent(EditViewModel.ParentKey.Task(it), parentTreeDatas)
    } ?: true

    val taskData = (startParameters as? EditViewModel.StartParameters.Task)?.let {
        val task = getTaskForce(it.taskKey)

        val parentKey: EditViewModel.ParentKey?
        var scheduleDataWrappers: List<EditViewModel.ScheduleDataWrapper>? = null

        if (task.isRootTask(now)) {
            val schedules = task.getCurrentSchedules(now)

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
                task.project.name,
                task.getImage(deviceDbInfo)
        )
    }

    val parentTreeDatas = getParentTreeDatas(now, startParameters.excludedTaskKeys, includeTaskKeys)

    check(checkHintPresent(parentTreeDatas))

    val customTimeDatas = customTimes.values.associate {
        it.key to EditViewModel.CustomTimeData(it.key, it.name, it.hourMinutes.toSortedMap())
    }

    val showAllInstancesDialog = (startParameters as? EditViewModel.StartParameters.Join)?.let {
        it.joinTaskKeys.any { getTaskForce(it).hasFutureReminders(now) }
    } ?: false

    return EditViewModel.Data(
            taskData,
            parentTreeDatas,
            customTimeDatas,
            myUserFactory.user.defaultReminder,
            showAllInstancesDialog
    )
}

@Synchronized
fun DomainFactory.createScheduleRootTask(
        dataId: Int,
        source: SaveService.Source,
        name: String,
        scheduleDatas: List<ScheduleData>,
        note: String?,
        projectId: ProjectKey<*>?,
        imagePath: Pair<String, Uri>?,
        copyTaskKey: TaskKey? = null
): TaskKey {
    MyCrashlytics.log("DomainFactory.createScheduleRootTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now

    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())

    val finalProjectId = projectId ?: defaultProjectId

    val imageUuid = imagePath?.let { newUuid() }

    val task = projectsFactory.createScheduleRootTask(
            now,
            name,
            scheduleDatas.map { it to getTime(it.timePair) },
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo
    )

    copyTaskKey?.let { copyTask(now, task, it) }

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(task.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath)
    }

    return task.taskKey
}

@Synchronized
fun DomainFactory.createChildTask(
        dataId: Int,
        source: SaveService.Source,
        parentTaskKey: TaskKey,
        name: String,
        note: String?,
        imagePath: Pair<String, Uri>?,
        copyTaskKey: TaskKey? = null
): TaskKey {
    MyCrashlytics.log("DomainFactory.createChildTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now

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

    save(dataId, source)

    notifyCloud(childTask.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, childTask.taskKey, it, imagePath)
    }

    return childTask.taskKey
}

@Synchronized
fun DomainFactory.createRootTask(
        dataId: Int,
        source: SaveService.Source,
        name: String,
        note: String?,
        projectId: ProjectKey<*>?,
        imagePath: Pair<String, Uri>?,
        copyTaskKey: TaskKey? = null
): TaskKey {
    MyCrashlytics.log("DomainFactory.createRootTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(name.isNotEmpty())

    val now = ExactTimeStamp.now

    val finalProjectId = projectId ?: defaultProjectId

    val imageUuid = imagePath?.let { newUuid() }

    val task = projectsFactory.createNoScheduleOrParentTask(
            now,
            name,
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo
    )

    copyTaskKey?.let { copyTask(now, task, it) }

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(task.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath)
    }

    return task.taskKey
}

@Synchronized
fun DomainFactory.updateScheduleTask(
        dataId: Int,
        source: SaveService.Source,
        taskKey: TaskKey,
        name: String,
        scheduleDatas: List<ScheduleData>,
        note: String?,
        projectId: ProjectKey<*>?,
        imagePath: NullableWrapper<Pair<String, Uri>>?
): TaskKey {
    MyCrashlytics.log("DomainFactory.updateScheduleTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())

    val now = ExactTimeStamp.now

    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())

    val imageUuid = imagePath?.value?.let { newUuid() }

    val task = getTaskForce(taskKey).also {
        it.requireCurrent(now)
        it.updateProject(this, now, projectId ?: defaultProjectId)
    }.apply {
        setName(name, note)

        endAllCurrentTaskHierarchies(now)
        endAllCurrentNoScheduleOrParents(now)

        if (isGroupTask(now)) {
            project.getTaskHierarchiesByParentTaskKey(taskKey).forEach { it.childTask.invalidateParentTaskHierarchies() }
            invalidateChildTaskHierarchies()
        }

        updateSchedules(ownerKey, localFactory, scheduleDatas.map { it to getTime(it.timePair) }, now)

        if (imagePath != null)
            setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })
    }

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(task.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value)
    }

    return task.taskKey
}

@Synchronized
fun DomainFactory.updateChildTask(
        now: ExactTimeStamp,
        dataId: Int,
        source: SaveService.Source,
        taskKey: TaskKey,
        name: String,
        parentTaskKey: TaskKey,
        note: String?,
        imagePath: NullableWrapper<Pair<String, Uri>>?
): TaskKey {
    MyCrashlytics.log("DomainFactory.updateChildTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(name.isNotEmpty())

    val task = getTaskForce(taskKey)
    task.requireCurrent(now)

    val newParentTask = getTaskForce(parentTaskKey)
    newParentTask.requireCurrent(now)

    task.setName(name, note)

    if (task.getParentTask(now) != newParentTask) {
        task.endAllCurrentTaskHierarchies(now)
        newParentTask.addChild(task, now)
    }

    task.endAllCurrentSchedules(now)
    task.endAllCurrentNoScheduleOrParents(now)

    val imageUuid = imagePath?.value?.let { newUuid() }
    if (imagePath != null)
        task.setImage(deviceDbInfo, imageUuid?.let { ImageState.Local(imageUuid) })

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(task.project) // todo image on server, purge images after this call

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value)
    }

    return task.taskKey
}


@Synchronized
fun DomainFactory.updateRootTask(
        dataId: Int,
        source: SaveService.Source,
        taskKey: TaskKey,
        name: String,
        note: String?,
        projectId: ProjectKey<*>?,
        imagePath: NullableWrapper<Pair<String, Uri>>?
): TaskKey {
    MyCrashlytics.log("DomainFactory.updateRootTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(name.isNotEmpty())

    val now = ExactTimeStamp.now

    val task = getTaskForce(taskKey).also {
        it.requireCurrent(now)
        it.updateProject(this, now, projectId ?: defaultProjectId)
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

    save(dataId, source)

    notifyCloud(task.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, task.taskKey, it, imagePath.value)
    }

    return task.taskKey
}

@Synchronized
fun DomainFactory.createScheduleJoinRootTask(
        now: ExactTimeStamp,
        dataId: Int,
        source: SaveService.Source,
        name: String,
        scheduleDatas: List<ScheduleData>,
        joinTaskKeys: List<TaskKey>,
        note: String?,
        projectId: ProjectKey<*>?,
        imagePath: Pair<String, Uri>?,
        removeInstanceKeys: List<InstanceKey>,
        allReminders: Boolean
): TaskKey {
    MyCrashlytics.log("DomainFactory.createScheduleJoinRootTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(name.isNotEmpty())
    check(scheduleDatas.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val finalProjectId = projectId ?: defaultProjectId

    val joinTasks = joinTaskKeys.map { getTaskForce(it).updateProject(this, now, finalProjectId) }

    val imageUuid = imagePath?.let { newUuid() }

    val newParentTask = projectsFactory.createScheduleRootTask(
            now,
            name,
            scheduleDatas.map { it to getTime(it.timePair) },
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo,
            allReminders
    )

    joinTasks(newParentTask, joinTasks, now, removeInstanceKeys, allReminders)

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(newParentTask.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath)
    }

    return newParentTask.taskKey
}

@Synchronized
fun DomainFactory.createJoinChildTask(
        dataId: Int,
        source: SaveService.Source,
        parentTaskKey: TaskKey,
        name: String,
        joinTaskKeys: List<TaskKey>,
        note: String?,
        imagePath: Pair<String, Uri>?,
        removeInstanceKeys: List<InstanceKey>
): TaskKey {
    MyCrashlytics.log("DomainFactory.createJoinChildTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(name.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val now = ExactTimeStamp.now

    val parentTask = getTaskForce(parentTaskKey)
    parentTask.requireCurrent(now)

    check(joinTaskKeys.map { it.projectKey }.distinct().size == 1)

    val joinTasks = joinTaskKeys.map { getTaskForce(it) }

    val imageUuid = imagePath?.let { newUuid() }

    val childTask = parentTask.createChildTask(now, name, note, imageUuid?.let { TaskJson.Image(it, uuid) })

    joinTasks(childTask, joinTasks, now, removeInstanceKeys)

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(childTask.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, childTask.taskKey, it, imagePath)
    }

    return childTask.taskKey
}

@Synchronized
fun DomainFactory.createJoinRootTask(
        dataId: Int,
        source: SaveService.Source,
        name: String,
        joinTaskKeys: List<TaskKey>,
        note: String?,
        projectId: ProjectKey<*>?,
        imagePath: Pair<String, Uri>?,
        removeInstanceKeys: List<InstanceKey>
): TaskKey {
    MyCrashlytics.log("DomainFactory.createJoinRootTask")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(name.isNotEmpty())
    check(joinTaskKeys.size > 1)

    val now = ExactTimeStamp.now

    val finalProjectId = projectId ?: joinTaskKeys.map { it.projectKey }
            .distinct()
            .single()

    val joinTasks = joinTaskKeys.map { getTaskForce(it).updateProject(this, now, finalProjectId) }

    val imageUuid = imagePath?.let { newUuid() }

    val newParentTask = projectsFactory.createNoScheduleOrParentTask(
            now,
            name,
            note,
            finalProjectId,
            imageUuid,
            deviceDbInfo
    )

    joinTasks(newParentTask, joinTasks, now, removeInstanceKeys)

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(newParentTask.project)

    imageUuid?.let {
        Uploader.addUpload(deviceDbInfo, newParentTask.taskKey, it, imagePath)
    }

    return newParentTask.taskKey
}