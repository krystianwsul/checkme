package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ProjectUndoData
import com.krystianwsul.common.domain.RemoteToRemoteConversion
import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.TaskHierarchyJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.interrupt.throwIfInterrupted
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

abstract class Project<T : ProjectType> : Current {

    abstract val projectRecord: ProjectRecord<T>

    @Suppress("PropertyName")
    protected abstract val _tasks: MutableMap<String, Task<T>>
    protected abstract val taskHierarchyContainer: TaskHierarchyContainer<T>
    protected abstract val remoteCustomTimes: Map<out CustomTimeId<T>, Time.Custom<T>>

    abstract val projectKey: ProjectKey<T>

    var name
        get() = projectRecord.name
        set(name) {
            check(name.isNotEmpty())

            projectRecord.name = name
        }

    override val startExactTimeStamp by lazy {
        ExactTimeStamp.fromOffset(projectRecord.startTime, projectRecord.startTimeOffset)
    }

    override val endExactTimeStamp
        get() = projectRecord.endTime?.let {
            ExactTimeStamp.fromOffset(it, projectRecord.endTimeOffset)
        }

    // don't want these to be mutable
    val taskIds: Set<String> get() = _tasks.keys
    val tasks: Collection<Task<T>> get() = _tasks.values

    abstract val customTimes: Collection<Time.Custom<T>>

    val taskHierarchies get() = taskHierarchyContainer.all

    val existingInstances get() = tasks.flatMap { it.existingInstances.values }

    protected abstract fun newRootInstanceManager(taskRecord: TaskRecord<T>): RootInstanceManager<T>

    fun newTask(taskJson: TaskJson): Task<T> {
        val taskRecord = projectRecord.newTaskRecord(taskJson)

        val task = Task(
                this,
                taskRecord,
                newRootInstanceManager(taskRecord)
        )
        check(!_tasks.containsKey(task.id))
        _tasks[task.id] = task

        return task
    }

    fun createTaskHierarchy(
            parentTask: Task<T>,
            childTask: Task<T>,
            now: ExactTimeStamp
    ) {
        val taskHierarchyJson = TaskHierarchyJson(
                parentTask.id,
                childTask.id,
                now.long
        )

        val taskHierarchyRecord = projectRecord.newTaskHierarchyRecord(taskHierarchyJson)

        val taskHierarchy = TaskHierarchy(this, taskHierarchyRecord)

        taskHierarchyContainer.add(taskHierarchy.id, taskHierarchy)
        taskHierarchy.invalidateTasks()
    }

    @Suppress("ConstantConditionIf")
    fun copyTask(
            deviceDbInfo: DeviceDbInfo,
            oldTask: Task<*>,
            instances: Collection<Instance<*>>,
            now: ExactTimeStamp
    ): Task<T> {
        val endTime = oldTask.endExactTimeStamp?.long

        val instanceDatas = instances.map { it to getInstanceJson(deviceDbInfo.key, it) }

        val instanceJsons = if (Task.USE_ROOT_INSTANCES) {
            mutableMapOf()
        } else {
            instanceDatas.associate {
                InstanceRecord.scheduleKeyToString(it.first.scheduleKey) to it.second
            }.toMutableMap()
        }

        val taskJson = TaskJson(
                oldTask.name,
                now.long,
                now.offset,
                endTime,
                oldTask.note,
                instanceJsons,
                ordinal = oldTask.ordinal
        )

        val taskRecord = projectRecord.newTaskRecord(taskJson)

        val newTask = Task(this, taskRecord, newRootInstanceManager(taskRecord))
        check(!_tasks.containsKey(newTask.id))

        if (Task.USE_ROOT_INSTANCES) {
            instanceDatas.forEach {
                newTask.rootInstanceManager.newRootInstanceRecord(
                        it.second,
                        it.first.scheduleKey,
                        getOrCopyAndDestructureTime(deviceDbInfo.key, it.first.scheduleTime).first
                )
            }
        }

        _tasks[newTask.id] = newTask

        val currentSchedules = oldTask.getCurrentSchedules(now).map { it.schedule }
        val currentNoScheduleOrParent =
                oldTask.getCurrentNoScheduleOrParent(now)?.noScheduleOrParent

        if (currentSchedules.isNotEmpty()) {
            check(currentNoScheduleOrParent == null)

            newTask.copySchedules(deviceDbInfo, now, currentSchedules)
        } else {
            currentNoScheduleOrParent?.let { newTask.setNoScheduleOrParent(now) }
        }

        return newTask
    }

