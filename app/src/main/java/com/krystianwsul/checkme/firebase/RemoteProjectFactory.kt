package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.androidhuman.rxfirebase2.database.ChildAddEvent
import com.androidhuman.rxfirebase2.database.ChildChangeEvent
import com.androidhuman.rxfirebase2.database.ChildEvent
import com.androidhuman.rxfirebase2.database.ChildRemoveEvent
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.json.JsonWrapper
import com.krystianwsul.checkme.firebase.json.SharedProjectJson
import com.krystianwsul.checkme.firebase.json.TaskJson
import com.krystianwsul.checkme.firebase.records.RemotePrivateProjectManager
import com.krystianwsul.checkme.firebase.records.RemoteSharedProjectManager
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import java.util.*

class RemoteProjectFactory(
        private val domainFactory: DomainFactory,
        sharedChildren: Iterable<DataSnapshot>,
        privateSnapshot: DataSnapshot,
        private val userInfo: UserInfo,
        now: ExactTimeStamp) {

    val uuid = domainFactory.uuid

    private val remotePrivateProjectManager = RemotePrivateProjectManager(domainFactory, userInfo, privateSnapshot, now)
    private val remoteSharedProjectManager = RemoteSharedProjectManager(domainFactory, sharedChildren)

    var remotePrivateProject = RemotePrivateProject(domainFactory, remotePrivateProjectManager.remoteProjectRecord, uuid, now)
        private set

    val remoteSharedProjects = remoteSharedProjectManager.remoteProjectRecords
            .values
            .map { RemoteSharedProject(domainFactory, it, userInfo, uuid, now) }
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

    fun onChildEvent(childEvent: ChildEvent, now: ExactTimeStamp) {
        when (childEvent) {
            is ChildAddEvent -> {
                val remoteProjectRecord = remoteSharedProjectManager.addChild(childEvent.dataSnapshot())

                check(!remoteProjects.containsKey(remoteProjectRecord.id))
                remoteSharedProjects[remoteProjectRecord.id] = RemoteSharedProject(domainFactory, remoteProjectRecord, userInfo, uuid, now)
            }
            is ChildChangeEvent -> {
                val remoteProjectRecord = remoteSharedProjectManager.changeChild(childEvent.dataSnapshot())

                check(remoteProjects.containsKey(remoteProjectRecord.id))
                remoteSharedProjects[remoteProjectRecord.id] = RemoteSharedProject(domainFactory, remoteProjectRecord, userInfo, uuid, now)
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

        remotePrivateProject = RemotePrivateProject(domainFactory, remotePrivateProjectRecord, uuid, now)
    }

    fun createScheduleRootTask(now: ExactTimeStamp, name: String, scheduleDatas: List<CreateTaskViewModel.ScheduleData>, note: String?, projectId: String) = createRemoteTaskHelper(now, name, note, projectId).apply {
        createSchedules(now, scheduleDatas)
    }

    fun createRemoteTaskHelper(now: ExactTimeStamp, name: String, note: String?, projectId: String): RemoteTask<*> {
        val taskJson = TaskJson(name, now.long, null, null, null, null, note)

        return getRemoteProjectForce(projectId).newRemoteTask(taskJson, now)
    }

    fun createRemoteProject(name: String, now: ExactTimeStamp, recordOf: Set<String>, remoteRootUser: RemoteRootUser): RemoteSharedProject {
        check(!TextUtils.isEmpty(name))

        val friendIds = HashSet(recordOf)
        friendIds.remove(userInfo.key)

        val userJsons = domainFactory.remoteFriendFactory.getUserJsons(friendIds)
        userJsons[userInfo.key] = remoteRootUser.userJson

        val projectJson = SharedProjectJson(name, now.long, users = userJsons)

        val remoteProjectRecord = remoteSharedProjectManager.newRemoteProjectRecord(domainFactory, JsonWrapper(recordOf, projectJson))

        val remoteProject = RemoteSharedProject(domainFactory, remoteProjectRecord, userInfo, uuid, now)

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

    fun updateUserInfo(userInfo: UserInfo) = remoteSharedProjects.values.forEach { it.updateUserInfo(userInfo, uuid) }

    fun updatePhotoUrl(userInfo: UserInfo, photoUrl: String) = remoteSharedProjects.values.forEach { it.updatePhotoUrl(userInfo, photoUrl) }

    fun getRemoteProjectForce(projectId: String): RemoteProject<*> {
        check(!TextUtils.isEmpty(projectId))
        check(remoteProjects.containsKey(projectId))

        return remoteProjects.getValue(projectId)
    }

    fun getRemoteProjectIfPresent(projectId: String): RemoteProject<*>? {
        check(!TextUtils.isEmpty(projectId))

        return remoteProjects[projectId]
    }

    fun deleteProject(remoteProject: RemoteProject<*>) {
        val projectId = remoteProject.id

        check(remoteProjects.containsKey(projectId))
        remoteSharedProjects.remove(projectId)
    }

    fun getTaskHierarchy(remoteTaskHierarchyKey: TaskHierarchyKey.RemoteTaskHierarchyKey) = remoteProjects.getValue(remoteTaskHierarchyKey.projectId).getTaskHierarchy(remoteTaskHierarchyKey.taskHierarchyId)

    fun getSchedule(scheduleId: ScheduleId.Remote) = remoteProjects.getValue(scheduleId.projectId).getRemoteTaskForce(scheduleId.taskId)
            .schedules
            .single { it.scheduleId == scheduleId }
}
