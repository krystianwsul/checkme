package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.domainmodel.Task
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy
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
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

abstract class RemoteProject<T : RemoteCustomTimeId>(
        protected val domainFactory: DomainFactory,
        val uuid: String) {

    protected abstract val remoteProjectRecord: RemoteProjectRecord<T>

    protected abstract val remoteTasks: MutableMap<String, RemoteTask<T>>
    protected abstract val remoteTaskHierarchies: TaskHierarchyContainer<String, RemoteTaskHierarchy>
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

        remoteTaskHierarchies.add(remoteTaskHierarchy.id, remoteTaskHierarchy)
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

            (scheduleKey.scheduleTimePair.customTimeKey as? CustomTimeKey.LocalCustomTimeKey)?.let { getRemoteCustomTimeId(it) }

            RemoteInstanceRecord.scheduleKeyToString(domainFactory, remoteProjectRecord.id, scheduleKey) to instanceJson
        }.toMutableMap()

        val oldestVisibleMap = oldestVisible?.let { mapOf(uuid to OldestVisibleJson(it.year, it.month, it.day)) }
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

        return InstanceJson(done, instanceDate.year, instanceDate.month, instanceDate.day, instanceRemoteCustomTimeId?.value, instanceHour, instanceMinute, instance.ordinal)
    }

    fun copyLocalTaskHierarchy(localTaskHierarchy: LocalTaskHierarchy, remoteParentTaskId: String, remoteChildTaskId: String): RemoteTaskHierarchy {
        check(!TextUtils.isEmpty(remoteParentTaskId))
        check(!TextUtils.isEmpty(remoteChildTaskId))

        val endTime = if (localTaskHierarchy.getEndExactTimeStamp() != null) localTaskHierarchy.getEndExactTimeStamp()!!.long else null

        val taskHierarchyJson = TaskHierarchyJson(remoteParentTaskId, remoteChildTaskId, localTaskHierarchy.startExactTimeStamp.long, endTime, localTaskHierarchy.ordinal)
        val remoteTaskHierarchyRecord = remoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson)

        val remoteTaskHierarchy = RemoteTaskHierarchy(domainFactory, this, remoteTaskHierarchyRecord)

        remoteTaskHierarchies.add(remoteTaskHierarchy.id, remoteTaskHierarchy)

        return remoteTaskHierarchy
    }

    fun deleteTask(remoteTask: RemoteTask<T>) {
        check(remoteTasks.containsKey(remoteTask.id))

        remoteTasks.remove(remoteTask.id)
    }

    fun deleteTaskHierarchy(remoteTaskHierarchy: RemoteTaskHierarchy) = remoteTaskHierarchies.removeForce(remoteTaskHierarchy.id)

    fun getRemoteTaskIfPresent(taskId: String) = remoteTasks[taskId]

    fun getRemoteTaskForce(taskId: String) = remoteTasks[taskId]!!

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<RemoteTaskHierarchy> {
        check(!TextUtils.isEmpty(childTaskKey.remoteTaskId))

        return remoteTaskHierarchies.getByChildTaskKey(childTaskKey)
    }

    fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<RemoteTaskHierarchy> {
        check(!TextUtils.isEmpty(parentTaskKey.remoteTaskId))

        return remoteTaskHierarchies.getByParentTaskKey(parentTaskKey)
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
                .forEach { it.setEndExactTimeStamp(now, projectUndoData?.taskUndoData) }

        projectUndoData?.projectIds?.add(id)

        remoteProjectRecord.endTime = now.long
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        check(!current(now))

        remoteProjectRecord.endTime = null
    }

    fun getTaskHierarchy(id: String) = remoteTaskHierarchies.getById(id)

    abstract fun getRemoteCustomTimeIfPresent(localCustomTimeId: Int): RemoteCustomTime<T>?

    abstract fun updateRecordOf(addedFriends: Set<RemoteRootUser>, removedFriends: Set<String>)

    abstract fun getRemoteCustomTimeId(customTimeKey: CustomTimeKey): T

    abstract fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): RemoteCustomTime<T>

    abstract fun getRemoteCustomTimeId(id: String): RemoteCustomTimeId
}
