package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.ProjectUndoData
import com.krystianwsul.common.domain.RemoteToRemoteConversion
import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.OldestVisibleJson
import com.krystianwsul.common.firebase.json.TaskHierarchyJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.NormalTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

abstract class Project<T : RemoteCustomTimeId, U : ProjectKey> {

    abstract val remoteProjectRecord: RemoteProjectRecord<T, U>

    protected abstract val remoteTasks: MutableMap<String, Task<T, U>>
    protected abstract val taskHierarchyContainer: TaskHierarchyContainer<T, U>
    protected abstract val remoteCustomTimes: Map<T, CustomTime<T, U>>

    val id: U by lazy { remoteProjectRecord.id }

    var name
        get() = remoteProjectRecord.name
        set(name) {
            check(name.isNotEmpty())

            remoteProjectRecord.name = name
        }

    private val startExactTimeStamp by lazy { ExactTimeStamp(remoteProjectRecord.startTime) }

    val endExactTimeStamp get() = remoteProjectRecord.endTime?.let { ExactTimeStamp(it) }

    val taskKeys get() = remoteTasks.keys

    val tasks get() = remoteTasks.values

    val taskIds get() = remoteTasks.keys

    abstract val customTimes: Collection<CustomTime<T, U>>

    val taskHierarchies get() = taskHierarchyContainer.all

    val existingInstances get() = tasks.flatMap { it.existingInstances.values }

    fun newRemoteTask(taskJson: TaskJson): Task<T, U> {
        val remoteTaskRecord = remoteProjectRecord.newRemoteTaskRecord(taskJson)

        val remoteTask = Task(this, remoteTaskRecord)
        check(!remoteTasks.containsKey(remoteTask.id))
        remoteTasks[remoteTask.id] = remoteTask

        return remoteTask
    }

    fun createTaskHierarchy(
            parentTask: Task<T, U>,
            childTask: Task<T, U>,
            now: ExactTimeStamp
    ) {
        val taskHierarchyJson = TaskHierarchyJson(parentTask.id, childTask.id, now.long, null, null)
        val remoteTaskHierarchyRecord = remoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson)

        val remoteTaskHierarchy = TaskHierarchy(this, remoteTaskHierarchyRecord)

