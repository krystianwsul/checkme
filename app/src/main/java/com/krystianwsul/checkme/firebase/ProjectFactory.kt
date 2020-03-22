package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

class ProjectFactory(
        deviceDbInfo: DeviceDbInfo,
        private val localFactory: LocalFactory,
        private val privateProjectManager: AndroidPrivateProjectManager,
        private val sharedProjectManager: AndroidSharedProjectManager,
        private val rootInstanceManagers: Map<TaskKey, AndroidRootInstanceManager<*>>,
        now: ExactTimeStamp
) : Project.Parent {

    var privateProject = PrivateProject(privateProjectManager.privateProjectRecord).apply { fixNotificationShown(localFactory, now) }
        private set

    val sharedProjects = sharedProjectManager.sharedProjectRecords
            .values
            .associate { (sharedProjectRecord, _) ->
                sharedProjectRecord.projectKey to SharedProject(sharedProjectRecord).apply {
                    fixNotificationShown(localFactory, now)
                    updateDeviceDbInfo(deviceDbInfo)
                }
            }
            .toMutableMap()

    val projects
        get() = sharedProjects.toMutableMap<ProjectKey<*>, Project<*>>().apply {
            put(privateProject.id, privateProject)
        }.toMap()

    var isPrivateSaved
        get() = privateProjectManager.isSaved
        set(value) {
            privateProjectManager.isSaved = value
        }

    val isSharedSaved get() = sharedProjectManager.isSaved

    val eitherSaved get() = isPrivateSaved || isSharedSaved

    val tasks get() = projects.values.flatMap { it.tasks }

    val remoteCustomTimes get() = projects.values.flatMap { it.customTimes }

    val instanceCount
        get() = projects.values
                .flatMap { it.tasks }
                .asSequence()
                .map { it.existingInstances.size }
                .sum()

    val existingInstances
        get() = projects.values.flatMap { it.existingInstances }

    val taskKeys
        get() = projects.values
                .flatMap {
                    val projectId = it.id

                    it.taskIds.map { TaskKey(projectId, it) }
                }
                .toSet()

    val taskCount: Int
        get() = projects.values
                .map { it.tasks.size }
                .sum()

    fun onChildEvent(deviceDbInfo: DeviceDbInfo, databaseEvent: DatabaseEvent, now: ExactTimeStamp): Boolean {
        val projectKey = ProjectKey.Shared(databaseEvent.key)
        val projectPair = sharedProjectManager.sharedProjectRecords[projectKey]

        if (projectPair?.second == true) {
            sharedProjectManager.setSharedProjectRecord(projectKey, Pair(projectPair.first, false))

            return true
        } else {
            when (databaseEvent) {
                is DatabaseEvent.AddChange -> {
                    try {
                        val remoteProjectRecord = sharedProjectManager.setChild(databaseEvent.dataSnapshot)

                        sharedProjects[remoteProjectRecord.projectKey] = SharedProject(remoteProjectRecord).apply {
                            fixNotificationShown(localFactory, now)
                            updateDeviceDbInfo(deviceDbInfo)
                        }
                    } catch (onlyVisibilityPresentException: TaskRecord.OnlyVisibilityPresentException) {
                        // hack for oldestVisible being set on records removed by cloud function
                    }
                }
                is DatabaseEvent.Remove -> {
                    sharedProjectManager.deleteRemoteSharedProjectRecord(projectKey)
                    sharedProjects.remove(projectKey)
                }
            }

            return false
        }
    }

    fun onNewPrivate(dataSnapshot: DataSnapshot, now: ExactTimeStamp) {
        try {
            val remotePrivateProjectRecord = privateProjectManager.newSnapshot(dataSnapshot)

            privateProject = PrivateProject(remotePrivateProjectRecord).apply { fixNotificationShown(localFactory, now) }
        } catch (onlyVisibilityPresentException: TaskRecord.OnlyVisibilityPresentException) {
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
            projectId: ProjectKey<*>,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo
    ) = createTaskHelper(now, name, note, projectId, imageUuid, deviceDbInfo).apply {
        createSchedules(privateProject.id.toUserKey(), now, scheduleDatas)
    }

    fun createTaskHelper(
            now: ExactTimeStamp,
            name: String,
            note: String?,
            projectId: ProjectKey<*>,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo
    ): Task<*> {
        val image = imageUuid?.let { TaskJson.Image(imageUuid, deviceDbInfo.uuid) }
        val taskJson = TaskJson(name, now.long, null, note, image = image)

        return getProjectForce(projectId).newTask(taskJson)
    }

    fun createProject(
            name: String,
            now: ExactTimeStamp,
            recordOf: Set<UserKey>,
            rootUser: RootUser,
            userInfo: UserInfo,
            friendFactory: RemoteFriendFactory
    ): SharedProject {
        check(!TextUtils.isEmpty(name))

        val friendIds = recordOf.toMutableSet()
        friendIds.remove(userInfo.key)

        val userJsons = friendFactory.getUserJsons(friendIds)
        userJsons[userInfo.key] = rootUser.userJson

        val sharedProjectJson = SharedProjectJson(
                name,
                now.long,
                users = userJsons.mapKeys { it.key.key }.toMutableMap()
        )

        val sharedProjectRecord = sharedProjectManager.newProjectRecord(JsonWrapper(sharedProjectJson))

        val sharedProject = SharedProject(sharedProjectRecord)

        check(!projects.containsKey(sharedProject.id))

        sharedProjects[sharedProject.id] = sharedProject

        return sharedProject
    }

    fun save(domainFactory: DomainFactory): Boolean {
        val privateSaved = privateProjectManager.save(domainFactory)
        val sharedSaved = sharedProjectManager.save(domainFactory)
        return privateSaved || sharedSaved
    }

    fun getCustomTime(customTimeKey: CustomTimeKey<*>) = projects.getValue(customTimeKey.projectId).getCustomTime(customTimeKey.customTimeId)

    fun getExistingInstanceIfPresent(instanceKey: InstanceKey): Instance<*>? {
        val taskKey = instanceKey.taskKey

        return getProjectForce(taskKey).getTaskIfPresent(taskKey.taskId)?.getExistingInstanceIfPresent(instanceKey.scheduleKey)
    }

    private fun getProjectForce(taskKey: TaskKey) = getProjectIfPresent(taskKey)!!

    private fun getProjectIfPresent(taskKey: TaskKey) = projects[taskKey.projectId]

    fun getTaskForce(taskKey: TaskKey) = getProjectForce(taskKey).getTaskForce(taskKey.taskId)

    fun getTaskIfPresent(taskKey: TaskKey) = getProjectIfPresent(taskKey)?.getTaskIfPresent(taskKey.taskId)

    fun updateDeviceInfo(deviceDbInfo: DeviceDbInfo) = sharedProjects.values.forEach {
        it.updateDeviceDbInfo(deviceDbInfo)
    }

    fun updatePhotoUrl(deviceInfo: DeviceInfo, photoUrl: String) = sharedProjects.values.forEach {
        it.updatePhotoUrl(deviceInfo, photoUrl)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ProjectType> getProjectForce(projectId: ProjectKey<T>) = projects.getValue(projectId) as Project<T>

    fun getProjectIfPresent(projectId: String) = projects.entries
            .singleOrNull { it.key.key == projectId }
            ?.value

    override fun deleteProject(project: Project<*>) {
        val projectId = project.id

        check(projects.containsKey(projectId))
        sharedProjects.remove(projectId)
    }

    fun getTaskHierarchy(taskHierarchyKey: TaskHierarchyKey) = projects.getValue(taskHierarchyKey.projectId).getTaskHierarchy(taskHierarchyKey.taskHierarchyId)

    fun getSchedule(scheduleId: ScheduleId) = projects.getValue(scheduleId.projectId)
            .getTaskForce(scheduleId.taskId)
            .schedules
            .single { it.scheduleId == scheduleId }
}