    protected abstract fun getOrCreateCustomTime(
            ownerKey: UserKey,
            customTime: Time.Custom<*>
    ): Time.Custom<T>

    fun getOrCopyTime(ownerKey: UserKey, time: Time) = time.let {
        when (it) {
            is Time.Custom<*> -> getOrCreateCustomTime(ownerKey, it)
            is Time.Normal -> it
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getOrCopyAndDestructureTime(
            ownerKey: UserKey,
            time: Time
    ) = when (val newTime = getOrCopyTime(ownerKey, time)) {
        is Time.Custom<*> -> Triple(newTime.key.customTimeId as CustomTimeId<T>, null, null)
        is Time.Normal -> Triple(null, newTime.hourMinute.hour, newTime.hourMinute.minute)
    }

    private fun getInstanceJson(ownerKey: UserKey, instance: Instance<*>): InstanceJson {
        val done = instance.done?.long

        val instanceDate = instance.instanceDate

        val newInstanceTime = instance.instanceTime.let {
            when (it) {
                is Time.Custom<*> -> getOrCreateCustomTime(ownerKey, it)
                is Time.Normal -> it
            }
        }

        val instanceTimeString = when (newInstanceTime) {
            is Time.Custom<*> -> newInstanceTime.key
                    .customTimeId
                    .value
            is Time.Normal -> newInstanceTime.hourMinute.toJson()
        }

        return InstanceJson(
                done,
                instanceDate.toJson(),
                instanceTimeString
        )
    }

    fun <V : TaskHierarchy<*>> copyTaskHierarchy(
            now: ExactTimeStamp,
            startTaskHierarchy: V,
            parentTaskId: String,
            childTaskId: String
    ): TaskHierarchy<T> {
        check(parentTaskId.isNotEmpty())
        check(childTaskId.isNotEmpty())

        val endTime = startTaskHierarchy.endExactTimeStamp?.long

        val taskHierarchyJson = TaskHierarchyJson(
                parentTaskId,
                childTaskId,
                now.long,
                endTime
        )

        val taskHierarchyRecord = projectRecord.newTaskHierarchyRecord(taskHierarchyJson)

        val taskHierarchy = TaskHierarchy(this, taskHierarchyRecord)

        taskHierarchyContainer.add(taskHierarchy.id, taskHierarchy)
        taskHierarchy.invalidateTasks()

        return taskHierarchy
    }

    fun deleteTask(task: Task<T>) {
        check(_tasks.containsKey(task.id))

        _tasks.remove(task.id)
    }

    fun deleteTaskHierarchy(taskHierarchy: TaskHierarchy<T>) {
        taskHierarchyContainer.removeForce(taskHierarchy.id)
        taskHierarchy.invalidateTasks()
    }

    fun getTaskIfPresent(taskId: String) = _tasks[taskId]

    fun getTaskForce(taskId: String) = _tasks[taskId]
            ?: throw MissingTaskException(projectKey, taskId)

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<TaskHierarchy<T>> {
        check(childTaskKey.taskId.isNotEmpty())

        return taskHierarchyContainer.getByChildTaskKey(childTaskKey)
    }

    fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy<T>> {
        check(parentTaskKey.taskId.isNotEmpty())

        return taskHierarchyContainer.getByParentTaskKey(parentTaskKey)
    }

    fun delete(parent: Parent) {
        parent.deleteProject(this)

        projectRecord.delete()
    }

    fun setEndExactTimeStamp(
            now: ExactTimeStamp,
            projectUndoData: ProjectUndoData,
            removeInstances: Boolean
    ) {
        requireCurrent(now)

        _tasks.values
                .filter { it.current(now) }
                .forEach {
                    it.setEndData(Task.EndData(now, removeInstances), projectUndoData.taskUndoData)
                }

        projectUndoData.projectIds.add(projectKey)

        projectRecord.endTime = now.long
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        requireNotCurrent(now)

        projectRecord.endTime = null
    }

    fun getTaskHierarchy(id: String) = taskHierarchyContainer.getById(id)

    abstract fun getCustomTime(customTimeId: CustomTimeId<*>): Time.Custom<T>
    abstract fun getCustomTime(customTimeKey: CustomTimeKey<T>): Time.Custom<T>
    abstract fun getCustomTime(customTimeId: String): Time.Custom<T>

    fun convertRemoteToRemoteHelper(
            now: ExactTimeStamp,
            remoteToRemoteConversion: RemoteToRemoteConversion<T>,
            startTask: Task<T>
    ) {
        if (remoteToRemoteConversion.startTasks.containsKey(startTask.id))
            return

        remoteToRemoteConversion.startTasks[startTask.id] = Pair(
                startTask,
                startTask.existingInstances
                        .values
                        .filter { listOf(it.scheduleDateTime, it.instanceDateTime).maxOrNull()!!.toExactTimeStamp() >= now }
        )

        val childTaskHierarchies = startTask.getChildTaskHierarchies(now)

        remoteToRemoteConversion.startTaskHierarchies.addAll(childTaskHierarchies)

        childTaskHierarchies.map { it.childTask }.forEach {
            it.requireCurrent(now)

            convertRemoteToRemoteHelper(now, remoteToRemoteConversion, it)
        }
    }

    fun fixNotificationShown(
            shownFactory: Instance.ShownFactory,
            now: ExactTimeStamp
    ) = tasks.forEach {
        it.existingInstances
                .values
                .forEach { it.fixNotificationShown(shownFactory, now) }
    }

    fun getRootInstances(
            startExactTimeStamp: ExactTimeStamp?,
            endExactTimeStamp: ExactTimeStamp?,
            now: ExactTimeStamp,
            query: String? = null,
            filterVisible: Boolean = true
    ): Sequence<Instance<out T>> {
        check(startExactTimeStamp == null || endExactTimeStamp == null || startExactTimeStamp < endExactTimeStamp)

        throwIfInterrupted()

        val filteredTasks = query?.let {
            fun filterQuery(task: Task<T>): Boolean {
                throwIfInterrupted()

                if (task.matchesQuery(it)) return true

                return task.childHierarchyIntervals.any { filterQuery(it.taskHierarchy.childTask) }
            }

            tasks.filter(::filterQuery)
        } ?: tasks

        val instanceSequences = filteredTasks.map { task ->
            throwIfInterrupted()

            val instances = task.getInstances(startExactTimeStamp, endExactTimeStamp, now, onlyRoot = true)

            if (filterVisible) {
                instances.filter { instance ->
                    throwIfInterrupted()

                    instance.isVisible(now, true)
                }
            } else {
                instances
            }
        }

        return combineInstanceSequences(instanceSequences)
    }

    fun fixOffsets() {
        if (projectRecord.startTimeOffset == null) projectRecord.startTimeOffset = startExactTimeStamp.offset

        endExactTimeStamp?.let {
            if (projectRecord.endTimeOffset == null) projectRecord.endTimeOffset = it.offset
        }

        _tasks.values.forEach { it.fixOffsets() }
    }

    private class MissingTaskException(projectId: ProjectKey<*>, taskId: String) :
            Exception("projectId: $projectId, taskId: $taskId")

    interface Parent {

        fun deleteProject(project: Project<*>)
    }
}