        taskHierarchyContainer.add(remoteTaskHierarchy.id, remoteTaskHierarchy)
    }

    fun copyTask(
            deviceDbInfo: DeviceDbInfo,
            task: Task<*, *>,
            instances: Collection<Instance<*, *>>,
            now: ExactTimeStamp
    ): Task<T, U> {
        val endTime = task.getEndExactTimeStamp()?.long

        val oldestVisible = task.getOldestVisible()

        val instanceJsons = instances.associate {
            val instanceJson = getInstanceJson(deviceDbInfo.key, it)
            val scheduleKey = it.scheduleKey

            InstanceRecord.scheduleKeyToString(scheduleKey) to instanceJson
        }.toMutableMap()

        val oldestVisibleMap = oldestVisible?.let { mapOf(deviceDbInfo.uuid to OldestVisibleJson.fromDate(Date(it.year, it.month, it.day))) }
                ?: mapOf()

        val taskJson = TaskJson(
                task.name,
                now.long,
                endTime,
                task.note,
                instanceJsons,
                oldestVisible = oldestVisibleMap.toMutableMap())
        val remoteTaskRecord = remoteProjectRecord.newRemoteTaskRecord(taskJson)

        val remoteTask = Task(this, remoteTaskRecord)
        check(!remoteTasks.containsKey(remoteTask.id))

        remoteTasks[remoteTask.id] = remoteTask

        remoteTask.copySchedules(deviceDbInfo, now, task.getCurrentSchedules(now))

        return remoteTask
    }

    abstract fun getOrCreateCustomTime(
            ownerKey: UserKey,
            customTime: CustomTime<*, *>
    ): CustomTime<T, U>

    fun getOrCopyTime(ownerKey: UserKey, time: Time) = time.let {
        when (it) {
            is CustomTime<*, *> -> getOrCreateCustomTime(ownerKey, it)
            is NormalTime -> it
            else -> throw IllegalArgumentException()
        }
    }

    fun getOrCopyAndDestructureTime(
            ownerKey: UserKey,
            time: Time
    ) = when (val newTime = getOrCopyTime(ownerKey, time)) {
        is CustomTime<*, *> -> Triple(newTime.key.remoteCustomTimeId, null, null)
        is NormalTime -> Triple(null, newTime.hourMinute.hour, newTime.hourMinute.minute)
        else -> throw IllegalArgumentException()
    }

    private fun getInstanceJson(ownerKey: UserKey, instance: Instance<*, *>): InstanceJson {
        val done = instance.done?.long

        val instanceDate = instance.instanceDate

        val newInstanceTime = instance.instanceTime.let {
            when (it) {
                is CustomTime<*, *> -> getOrCreateCustomTime(ownerKey, it)
                is NormalTime -> it
                else -> throw IllegalArgumentException()
            }
        }

        val instanceCustomTimeId: String?
        val instanceHour: Int?
        val instanceMinute: Int?
        val instanceTimeString: String

        when (newInstanceTime) {
            is CustomTime<*, *> -> {
                instanceCustomTimeId = newInstanceTime.key
                        .remoteCustomTimeId
                        .value
                instanceHour = null
                instanceMinute = null
                instanceTimeString = instanceCustomTimeId
            }
            is NormalTime -> {
                instanceCustomTimeId = null
                instanceHour = newInstanceTime.hourMinute.hour
                instanceMinute = newInstanceTime.hourMinute.minute
                instanceTimeString = newInstanceTime.hourMinute.toJson()
            }
            else -> throw IllegalArgumentException()
        }

        return InstanceJson(
                done,
                instanceDate.toJson(),
                instanceDate.year,
                instanceDate.month,
                instanceDate.day,
                instanceTimeString,
                instanceCustomTimeId,
                instanceHour,
                instanceMinute,
                instance.ordinal)
    }

    fun <V : TaskHierarchy<*, *>> copyRemoteTaskHierarchy(
            now: ExactTimeStamp,
            startTaskHierarchy: V,
            remoteParentTaskId: String,
            remoteChildTaskId: String
    ): TaskHierarchy<T, U> {
        check(remoteParentTaskId.isNotEmpty())
        check(remoteChildTaskId.isNotEmpty())

        val endTime = if (startTaskHierarchy.getEndExactTimeStamp() != null) startTaskHierarchy.getEndExactTimeStamp()!!.long else null

        val taskHierarchyJson = TaskHierarchyJson(remoteParentTaskId, remoteChildTaskId, now.long, endTime, startTaskHierarchy.ordinal)
        val remoteTaskHierarchyRecord = remoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson)

        val remoteTaskHierarchy = TaskHierarchy(this, remoteTaskHierarchyRecord)

        taskHierarchyContainer.add(remoteTaskHierarchy.id, remoteTaskHierarchy)

        return remoteTaskHierarchy
    }

    fun deleteTask(task: Task<T, U>) {
        check(remoteTasks.containsKey(task.id))

        remoteTasks.remove(task.id)
    }

    fun deleteTaskHierarchy(taskHierarchy: TaskHierarchy<T, U>) = taskHierarchyContainer.removeForce(taskHierarchy.id)

    fun getRemoteTaskIfPresent(taskId: String) = remoteTasks[taskId]

    fun getRemoteTaskForce(taskId: String) = remoteTasks[taskId]
            ?: throw MissingTaskException(id, taskId)

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<TaskHierarchy<T, U>> {
        check(childTaskKey.remoteTaskId.isNotEmpty())

        return taskHierarchyContainer.getByChildTaskKey(childTaskKey)
    }

    fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy<T, U>> {
        check(parentTaskKey.remoteTaskId.isNotEmpty())

        return taskHierarchyContainer.getByParentTaskKey(parentTaskKey)
    }

    fun delete(parent: Parent) {
        parent.deleteProject(this)

        remoteProjectRecord.delete()
    }

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val endExactTimeStamp = endExactTimeStamp

        return startExactTimeStamp <= exactTimeStamp && (endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp)
    }

    fun setEndExactTimeStamp(uuid: String, now: ExactTimeStamp, projectUndoData: ProjectUndoData, removeInstances: Boolean) {
        check(current(now))

        remoteTasks.values
                .filter { it.current(now) }
                .forEach {
                    it.setEndData(
                            uuid,
                            Task.EndData(now, removeInstances),
                            projectUndoData.taskUndoData
                    )
                }

        projectUndoData.projectIds.add(id)

        remoteProjectRecord.endTime = now.long
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        check(!current(now))

        remoteProjectRecord.endTime = null
    }

    fun getTaskHierarchy(id: String) = taskHierarchyContainer.getById(id)

    abstract fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): CustomTime<T, U>
    abstract fun getRemoteCustomTime(customTimeKey: CustomTimeKey<T, U>): CustomTime<T, U>

    abstract fun getRemoteCustomTimeId(id: String): T

    fun getRemoteCustomTime(remoteCustomTimeId: String) = getRemoteCustomTime(getRemoteCustomTimeId(remoteCustomTimeId))

    fun convertRemoteToRemoteHelper(
            now: ExactTimeStamp,
            remoteToRemoteConversion: RemoteToRemoteConversion<T, U>,
            startTask: Task<T, U>) {
        if (remoteToRemoteConversion.startTasks.containsKey(startTask.id))
            return

        remoteToRemoteConversion.startTasks[startTask.id] = Pair(
                startTask,
                startTask.existingInstances
                        .values
                        .toList()
                        .filter { listOf(it.scheduleDateTime, it.instanceDateTime).max()!!.toExactTimeStamp() >= now }
        )

        @Suppress("UNCHECKED_CAST")
        val childTaskHierarchies = startTask.getChildTaskHierarchies(now)

        remoteToRemoteConversion.startTaskHierarchies.addAll(childTaskHierarchies)

        childTaskHierarchies.map { it.childTask }.forEach {
            check(it.current(now))

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
            endExactTimeStamp: ExactTimeStamp,
            now: ExactTimeStamp
    ): List<Instance<T, U>> {
        check(startExactTimeStamp == null || startExactTimeStamp < endExactTimeStamp)

        val allInstances = mutableMapOf<InstanceKey, Instance<T, U>>()

        for (instance in existingInstances) {
            val instanceExactTimeStamp = instance.instanceDateTime
                    .timeStamp
                    .toExactTimeStamp()

            if (startExactTimeStamp != null && startExactTimeStamp > instanceExactTimeStamp)
                continue

            if (endExactTimeStamp <= instanceExactTimeStamp)
                continue

            allInstances[instance.instanceKey] = instance
        }

        tasks.forEach { task ->
            for (instance in task.getInstances(startExactTimeStamp, endExactTimeStamp, now)) {
                val instanceExactTimeStamp = instance.instanceDateTime.timeStamp.toExactTimeStamp()

                if (startExactTimeStamp != null && startExactTimeStamp > instanceExactTimeStamp)
                    continue

                if (endExactTimeStamp <= instanceExactTimeStamp)
                    continue

                allInstances[instance.instanceKey] = instance
            }
        }

        return allInstances.values.filter { it.isRootInstance(now) && it.isVisible(now, true) }
    }

    private class MissingTaskException(projectId: ProjectKey, taskId: String) : Exception("projectId: $projectId, taskId: $taskId")

    interface Parent {

        fun deleteProject(project: Project<*, *>)
    }
}
