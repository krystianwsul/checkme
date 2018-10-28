package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.json.CustomTimeJson
import com.krystianwsul.checkme.firebase.json.JsonWrapper
import com.krystianwsul.checkme.firebase.json.ProjectJson
import com.krystianwsul.checkme.firebase.json.TaskJson
import com.krystianwsul.checkme.firebase.records.RemoteProjectManager
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import java.util.*

class RemoteProjectFactory(
        private val kotlinDomainFactory: KotlinDomainFactory,
        children: Iterable<DataSnapshot>,
        private val userInfo: UserInfo,
        private val uuid: String, now: ExactTimeStamp) {

    private val remoteProjectManager = RemoteProjectManager(kotlinDomainFactory, children)

    val remoteProjects = remoteProjectManager.remoteProjectRecords
            .values
            .map { RemoteProject(kotlinDomainFactory, it, userInfo, uuid, now) }
            .associateBy { it.id }
            .toMutableMap()

    val isSaved get() = remoteProjectManager.isSaved

    val tasks get() = remoteProjects.values.flatMap { it.tasks }

    val remoteCustomTimes get() = remoteProjects.values.flatMap { it.customTimes }

    val instanceCount
        get() = remoteProjects.values
                .flatMap { it.tasks }
                .asSequence()
                .map { it.existingInstances.size }
                .sum()

    val existingInstances
        get() = remoteProjects.values
                .flatMap { it.tasks }
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
                .map { it.tasks.size }
                .sum()

    fun createScheduleRootTask(now: ExactTimeStamp, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?, projectId: String) = createRemoteTaskHelper(now, name, note, projectId).apply {
        createSchedules(now, scheduleDatas)
    }

    fun createRemoteTaskHelper(now: ExactTimeStamp, name: String, note: String?, projectId: String): RemoteTask {
        val taskJson = TaskJson(name, now.long, null, null, null, null, note)

        return getRemoteProjectForce(projectId).newRemoteTask(taskJson, now)
    }

    fun createRemoteProject(name: String, now: ExactTimeStamp, recordOf: Set<String>, remoteRootUser: RemoteRootUser): RemoteProject {
        check(!TextUtils.isEmpty(name))

        val friendIds = HashSet(recordOf)
        friendIds.remove(userInfo.key)

        val userJsons = RemoteFriendFactory.getUserJsons(friendIds)
        userJsons[userInfo.key] = remoteRootUser.userJson

        val projectJson = ProjectJson(name, now.long, users = userJsons)

        val remoteProjectRecord = remoteProjectManager.newRemoteProjectRecord(kotlinDomainFactory, JsonWrapper(recordOf, projectJson))

        val remoteProject = RemoteProject(kotlinDomainFactory, remoteProjectRecord, userInfo, uuid, now)

        check(!this.remoteProjects.containsKey(remoteProject.id))

        this.remoteProjects[remoteProject.id] = remoteProject

        return remoteProject
    }

    fun save() {
        check(!remoteProjectManager.isSaved)

        remoteProjectManager.save()
    }

    fun getRemoteCustomTimeId(customTimeKey: CustomTimeKey, remoteProject: RemoteProject): String {
        check(customTimeKey.localCustomTimeId != null)
        check(TextUtils.isEmpty(customTimeKey.remoteCustomTimeId))

        val localCustomTimeId = customTimeKey.localCustomTimeId!!

        val localCustomTime = kotlinDomainFactory.localFactory.getLocalCustomTime(localCustomTimeId)

        if (!localCustomTime.hasRemoteRecord(remoteProject.id)) {
            val customTimeJson = CustomTimeJson(kotlinDomainFactory.localFactory.uuid, localCustomTime.id, localCustomTime.name, localCustomTime.getHourMinute(DayOfWeek.SUNDAY).hour, localCustomTime.getHourMinute(DayOfWeek.SUNDAY).minute, localCustomTime.getHourMinute(DayOfWeek.MONDAY).hour, localCustomTime.getHourMinute(DayOfWeek.MONDAY).minute, localCustomTime.getHourMinute(DayOfWeek.TUESDAY).hour, localCustomTime.getHourMinute(DayOfWeek.TUESDAY).minute, localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).hour, localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).minute, localCustomTime.getHourMinute(DayOfWeek.THURSDAY).hour, localCustomTime.getHourMinute(DayOfWeek.THURSDAY).minute, localCustomTime.getHourMinute(DayOfWeek.FRIDAY).hour, localCustomTime.getHourMinute(DayOfWeek.FRIDAY).minute, localCustomTime.getHourMinute(DayOfWeek.SATURDAY).hour, localCustomTime.getHourMinute(DayOfWeek.SATURDAY).minute)

            val remoteCustomTime = remoteProject.newRemoteCustomTime(customTimeJson)

            localCustomTime.addRemoteCustomTimeRecord(remoteCustomTime.remoteCustomTimeRecord)
        }

        return localCustomTime.getRemoteId(remoteProject.id)
    }

    fun getRemoteCustomTime(remoteProjectId: String, remoteCustomTimeId: String): RemoteCustomTime {
        check(!TextUtils.isEmpty(remoteProjectId))
        check(!TextUtils.isEmpty(remoteCustomTimeId))

        check(this.remoteProjects.containsKey(remoteProjectId))

        val remoteProject = this.remoteProjects[remoteProjectId]
        check(remoteProject != null)

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
        check(!TextUtils.isEmpty(taskKey.remoteProjectId))
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))

        return remoteProjects[taskKey.remoteProjectId]
    }

    fun getTaskForce(taskKey: TaskKey): RemoteTask {
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))

        return getRemoteProjectForce(taskKey).getRemoteTaskForce(taskKey.remoteTaskId!!)
    }

    fun getTaskIfPresent(taskKey: TaskKey): RemoteTask? {
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))

        val remoteProject = getRemoteProjectIfPresent(taskKey) ?: return null

        return remoteProject.getRemoteTaskIfPresent(taskKey.remoteTaskId!!)
    }

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<RemoteTaskHierarchy> {
        check(!TextUtils.isEmpty(childTaskKey.remoteTaskId))

        return getRemoteProjectForce(childTaskKey).getTaskHierarchiesByChildTaskKey(childTaskKey)
    }

    fun updateUserInfo(userInfo: UserInfo) = remoteProjects.values.forEach { it.updateUserInfo(userInfo, uuid) }

    fun getRemoteProjectForce(projectId: String): RemoteProject {
        check(!TextUtils.isEmpty(projectId))
        check(remoteProjects.containsKey(projectId))

        return remoteProjects[projectId]!!
    }

    fun deleteProject(remoteProject: RemoteProject) {
        val projectId = remoteProject.id

        check(remoteProjects.containsKey(projectId))
        remoteProjects.remove(projectId)
    }
}
