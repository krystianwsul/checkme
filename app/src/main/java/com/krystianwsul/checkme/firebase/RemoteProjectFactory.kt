package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.json.*
import com.krystianwsul.checkme.firebase.records.RemoteProjectManager
import com.krystianwsul.checkme.loaders.CreateTaskLoader
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import junit.framework.Assert
import java.util.*

class RemoteProjectFactory(private val domainFactory: DomainFactory, children: Iterable<DataSnapshot>, private val userInfo: UserInfo, private val uuid: String, now: ExactTimeStamp) {

    private val remoteProjectManager = RemoteProjectManager(domainFactory, children)

    val remoteProjects = remoteProjectManager.remoteProjectRecords
            .values
            .map { RemoteProject(domainFactory, it, userInfo, uuid, now) }
            .associateBy { it.id }
            .toMutableMap()

    val isSaved get() = remoteProjectManager.isSaved

    val tasks get() = remoteProjects.values.flatMap { it.remoteTasks }

    val remoteCustomTimes get() = remoteProjects.values.flatMap { it.remoteCustomTimes }

    val instanceCount
        get() = remoteProjects.values
                .flatMap { it.remoteTasks }
                .map { it.existingInstances.size }
                .sum()

    val existingInstances
        get() = remoteProjects.values
                .flatMap { it.remoteTasks }
                .flatMap { it.existingInstances.values }

    val taskKeys
        get() = remoteProjects.values
                .flatMap {
                    val projectId = it.id

                    it.taskIds.map { TaskKey(projectId, it) }
                }
                .toSet()

    val taskCount: Int
        get() = remoteProjects.values
                .map { it.remoteTasks.size }
                .sum()

    fun createScheduleRootTask(now: ExactTimeStamp, name: String, scheduleDatas: List<CreateTaskLoader.ScheduleData>, note: String?, projectId: String) = createRemoteTaskHelper(now, name, note, projectId).apply {
        createSchedules(now, scheduleDatas)
    }

    fun createRemoteTaskHelper(now: ExactTimeStamp, name: String, note: String?, projectId: String): RemoteTask {
        val taskJson = TaskJson(name, now.long, null, null, null, null, note, emptyMap<String, InstanceJson>())

        return getRemoteProjectForce(projectId).newRemoteTask(taskJson, now)
    }

    fun createRemoteProject(name: String, now: ExactTimeStamp, recordOf: Set<String>, remoteRootUser: RemoteRootUser): RemoteProject {
        Assert.assertTrue(!TextUtils.isEmpty(name))

        val friendIds = HashSet(recordOf)
        friendIds.remove(userInfo.key)

        val userJsons = domainFactory.getUserJsons(friendIds)
        userJsons[userInfo.key] = remoteRootUser.userJson

        val projectJson = ProjectJson(name, now.long, null, HashMap(), HashMap<String, TaskHierarchyJson>(), HashMap(), userJsons)

        val remoteProjectRecord = remoteProjectManager.newRemoteProjectRecord(domainFactory, JsonWrapper(recordOf, projectJson))

        val remoteProject = RemoteProject(domainFactory, remoteProjectRecord, userInfo, uuid, now)

        Assert.assertTrue(!this.remoteProjects.containsKey(remoteProject.id))

        this.remoteProjects[remoteProject.id] = remoteProject

        return remoteProject
    }

    fun save() {
        Assert.assertTrue(!remoteProjectManager.isSaved)

        remoteProjectManager.save()
    }

