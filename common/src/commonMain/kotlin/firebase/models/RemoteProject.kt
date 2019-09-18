package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.*
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.OldestVisibleJson
import com.krystianwsul.common.firebase.json.TaskHierarchyJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.records.RemoteInstanceRecord
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.NormalTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.TaskKey

abstract class RemoteProject<T : RemoteCustomTimeId> {

    abstract val remoteProjectRecord: RemoteProjectRecord<T>

    protected abstract val remoteTasks: MutableMap<String, RemoteTask<T>>
    protected abstract val remoteTaskHierarchyContainer: TaskHierarchyContainer<String, RemoteTaskHierarchy<T>>
    protected abstract val remoteCustomTimes: Map<T, RemoteCustomTime<T>>

    val id by lazy { remoteProjectRecord.id }

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

    abstract val customTimes: Collection<RemoteCustomTime<T>>

    val taskHierarchies get() = remoteTaskHierarchyContainer.all

    val existingInstances get() = tasks.flatMap { it.existingInstances.values }

    fun newRemoteTask(taskJson: TaskJson): RemoteTask<T> {
        val remoteTaskRecord = remoteProjectRecord.newRemoteTaskRecord(taskJson)

        val remoteTask = RemoteTask(this, remoteTaskRecord)
        check(!remoteTasks.containsKey(remoteTask.id))
        remoteTasks[remoteTask.id] = remoteTask

        return remoteTask
    }

    fun createTaskHierarchy(parentRemoteTask: RemoteTask<T>, childRemoteTask: RemoteTask<T>, now: ExactTimeStamp) {
        val taskHierarchyJson = TaskHierarchyJson(parentRemoteTask.id, childRemoteTask.id, now.long, null, null)
        val remoteTaskHierarchyRecord = remoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson)

        val remoteTaskHierarchy = RemoteTaskHierarchy(this, remoteTaskHierarchyRecord)

