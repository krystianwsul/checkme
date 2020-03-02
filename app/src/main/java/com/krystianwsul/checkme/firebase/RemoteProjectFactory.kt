package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.androidhuman.rxfirebase2.database.ChildAddEvent
import com.androidhuman.rxfirebase2.database.ChildChangeEvent
import com.androidhuman.rxfirebase2.database.ChildEvent
import com.androidhuman.rxfirebase2.database.ChildRemoveEvent
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.managers.AndroidRemotePrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidRemoteSharedProjectManager
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.firebase.records.RemoteTaskRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

class RemoteProjectFactory(
        deviceDbInfo: DeviceDbInfo,
        private val localFactory: LocalFactory,
        sharedChildren: Iterable<DataSnapshot>,
        privateSnapshot: DataSnapshot,
        now: ExactTimeStamp
) : RemoteProject.Parent {

    private val remotePrivateProjectManager = AndroidRemotePrivateProjectManager(deviceDbInfo.userInfo, privateSnapshot, now)
    private val remoteSharedProjectManager = AndroidRemoteSharedProjectManager(sharedChildren)

    var remotePrivateProject = RemotePrivateProject(remotePrivateProjectManager.remoteProjectRecord).apply { fixNotificationShown(localFactory, now) }
        private set

    val remoteSharedProjects = remoteSharedProjectManager.remoteProjectRecords
            .values
            .associate {
                it.id to RemoteSharedProject(it).apply {
                    fixNotificationShown(localFactory, now)
                    updateDeviceDbInfo(deviceDbInfo)
                }
            }
            .toMutableMap()

    val remoteProjects
        get() = remoteSharedProjects.toMutableMap<ProjectKey, RemoteProject<*, *>>().apply {
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

    fun onChildEvent(deviceDbInfo: DeviceDbInfo, childEvent: ChildEvent, now: ExactTimeStamp) {
        when (childEvent) {
            is ChildAddEvent, is ChildChangeEvent -> {
                try {
                    val remoteProjectRecord = remoteSharedProjectManager.setChild(childEvent.dataSnapshot())

                    remoteSharedProjects[remoteProjectRecord.id] = RemoteSharedProject(remoteProjectRecord).apply {
                        fixNotificationShown(localFactory, now)
                        updateDeviceDbInfo(deviceDbInfo)
                    }
                } catch (onlyVisibilityPresentException: RemoteTaskRecord.OnlyVisibilityPresentException) {
                    // hack for oldestVisible being set on records removed by cloud function
                }
            }
            is ChildRemoveEvent -> {
                val key = remoteSharedProjectManager.removeChild(childEvent.dataSnapshot())

                remoteSharedProjects.remove(key)
            }
            else -> throw IllegalArgumentException()
        }
    }

    fun onNewPrivate(dataSnapshot: DataSnapshot, now: ExactTimeStamp) {
        try {
            val remotePrivateProjectRecord = remotePrivateProjectManager.newSnapshot(dataSnapshot)

            remotePrivateProject = RemotePrivateProject(remotePrivateProjectRecord).apply { fixNotificationShown(localFactory, now) }
        } catch (onlyVisibilityPresentException: RemoteTaskRecord.OnlyVisibilityPresentException) {
            // hack for oldestVisible being set on records removed by cloud function

            /*
            todo: either set oldestVisible once you're sure the database has been updated, or find
              a better mechanism than oldestVisible
             */
        }
    }

    fun createScheduleRootTask(
            now: ExactTimeStamp,
            name: String,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            note: String?,
            projectId: ProjectKey,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo
    ) = createRemoteTaskHelper(now, name, note, projectId, imageUuid, deviceDbInfo).apply {
        createSchedules(remotePrivateProject.id.toUserKey(), now, scheduleDatas)
    }

    fun createRemoteTaskHelper(
            now: ExactTimeStamp,
            name: String,
            note: String?,
            projectId: ProjectKey,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo
    ): RemoteTask<*, *> {
        val image = imageUuid?.let { TaskJson.Image(imageUuid, deviceDbInfo.uuid) }
        val taskJson = TaskJson(name, now.long, null, note, image = image)

        return getRemoteProjectForce(projectId).newRemoteTask(taskJson)
    }

    fun createRemoteProject(
            name: String,
            now: ExactTimeStamp,
            recordOf: Set<UserKey>,
            remoteRootUser: RemoteRootUser,
            userInfo: UserInfo,
            remoteFriendFactory: RemoteFriendFactory
    ): RemoteSharedProject {
        check(!TextUtils.isEmpty(name))

        val friendIds = recordOf.toMutableSet()
        friendIds.remove(userInfo.key)

        val userJsons = remoteFriendFactory.getUserJsons(friendIds)
        userJsons[userInfo.key] = remoteRootUser.userJson

        val projectJson = SharedProjectJson(
                name,
                now.long,
                users = userJsons.mapKeys { it.key.key }.toMutableMap()
        )

        val remoteProjectRecord = remoteSharedProjectManager.newRemoteProjectRecord(JsonWrapper(
                recordOf.map { it.key }.toSet(),
                projectJson
        ))

        val remoteProject = RemoteSharedProject(remoteProjectRecord)

        check(!remoteProjects.containsKey(remoteProject.id))

        remoteSharedProjects[remoteProject.id] = remoteProject

        return remoteProject
    }

    fun save(domainFactory: DomainFactory): Boolean {
        val privateSaved = remotePrivateProjectManager.save(domainFactory)
        val sharedSaved = remoteSharedProjectManager.save(domainFactory)
        return privateSaved || sharedSaved
    }

    fun getRemoteCustomTime(remoteProjectId: ProjectKey, remoteCustomTimeId: RemoteCustomTimeId): RemoteCustomTime<*, *> {
        check(remoteProjects.containsKey(remoteProjectId))

        return remoteProjects.getValue(remoteProjectId).getRemoteCustomTime(remoteCustomTimeId)
    }

    fun getExistingInstanceIfPresent(instanceKey: InstanceKey): RemoteInstance<*, *>? {
        val taskKey = instanceKey.taskKey

        if (TextUtils.isEmpty(taskKey.remoteTaskId))
            return null

        val remoteTask = getRemoteProjectForce(taskKey).getRemoteTaskIfPresent(taskKey.remoteTaskId)
                ?: return null

        return remoteTask.getExistingInstanceIfPresent(instanceKey.scheduleKey)
    }

    private fun getRemoteProjectForce(taskKey: TaskKey) = getRemoteProjectIfPresent(taskKey)!!

    private fun getRemoteProjectIfPresent(taskKey: TaskKey): RemoteProject<*, *>? {
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))

        return remoteProjects[taskKey.remoteProjectId]
    }

    fun getTaskForce(taskKey: TaskKey): RemoteTask<*, *> {
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))

        return getRemoteProjectForce(taskKey).getRemoteTaskForce(taskKey.remoteTaskId)
    }

    fun getTaskIfPresent(taskKey: TaskKey): RemoteTask<*, *>? {
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))

        return getRemoteProjectIfPresent(taskKey)?.getRemoteTaskIfPresent(taskKey.remoteTaskId)
    }

    fun updateDeviceInfo(deviceDbInfo: DeviceDbInfo) = remoteSharedProjects.values.forEach { it.updateDeviceDbInfo(deviceDbInfo) }

    fun updatePhotoUrl(deviceInfo: DeviceInfo, photoUrl: String) = remoteSharedProjects.values.forEach { it.updatePhotoUrl(deviceInfo, photoUrl) }

    fun getRemoteProjectForce(projectId: ProjectKey): RemoteProject<*, *> {
        check(remoteProjects.containsKey(projectId))

        return remoteProjects.getValue(projectId)
    }

    fun getRemoteProjectIfPresent(projectId: ProjectKey) = remoteProjects[projectId]

    fun getRemoteProjectIfPresent(projectId: String) = remoteProjects.entries
            .singleOrNull { it.key.key == projectId }
            ?.value

    override fun deleteProject(remoteProject: RemoteProject<*, *>) {
        val projectId = remoteProject.id

        check(remoteProjects.containsKey(projectId))
        remoteSharedProjects.remove(projectId)
    }

    fun getTaskHierarchy(remote: TaskHierarchyKey.Remote) = remoteProjects.getValue(remote.projectId).getTaskHierarchy(remote.taskHierarchyId)

    fun getSchedule(scheduleId: ScheduleId.Remote) = remoteProjects.getValue(scheduleId.projectId).getRemoteTaskForce(scheduleId.taskId)
            .schedules
            .single { it.scheduleId == scheduleId }
}
