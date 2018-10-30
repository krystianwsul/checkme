package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.domainmodel.local.LocalInstance
import com.krystianwsul.checkme.domainmodel.local.LocalTask
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy
import com.krystianwsul.checkme.firebase.json.CustomTimeJson
import com.krystianwsul.checkme.firebase.json.InstanceJson
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson
import com.krystianwsul.checkme.firebase.json.TaskJson
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord
import com.krystianwsul.checkme.firebase.records.RemoteProjectRecord
import com.krystianwsul.checkme.utils.TaskHierarchyContainer
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import java.util.*

class RemoteProject(
        private val kotlinDomainFactory: KotlinDomainFactory,
        private val remoteProjectRecord: RemoteProjectRecord,
        userInfo: UserInfo,
        uuid: String,
        now: ExactTimeStamp) {

    private val remoteTasks: MutableMap<String, RemoteTask>
    private val remoteTaskHierarchies = TaskHierarchyContainer<String, RemoteTaskHierarchy>()
    private val remoteCustomTimes = HashMap<String, RemoteCustomTime>()
    private val remoteUsers: MutableMap<String, RemoteProjectUser>

    val id by lazy { remoteProjectRecord.id }

    var name
        get() = remoteProjectRecord.name
        set(name) {
            check(!TextUtils.isEmpty(name))

            remoteProjectRecord.name = name
        }

    private val startExactTimeStamp by lazy { ExactTimeStamp(remoteProjectRecord.startTime) }

    private val endExactTimeStamp get() = remoteProjectRecord.endTime?.let { ExactTimeStamp(it) }

    private val remoteFactory get() = kotlinDomainFactory.remoteProjectFactory!!

    val tasks get() = remoteTasks.values

    val taskIds get() = remoteTasks.keys

    val customTimes get() = remoteCustomTimes.values

    val users get() = remoteUsers.values

    init {
        for (remoteCustomTimeRecord in remoteProjectRecord.remoteCustomTimeRecords.values) {
            val remoteCustomTime = RemoteCustomTime(kotlinDomainFactory, this, remoteCustomTimeRecord)

            check(!TextUtils.isEmpty(remoteCustomTime.customTimeKey.remoteCustomTimeId))
            check(!remoteCustomTimes.containsKey(remoteCustomTime.customTimeKey.remoteCustomTimeId))

            remoteCustomTimes[remoteCustomTime.customTimeKey.remoteCustomTimeId!!] = remoteCustomTime

            if (remoteCustomTimeRecord.ownerId == kotlinDomainFactory.localFactory.uuid && kotlinDomainFactory.localFactory.hasLocalCustomTime(remoteCustomTimeRecord.localId)) {
                val localCustomTime = kotlinDomainFactory.localFactory.getLocalCustomTime(remoteCustomTimeRecord.localId)

                localCustomTime.addRemoteCustomTimeRecord(remoteCustomTimeRecord)
            }
        }

        remoteTasks = remoteProjectRecord.remoteTaskRecords
                .values
                .map { RemoteTask(kotlinDomainFactory, this, it, now) }
                .associateBy { it.id }
                .toMutableMap()

        remoteProjectRecord.remoteTaskHierarchyRecords
                .values
                .map { RemoteTaskHierarchy(kotlinDomainFactory, this, it) }
                .forEach { remoteTaskHierarchies.add(it.id, it) }

        remoteUsers = remoteProjectRecord.remoteUserRecords
                .values
                .map { RemoteProjectUser(this, it) }
                .associateBy { it.id }
                .toMutableMap()

        updateUserInfo(userInfo, uuid)
    }

    fun newRemoteTask(taskJson: TaskJson, now: ExactTimeStamp): RemoteTask {
        val remoteTaskRecord = remoteProjectRecord.newRemoteTaskRecord(kotlinDomainFactory, taskJson)

        val remoteTask = RemoteTask(kotlinDomainFactory, this, remoteTaskRecord, now)
        check(!remoteTasks.containsKey(remoteTask.id))
        remoteTasks[remoteTask.id] = remoteTask

        return remoteTask
    }

    fun createTaskHierarchy(parentRemoteTask: RemoteTask, childRemoteTask: RemoteTask, now: ExactTimeStamp) {
        val taskHierarchyJson = TaskHierarchyJson(parentRemoteTask.id, childRemoteTask.id, now.long, null, null)
        val remoteTaskHierarchyRecord = remoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson)

        val remoteTaskHierarchy = RemoteTaskHierarchy(kotlinDomainFactory, this, remoteTaskHierarchyRecord)

        remoteTaskHierarchies.add(remoteTaskHierarchy.id, remoteTaskHierarchy)
    }

    fun copyLocalTask(localTask: LocalTask, localInstances: Collection<LocalInstance>, now: ExactTimeStamp): RemoteTask {
        val endTime = if (localTask.getEndExactTimeStamp() != null) localTask.getEndExactTimeStamp()!!.long else null

        val oldestVisible = localTask.getOldestVisible()
        val oldestVisibleYear: Int?
        val oldestVisibleMonth: Int?
        val oldestVisibleDay: Int?
        if (oldestVisible != null) {
            oldestVisibleYear = oldestVisible.year
            oldestVisibleMonth = oldestVisible.month
            oldestVisibleDay = oldestVisible.day
        } else {
            oldestVisibleYear = null
            oldestVisibleMonth = null
            oldestVisibleDay = null
        }

        val instanceJsons = HashMap<String, InstanceJson>()
        for (localInstance in localInstances) {
            check(localInstance.taskId == localTask.id)

            val instanceJson = getInstanceJson(localInstance)
            val scheduleKey = localInstance.scheduleKey

            if (scheduleKey.scheduleTimePair.customTimeKey != null)
                remoteFactory.getRemoteCustomTimeId(scheduleKey.scheduleTimePair.customTimeKey, this)

            instanceJsons[RemoteInstanceRecord.scheduleKeyToString(kotlinDomainFactory, remoteProjectRecord.id, scheduleKey)] = instanceJson
        }

        val taskJson = TaskJson(localTask.name, localTask.startExactTimeStamp.long, endTime, oldestVisibleYear, oldestVisibleMonth, oldestVisibleDay, localTask.note, instanceJsons)
        val remoteTaskRecord = remoteProjectRecord.newRemoteTaskRecord(kotlinDomainFactory, taskJson)

        val remoteTask = RemoteTask(kotlinDomainFactory, this, remoteTaskRecord, now)
        check(!remoteTasks.containsKey(remoteTask.id))

        remoteTasks[remoteTask.id] = remoteTask

        remoteTask.copySchedules(localTask.schedules)

        return remoteTask
    }

    private fun getInstanceJson(localInstance: LocalInstance): InstanceJson {
        val done = localInstance.done?.long

        val instanceDate = localInstance.instanceDate
        val instanceTimePair = localInstance.instanceTimePair

        val instanceRemoteCustomTimeId: String?
        val instanceHour: Int?
        val instanceMinute: Int?
        if (instanceTimePair.hourMinute != null) {
            check(instanceTimePair.customTimeKey == null)

            instanceRemoteCustomTimeId = null

            instanceHour = instanceTimePair.hourMinute.hour
            instanceMinute = instanceTimePair.hourMinute.minute
        } else {
            checkNotNull(instanceTimePair.customTimeKey)

            instanceRemoteCustomTimeId = remoteFactory.getRemoteCustomTimeId(instanceTimePair.customTimeKey, this)

            instanceHour = null
            instanceMinute = null
        }

        return InstanceJson(done, instanceDate.year, instanceDate.month, instanceDate.day, instanceRemoteCustomTimeId, instanceHour, instanceMinute, localInstance.ordinal)
    }

    fun copyLocalTaskHierarchy(localTaskHierarchy: LocalTaskHierarchy, remoteParentTaskId: String, remoteChildTaskId: String): RemoteTaskHierarchy {
        check(!TextUtils.isEmpty(remoteParentTaskId))
        check(!TextUtils.isEmpty(remoteChildTaskId))

        val endTime = if (localTaskHierarchy.getEndExactTimeStamp() != null) localTaskHierarchy.getEndExactTimeStamp()!!.long else null

        val taskHierarchyJson = TaskHierarchyJson(remoteParentTaskId, remoteChildTaskId, localTaskHierarchy.startExactTimeStamp.long, endTime, localTaskHierarchy.ordinal)
        val remoteTaskHierarchyRecord = remoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson)

        val remoteTaskHierarchy = RemoteTaskHierarchy(kotlinDomainFactory, this, remoteTaskHierarchyRecord)

        remoteTaskHierarchies.add(remoteTaskHierarchy.id, remoteTaskHierarchy)

        return remoteTaskHierarchy
    }

    fun updateRecordOf(addedFriends: Set<RemoteRootUser>, removedFriends: Set<String>) {
        remoteProjectRecord.updateRecordOf(addedFriends.asSequence().map { it.id }.toSet(), removedFriends)

        for (addedFriend in addedFriends)
            addUser(addedFriend)

        for (removedFriend in removedFriends) {
            check(remoteUsers.containsKey(removedFriend))

            remoteUsers[removedFriend]!!.delete()
        }
    }

    private fun addUser(remoteRootUser: RemoteRootUser) {
        val id = remoteRootUser.id

        check(!remoteUsers.containsKey(id))

        val remoteProjectUserRecord = remoteProjectRecord.newRemoteUserRecord(remoteRootUser.userJson)
        val remoteProjectUser = RemoteProjectUser(this, remoteProjectUserRecord)

        remoteUsers[id] = remoteProjectUser
    }

    fun deleteTask(remoteTask: RemoteTask) {
        check(remoteTasks.containsKey(remoteTask.id))

        remoteTasks.remove(remoteTask.id)
    }

    fun deleteTaskHierarchy(remoteTasHierarchy: RemoteTaskHierarchy) = remoteTaskHierarchies.removeForce(remoteTasHierarchy.id)

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

    fun getRemoteCustomTime(remoteCustomTimeId: String): RemoteCustomTime {
        check(remoteCustomTimes.containsKey(remoteCustomTimeId))

        return remoteCustomTimes[remoteCustomTimeId]!!
    }

    fun newRemoteCustomTime(customTimeJson: CustomTimeJson): RemoteCustomTime {
        val remoteCustomTimeRecord = remoteProjectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = RemoteCustomTime(kotlinDomainFactory, this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    fun deleteCustomTime(remoteCustomTime: RemoteCustomTime) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    fun deleteUser(remoteProjectUser: RemoteProjectUser) {
        val id = remoteProjectUser.id
        check(remoteUsers.containsKey(id))

        remoteUsers.remove(id)
    }

    fun updateUserInfo(userInfo: UserInfo, uuid: String) {
        val key = userInfo.key
        check(remoteUsers.containsKey(key))

        val remoteProjectUser = remoteUsers[key]!!

        remoteProjectUser.name = userInfo.name
        remoteProjectUser.setToken(userInfo.token, uuid)
    }

    fun delete() {
        remoteFactory.deleteProject(this)

        remoteProjectRecord.delete()
    }

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val endExactTimeStamp = endExactTimeStamp

        return startExactTimeStamp <= exactTimeStamp && (endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp)
    }

    fun setEndExactTimeStamp(now: ExactTimeStamp) {
        check(current(now))

        remoteTasks.values
                .filter { it.current(now) }
                .forEach { it.setEndExactTimeStamp(now) }

        remoteProjectRecord.setEndTime(now.long)
    }

    fun getTaskHierarchy(id: String) = remoteTaskHierarchies.getById(id)
}