        remoteTaskHierarchyContainer.add(remoteTaskHierarchy.id, remoteTaskHierarchy)
    }

    fun copyTask(deviceDbInfo: DeviceDbInfo, task: Task, instances: Collection<Instance>, now: ExactTimeStamp): RemoteTask<T> {
        val endTime = task.getEndExactTimeStamp()?.long

        val oldestVisible = task.getOldestVisible()
        val oldestVisibleYear = oldestVisible?.year
        val oldestVisibleMonth = oldestVisible?.month
        val oldestVisibleDay = oldestVisible?.day

        val instanceJsons = instances.associate {
            val instanceJson = getInstanceJson(deviceDbInfo.key, it)
            val scheduleKey = it.scheduleKey

            RemoteInstanceRecord.scheduleKeyToString(scheduleKey) to instanceJson
        }.toMutableMap()

        val oldestVisibleMap = oldestVisible?.let { mapOf(deviceDbInfo.uuid to OldestVisibleJson.fromDate(Date(it.year, it.month, it.day))) }
                ?: mapOf()

        val taskJson = TaskJson(
                task.name,
                now.long,
                endTime,
                oldestVisibleYear,
                oldestVisibleMonth,
                oldestVisibleDay,
                task.note,
                instanceJsons,
                oldestVisible = oldestVisibleMap.toMutableMap())
        val remoteTaskRecord = remoteProjectRecord.newRemoteTaskRecord(taskJson)

        val remoteTask = RemoteTask(this, remoteTaskRecord)
        check(!remoteTasks.containsKey(remoteTask.id))

        remoteTasks[remoteTask.id] = remoteTask

        remoteTask.copySchedules(deviceDbInfo, now, task.getCurrentSchedules(now))

        return remoteTask
    }

    abstract fun getOrCreateCustomTime(ownerKey: String, remoteCustomTime: RemoteCustomTime<*>): RemoteCustomTime<T>

    fun getOrCopyTime(ownerKey: String, time: Time) = time.let {
        when (it) {
            is CustomTime -> getOrCreateCustomTime(ownerKey, it as RemoteCustomTime<*>)
            is NormalTime -> it
            else -> throw IllegalArgumentException()
        }
    }

    fun getOrCopyAndDestructureTime(ownerKey: String, time: Time) = when (val newTime = getOrCopyTime(ownerKey, time)) {
        is CustomTime -> Triple(newTime.customTimeKey.remoteCustomTimeId, null, null)
        is NormalTime -> Triple(null, newTime.hourMinute.hour, newTime.hourMinute.minute)
        else -> throw IllegalArgumentException()
    }

    private fun getInstanceJson(ownerKey: String, instance: Instance): InstanceJson {
        val done = instance.done?.long

        val instanceDate = instance.instanceDate

        val newInstanceTime = instance.instanceTime.let {
            when (it) {
                is CustomTime -> getOrCreateCustomTime(ownerKey, it as RemoteCustomTime<*>)
                is NormalTime -> it
                else -> throw IllegalArgumentException()
            }
        }

        val instanceCustomTimeId: String?
        val instanceHour: Int?
        val instanceMinute: Int?
        val instanceTimeString: String

        when (newInstanceTime) {
            is CustomTime -> {
                instanceCustomTimeId = newInstanceTime.customTimeKey
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

    fun <U : RemoteCustomTimeId> copyRemoteTaskHierarchy(now: ExactTimeStamp, startTaskHierarchy: RemoteTaskHierarchy<U>, remoteParentTaskId: String, remoteChildTaskId: String): RemoteTaskHierarchy<T> {
        check(remoteParentTaskId.isNotEmpty())
        check(remoteChildTaskId.isNotEmpty())

        val endTime = if (startTaskHierarchy.getEndExactTimeStamp() != null) startTaskHierarchy.getEndExactTimeStamp()!!.long else null

        val taskHierarchyJson = TaskHierarchyJson(remoteParentTaskId, remoteChildTaskId, now.long, endTime, startTaskHierarchy.ordinal)
        val remoteTaskHierarchyRecord = remoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson)

        val remoteTaskHierarchy = RemoteTaskHierarchy(this, remoteTaskHierarchyRecord)

        remoteTaskHierarchyContainer.add(remoteTaskHierarchy.id, remoteTaskHierarchy)

        return remoteTaskHierarchy
    }

    fun deleteTask(remoteTask: RemoteTask<T>) {
        check(remoteTasks.containsKey(remoteTask.id))

        remoteTasks.remove(remoteTask.id)
    }

    fun deleteTaskHierarchy(remoteTaskHierarchy: RemoteTaskHierarchy<T>) = remoteTaskHierarchyContainer.removeForce(remoteTaskHierarchy.id)

    fun getRemoteTaskIfPresent(taskId: String) = remoteTasks[taskId]

    fun getRemoteTaskForce(taskId: String) = remoteTasks[taskId]
            ?: throw MissingTaskException(id, taskId)

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<RemoteTaskHierarchy<T>> {
        check(childTaskKey.remoteTaskId.isNotEmpty())

        return remoteTaskHierarchyContainer.getByChildTaskKey(childTaskKey)
    }

    fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<RemoteTaskHierarchy<T>> {
        check(parentTaskKey.remoteTaskId.isNotEmpty())

        return remoteTaskHierarchyContainer.getByParentTaskKey(parentTaskKey)
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
                .forEach { it.setEndData(uuid, Task.EndData(now, removeInstances), projectUndoData.taskUndoData) }

        projectUndoData.projectIds.add(id)

        remoteProjectRecord.endTime = now.long
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        check(!current(now))

        remoteProjectRecord.endTime = null
    }

    fun getTaskHierarchy(id: String) = remoteTaskHierarchyContainer.getById(id)

    abstract fun updateRecordOf(addedFriends: Set<RemoteRootUser>, removedFriends: Set<String>)

    abstract fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): RemoteCustomTime<T>

    abstract fun getRemoteCustomTimeId(id: String): RemoteCustomTimeId

    fun convertRemoteToRemoteHelper(
            now: ExactTimeStamp,
            remoteToRemoteConversion: RemoteToRemoteConversion<T>,
            startTask: RemoteTask<T>) {
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
        val childTaskHierarchies = startTask.getChildTaskHierarchies(now).map { it as RemoteTaskHierarchy<T> }

        remoteToRemoteConversion.startTaskHierarchies.addAll(childTaskHierarchies)

        childTaskHierarchies.map { it.childTask }.forEach {
            check(it.current(now))

            convertRemoteToRemoteHelper(now, remoteToRemoteConversion, it)
        }
    }

    fun fixNotificationShown(shownFactory: Instance.ShownFactory, now: ExactTimeStamp) = tasks.forEach {
        it.existingInstances
                .values
                .forEach { it.fixNotificationShown(shownFactory, now) }
    }

    fun getRootInstances(startExactTimeStamp: ExactTimeStamp?, endExactTimeStamp: ExactTimeStamp, now: ExactTimeStamp): List<Instance> {
        check(startExactTimeStamp == null || startExactTimeStamp < endExactTimeStamp)

        val allInstances = HashMap<InstanceKey, Instance>()

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

    private class MissingTaskException(projectId: String, taskId: String) : Exception("projectId: $projectId, taskId: $taskId")

    interface Parent {

        fun deleteProject(remoteProject: RemoteProject<*>)
    }
}