    fun getRemoteCustomTimeId(customTimeKey: CustomTimeKey, remoteProject: RemoteProject): String {
        Assert.assertTrue(customTimeKey.localCustomTimeId != null)
        Assert.assertTrue(TextUtils.isEmpty(customTimeKey.remoteCustomTimeId))

        val localCustomTimeId = customTimeKey.localCustomTimeId!!

        val localCustomTime = domainFactory.localFactory.getLocalCustomTime(localCustomTimeId)

        if (!localCustomTime.hasRemoteRecord(remoteProject.id)) {
            val customTimeJson = CustomTimeJson(domainFactory.localFactory.uuid, localCustomTime.id, localCustomTime.name, localCustomTime.getHourMinute(DayOfWeek.SUNDAY).hour, localCustomTime.getHourMinute(DayOfWeek.SUNDAY).minute, localCustomTime.getHourMinute(DayOfWeek.MONDAY).hour, localCustomTime.getHourMinute(DayOfWeek.MONDAY).minute, localCustomTime.getHourMinute(DayOfWeek.TUESDAY).hour, localCustomTime.getHourMinute(DayOfWeek.TUESDAY).minute, localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).hour, localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).minute, localCustomTime.getHourMinute(DayOfWeek.THURSDAY).hour, localCustomTime.getHourMinute(DayOfWeek.THURSDAY).minute, localCustomTime.getHourMinute(DayOfWeek.FRIDAY).hour, localCustomTime.getHourMinute(DayOfWeek.FRIDAY).minute, localCustomTime.getHourMinute(DayOfWeek.SATURDAY).hour, localCustomTime.getHourMinute(DayOfWeek.SATURDAY).minute)

            val remoteCustomTime = remoteProject.newRemoteCustomTime(customTimeJson)

            localCustomTime.addRemoteCustomTimeRecord(remoteCustomTime.remoteCustomTimeRecord)
        }

        return localCustomTime.getRemoteId(remoteProject.id)
    }

    fun getRemoteCustomTime(remoteProjectId: String, remoteCustomTimeId: String): RemoteCustomTime {
        Assert.assertTrue(!TextUtils.isEmpty(remoteProjectId))
        Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTimeId))

        Assert.assertTrue(this.remoteProjects.containsKey(remoteProjectId))

        val remoteProject = this.remoteProjects[remoteProjectId]
        Assert.assertTrue(remoteProject != null)

        return remoteProject!!.getRemoteCustomTime(remoteCustomTimeId)
    }

    fun getExistingInstanceIfPresent(instanceKey: InstanceKey): RemoteInstance? {
        val taskKey = instanceKey.taskKey

        if (TextUtils.isEmpty(taskKey.remoteTaskId))
            return null

        val remoteTask = getRemoteProjectForce(taskKey).getRemoteTaskIfPresent(taskKey.remoteTaskId!!)
                ?: return null

        return remoteTask.getExistingInstanceIfPresent(instanceKey.scheduleKey)
    }

    private fun getRemoteProjectForce(taskKey: TaskKey) = getRemoteProjectIfPresent(taskKey)!!

    private fun getRemoteProjectIfPresent(taskKey: TaskKey): RemoteProject? {
        Assert.assertTrue(!TextUtils.isEmpty(taskKey.remoteProjectId))
        Assert.assertTrue(!TextUtils.isEmpty(taskKey.remoteTaskId))

        return remoteProjects[taskKey.remoteProjectId]
    }

    fun getTaskForce(taskKey: TaskKey): RemoteTask {
        Assert.assertTrue(!TextUtils.isEmpty(taskKey.remoteTaskId))

        return getRemoteProjectForce(taskKey).getRemoteTaskForce(taskKey.remoteTaskId!!)
    }

    fun getTaskIfPresent(taskKey: TaskKey): RemoteTask? {
        Assert.assertTrue(!TextUtils.isEmpty(taskKey.remoteTaskId))

        val remoteProject = getRemoteProjectIfPresent(taskKey) ?: return null

        return remoteProject.getRemoteTaskIfPresent(taskKey.remoteTaskId!!)
    }

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<RemoteTaskHierarchy> {
        Assert.assertTrue(!TextUtils.isEmpty(childTaskKey.remoteTaskId))

        return getRemoteProjectForce(childTaskKey).getTaskHierarchiesByChildTaskKey(childTaskKey)
    }

    fun updateUserInfo(userInfo: UserInfo) = remoteProjects.values.forEach { it.updateUserInfo(userInfo, uuid) }

    fun getRemoteProjectForce(projectId: String): RemoteProject {
        Assert.assertTrue(!TextUtils.isEmpty(projectId))
        Assert.assertTrue(remoteProjects.containsKey(projectId))

        return remoteProjects[projectId]!!
    }

    fun deleteProject(remoteProject: RemoteProject) {
        val projectId = remoteProject.id

        Assert.assertTrue(remoteProjects.containsKey(projectId))
        remoteProjects.remove(projectId)
    }
}
