package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.domainmodel.RemoteToRemoteConversion
import com.krystianwsul.checkme.domainmodel.Task
import com.krystianwsul.checkme.firebase.json.InstanceJson
import com.krystianwsul.checkme.firebase.json.OldestVisibleJson
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson
import com.krystianwsul.checkme.firebase.json.TaskJson
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.firebase.records.RemoteProjectRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.TaskHierarchyContainer
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

abstract class RemoteProject<T : RemoteCustomTimeId>(
        protected val domainFactory: DomainFactory,
        val uuid: String) {

    protected abstract val remoteProjectRecord: RemoteProjectRecord<T>

    protected abstract val remoteTasks: MutableMap<String, RemoteTask<T>>
    protected abstract val remoteTaskHierarchyContainer: TaskHierarchyContainer<String, RemoteTaskHierarchy<T>>
    protected abstract val remoteCustomTimes: Map<T, RemoteCustomTime<T>>

    val id by lazy { remoteProjectRecord.id }

    var name
        get() = remoteProjectRecord.name
        set(name) {
            check(!TextUtils.isEmpty(name))

            remoteProjectRecord.name = name
        }

    private val startExactTimeStamp by lazy { ExactTimeStamp(remoteProjectRecord.startTime) }

    private val endExactTimeStamp get() = remoteProjectRecord.endTime?.let { ExactTimeStamp(it) }

    private val remoteFactory get() = domainFactory.remoteProjectFactory

    val taskKeys get() = remoteTasks.keys

    val tasks get() = remoteTasks.values

    val taskIds get() = remoteTasks.keys

    abstract val customTimes: Collection<RemoteCustomTime<T>>

    val taskHierarchies get() = remoteTaskHierarchyContainer.all

    fun newRemoteTask(taskJson: TaskJson, now: ExactTimeStamp): RemoteTask<T> {
        val remoteTaskRecord = remoteProjectRecord.newRemoteTaskRecord(domainFactory, taskJson)

        val remoteTask = RemoteTask(domainFactory, this, remoteTaskRecord, now)
        check(!remoteTasks.containsKey(remoteTask.id))
        remoteTasks[remoteTask.id] = remoteTask

        return remoteTask
    }

    fun createTaskHierarchy(parentRemoteTask: RemoteTask<T>, childRemoteTask: RemoteTask<T>, now: ExactTimeStamp) {
        val taskHierarchyJson = TaskHierarchyJson(parentRemoteTask.id, childRemoteTask.id, now.long, null, null)
        val remoteTaskHierarchyRecord = remoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson)

        val remoteTaskHierarchy = RemoteTaskHierarchy(domainFactory, this, remoteTaskHierarchyRecord)

        remoteTaskHierarchyContainer.add(remoteTaskHierarchy.id, remoteTaskHierarchy)
    }

    fun copyTask(task: Task, instances: Collection<Instance>, now: ExactTimeStamp): RemoteTask<T> {
        val endTime = task.getEndExactTimeStamp()?.long

        val oldestVisible = task.getOldestVisible()
        val oldestVisibleYear = oldestVisible?.year
        val oldestVisibleMonth = oldestVisible?.month
        val oldestVisibleDay = oldestVisible?.day

        val instanceJsons = instances.associate {
            val instanceJson = getInstanceJson(it)
            val scheduleKey = it.scheduleKey

            RemoteInstanceRecord.scheduleKeyToString(scheduleKey) to instanceJson
        }.toMutableMap()

        val oldestVisibleMap = oldestVisible?.let { mapOf(uuid to OldestVisibleJson(Date(it.year, it.month, it.day))) }
                ?: mapOf()

        val taskJson = TaskJson(task.name, task.startExactTimeStamp.long, endTime, oldestVisibleYear, oldestVisibleMonth, oldestVisibleDay, task.note, instanceJsons, oldestVisible = oldestVisibleMap.toMutableMap())
        val remoteTaskRecord = remoteProjectRecord.newRemoteTaskRecord(domainFactory, taskJson)

        val remoteTask = RemoteTask(domainFactory, this, remoteTaskRecord, now)
        check(!remoteTasks.containsKey(remoteTask.id))

        remoteTasks[remoteTask.id] = remoteTask

        remoteTask.copySchedules(task.schedules)

        return remoteTask
    }

    private fun getInstanceJson(instance: Instance): InstanceJson {
        val done = instance.done?.long

        val instanceDate = instance.instanceDate
        val instanceTimePair = instance.instanceTimePair

        val (instanceRemoteCustomTimeId, instanceHour, instanceMinute) = instanceTimePair.destructureRemote(this)

        val instanceTime = instanceRemoteCustomTimeId?.value
                ?: instanceTimePair.hourMinute?.toJson()

        return InstanceJson(done, instanceDate.toJson(), instanceDate.year, instanceDate.month, instanceDate.day, instanceTime, instanceRemoteCustomTimeId?.value, instanceHour, instanceMinute, instance.ordinal)
    }

    fun <U : RemoteCustomTimeId> copyRemoteTaskHierarchy(startTaskHierarchy: RemoteTaskHierarchy<U>, remoteParentTaskId: String, remoteChildTaskId: String): RemoteTaskHierarchy<T> {
        check(!TextUtils.isEmpty(remoteParentTaskId))
        check(!TextUtils.isEmpty(remoteChildTaskId))

        val endTime = if (startTaskHierarchy.getEndExactTimeStamp() != null) startTaskHierarchy.getEndExactTimeStamp()!!.long else null

        val taskHierarchyJson = TaskHierarchyJson(remoteParentTaskId, remoteChildTaskId, startTaskHierarchy.startExactTimeStamp.long, endTime, startTaskHierarchy.ordinal)
        val remoteTaskHierarchyRecord = remoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson)

        val remoteTaskHierarchy = RemoteTaskHierarchy(domainFactory, this, remoteTaskHierarchyRecord)

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
        check(!TextUtils.isEmpty(childTaskKey.remoteTaskId))

        return remoteTaskHierarchyContainer.getByChildTaskKey(childTaskKey)
    }

    fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<RemoteTaskHierarchy<T>> {
        check(!TextUtils.isEmpty(parentTaskKey.remoteTaskId))

        return remoteTaskHierarchyContainer.getByParentTaskKey(parentTaskKey)
    }

    fun delete() {
        remoteFactory.deleteProject(this)

        remoteProjectRecord.delete()
    }

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val endExactTimeStamp = endExactTimeStamp

        return startExactTimeStamp <= exactTimeStamp && (endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp)
    }

    fun setEndExactTimeStamp(now: ExactTimeStamp, projectUndoData: DomainFactory.ProjectUndoData? = null) {
        check(current(now))

        remoteTasks.values
                .filter { it.current(now) }
                .forEach { it.setEndExactTimeStamp(now, projectUndoData?.taskUndoData?.let { Pair(it, false) }) } // todo delete project instances

        projectUndoData?.projectIds?.add(id)

        remoteProjectRecord.endTime = now.long
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        check(!current(now))

        remoteProjectRecord.endTime = null
    }

    fun getTaskHierarchy(id: String) = remoteTaskHierarchyContainer.getById(id)

    abstract fun updateRecordOf(addedFriends: Set<RemoteRootUser>, removedFriends: Set<String>)

    abstract fun getRemoteCustomTimeKey(customTimeKey: CustomTimeKey<*>): CustomTimeKey<T>

    abstract fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): RemoteCustomTime<T>

    abstract fun getRemoteCustomTimeId(id: String): RemoteCustomTimeId

    fun convertRemoteToRemoteHelper(remoteToRemoteConversion: RemoteToRemoteConversion<T>, startTask: RemoteTask<T>) {
        if (remoteToRemoteConversion.startTasks.containsKey(startTask.id))
            return

        val taskKey = startTask.taskKey

        remoteToRemoteConversion.startTasks[startTask.id] = Pair(startTask, startTask.existingInstances.values.toList())

        val parentLocalTaskHierarchies = remoteTaskHierarchyContainer.getByChildTaskKey(taskKey)

        remoteToRemoteConversion.startTaskHierarchies.addAll(parentLocalTaskHierarchies)

        remoteTaskHierarchyContainer.getByParentTaskKey(taskKey)
                .map { it.childTask }
                .forEach { convertRemoteToRemoteHelper(remoteToRemoteConversion, it) }

        parentLocalTaskHierarchies.map { it.parentTask }.forEach { convertRemoteToRemoteHelper(remoteToRemoteConversion, it) }
    }

    private class MissingTaskException(projectId: String, taskId: String) : Exception("projectId: $projectId, taskId: $taskId")
}
