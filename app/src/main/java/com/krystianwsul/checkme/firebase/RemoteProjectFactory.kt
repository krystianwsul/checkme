package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.androidhuman.rxfirebase2.database.ChildAddEvent
import com.androidhuman.rxfirebase2.database.ChildChangeEvent
import com.androidhuman.rxfirebase2.database.ChildEvent
import com.androidhuman.rxfirebase2.database.ChildRemoveEvent
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.managers.RemotePrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.RemoteSharedProjectManager
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*
import java.util.*

class RemoteProjectFactory(
        private val domainFactory: DomainFactory,
        sharedChildren: Iterable<DataSnapshot>,
        privateSnapshot: DataSnapshot,
        now: ExactTimeStamp) : RemoteProject.Parent {

    private val remotePrivateProjectManager = RemotePrivateProjectManager(domainFactory, privateSnapshot, now)
    private val remoteSharedProjectManager = RemoteSharedProjectManager(domainFactory, sharedChildren)

    var remotePrivateProject = RemotePrivateProject(remotePrivateProjectManager.remoteProjectRecord).apply { fixNotificationShown(domainFactory.localFactory, now) }
        private set

    val remoteSharedProjects = remoteSharedProjectManager.remoteProjectRecords
            .values
            .map { RemoteSharedProject(it, domainFactory.deviceDbInfo).apply { fixNotificationShown(domainFactory.localFactory, now) } }
            .associateBy { it.id }
            .toMutableMap()

    val remoteProjects
        get() = remoteSharedProjects.toMutableMap<String, RemoteProject<*>>().apply {
            put(remotePrivateProject.id, remotePrivateProject)
        }.toMap()

    var isPrivateSaved
        get() = remotePrivateProjectManager.isSaved
        set(value) {
            remotePrivateProjectManager.isSaved = value
        }

    var isSharedSaved
        get() = remoteSharedProjectManager.isSaved
        set(value) {
            remoteSharedProjectManager.isSaved = value
        }

    val eitherSaved get() = isPrivateSaved || isSharedSaved

    val tasks get() = remoteProjects.values.flatMap { it.tasks }

    val remoteCustomTimes get() = remoteProjects.values.flatMap { it.customTimes }

    val instanceCount
        get() = remoteProjects.values
                .flatMap { it.tasks }
                .asSequence()
                .map { it.existingInstances.size }
                .sum()

    val existingInstances
        get() = remoteProjects.values.flatMap { it.existingInstances }

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

    fun onChildEvent(childEvent: ChildEvent, now: ExactTimeStamp) {
        when (childEvent) {
            is ChildAddEvent, is ChildChangeEvent -> {
                val remoteProjectRecord = remoteSharedProjectManager.changeChild(childEvent.dataSnapshot())

                check(remoteProjects.containsKey(remoteProjectRecord.id))
                remoteSharedProjects[remoteProjectRecord.id] = RemoteSharedProject(remoteProjectRecord, domainFactory.deviceDbInfo).apply { fixNotificationShown(domainFactory.localFactory, now) }
            }
            is ChildRemoveEvent -> {
                val key = remoteSharedProjectManager.removeChild(childEvent.dataSnapshot())

                remoteSharedProjects.remove(key)
            }
            else -> throw IllegalArgumentException()
        }
    }

    fun onNewPrivate(dataSnapshot: DataSnapshot, now: ExactTimeStamp) {
        val remotePrivateProjectRecord = remotePrivateProjectManager.newSnapshot(dataSnapshot)

        remotePrivateProject = RemotePrivateProject(remotePrivateProjectRecord).apply { fixNotificationShown(domainFactory.localFactory, now) }
    }

    fun createScheduleRootTask(now: ExactTimeStamp, name: String, scheduleDatas: List<Pair<ScheduleData, Time>>, note: String?, projectId: String, uuid: String?) = createRemoteTaskHelper(now, name, note, projectId, uuid).apply {
        createSchedules(remotePrivateProject.id, now, scheduleDatas)
    }

    fun createRemoteTaskHelper(now: ExactTimeStamp, name: String, note: String?, projectId: String, imageUuid: String?): RemoteTask<*> {
        val image = imageUuid?.let { TaskJson.Image(imageUuid, domainFactory.deviceDbInfo.uuid) }
        val taskJson = TaskJson(name, now.long, null, null, null, null, note, image = image)

        return getRemoteProjectForce(projectId).newRemoteTask(taskJson)
    }

    fun createRemoteProject(name: String, now: ExactTimeStamp, recordOf: Set<String>, remoteRootUser: RemoteRootUser): RemoteSharedProject {
        check(!TextUtils.isEmpty(name))

        val friendIds = HashSet(recordOf)
        friendIds.remove(domainFactory.deviceDbInfo.key)

        val userJsons = domainFactory.remoteFriendFactory.getUserJsons(friendIds)
        userJsons[domainFactory.deviceDbInfo.key] = remoteRootUser.userJson

        val projectJson = SharedProjectJson(name, now.long, users = userJsons)

        val remoteProjectRecord = remoteSharedProjectManager.newRemoteProjectRecord(domainFactory, JsonWrapper(recordOf, projectJson))

        val remoteProject = RemoteSharedProject(remoteProjectRecord, domainFactory.deviceDbInfo)

        check(!remoteProjects.containsKey(remoteProject.id))

        remoteSharedProjects[remoteProject.id] = remoteProject

        return remoteProject
    }

    fun save(): Boolean {
        val privateSaved = remotePrivateProjectManager.save()
        val sharedSaved = remoteSharedProjectManager.save()
        return privateSaved || sharedSaved
    }

    fun getRemoteCustomTime(remoteProjectId: String, remoteCustomTimeId: RemoteCustomTimeId): RemoteCustomTime<*> {
        check(!TextUtils.isEmpty(remoteProjectId))

        check(remoteProjects.containsKey(remoteProjectId))

        return remoteProjects.getValue(remoteProjectId).getRemoteCustomTime(remoteCustomTimeId)
    }

    fun getExistingInstanceIfPresent(instanceKey: InstanceKey): RemoteInstance<*>? {
        val taskKey = instanceKey.taskKey

        if (TextUtils.isEmpty(taskKey.remoteTaskId))
            return null

        val remoteTask = getRemoteProjectForce(taskKey).getRemoteTaskIfPresent(taskKey.remoteTaskId)
                ?: return null

        return remoteTask.getExistingInstanceIfPresent(instanceKey.scheduleKey)
    }

    private fun getRemoteProjectForce(taskKey: TaskKey) = getRemoteProjectIfPresent(taskKey)!!

    private fun getRemoteProjectIfPresent(taskKey: TaskKey): RemoteProject<*>? {
        check(!TextUtils.isEmpty(taskKey.remoteProjectId))
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))

        return remoteProjects[taskKey.remoteProjectId]
    }

    fun getTaskForce(taskKey: TaskKey): RemoteTask<*> {
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))

        return getRemoteProjectForce(taskKey).getRemoteTaskForce(taskKey.remoteTaskId)
    }

    fun getTaskIfPresent(taskKey: TaskKey): RemoteTask<*>? {
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))

        return getRemoteProjectIfPresent(taskKey)?.getRemoteTaskIfPresent(taskKey.remoteTaskId)
    }

    fun updateDeviceInfo(deviceDbInfo: DeviceDbInfo) = remoteSharedProjects.values.forEach { it.updateUserInfo(deviceDbInfo) }

    fun updatePhotoUrl(deviceInfo: DeviceInfo, photoUrl: String) = remoteSharedProjects.values.forEach { it.updatePhotoUrl(deviceInfo, photoUrl) }

    fun getRemoteProjectForce(projectId: String): RemoteProject<*> {
        check(!TextUtils.isEmpty(projectId))
        check(remoteProjects.containsKey(projectId))

        return remoteProjects.getValue(projectId)
    }

    fun getRemoteProjectIfPresent(projectId: String): RemoteProject<*>? {
        check(!TextUtils.isEmpty(projectId))

        return remoteProjects[projectId]
    }

    override fun deleteProject(remoteProject: RemoteProject<*>) {
        val projectId = remoteProject.id

        check(remoteProjects.containsKey(projectId))
        remoteSharedProjects.remove(projectId)
    }

    fun getTaskHierarchy(remote: TaskHierarchyKey.Remote) = remoteProjects.getValue(remote.projectId).getTaskHierarchy(remote.taskHierarchyId)

    fun getSchedule(scheduleId: ScheduleId.Remote) = remoteProjects.getValue(scheduleId.projectId).getRemoteTaskForce(scheduleId.taskId)
            .schedules
            .single { it.scheduleId == scheduleId }
}
