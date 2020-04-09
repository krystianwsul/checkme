package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.firebase.records.RootInstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

class ProjectFactory(
        deviceDbInfo: DeviceDbInfo,
        private val localFactory: FactoryProvider.Local,
        private val privateProjectManager: AndroidPrivateProjectManager,
        private val sharedProjectManager: AndroidSharedProjectManager,
        private val rootInstanceManagers: MutableMap<TaskKey, AndroidRootInstanceManager<*>>,
        now: ExactTimeStamp,
        private val factoryProvider: FactoryProvider
) : Project.Parent {

    var privateProject: PrivateProject
        private set

    val sharedProjects: MutableMap<ProjectKey.Shared, SharedProject>

    private fun getPrivateRootInstanceManagers() = rootInstanceManagers.filter { it.key.projectKey is ProjectKey.Private }
            .mapValues {
                @Suppress("UNCHECKED_CAST")
                it.value as RootInstanceManager<ProjectType.Private>
            }
            .toMap()

    private fun getSharedRootInstanceManagers(projectKey: ProjectKey.Shared) = rootInstanceManagers.filter { it.key.projectKey == projectKey }
            .mapValues {
                @Suppress("UNCHECKED_CAST")
                it.value as RootInstanceManager<ProjectType.Shared>
            }
            .toMap()

    init {
        privateProject = PrivateProject(
                privateProjectManager.privateProjectRecord,
                getPrivateRootInstanceManagers()
        ) {
            AndroidRootInstanceManager(it, listOf(), factoryProvider)
        }.apply { fixNotificationShown(localFactory, now) }

        sharedProjects = sharedProjectManager.sharedProjectRecords
                .values
                .associate { (sharedProjectRecord, _) ->
                    sharedProjectRecord.projectKey to SharedProject(
                            sharedProjectRecord,
                            getSharedRootInstanceManagers(sharedProjectRecord.projectKey)
                    ) {
                        AndroidRootInstanceManager(it, listOf(), factoryProvider)
                    }.apply {
                        fixNotificationShown(localFactory, now)
                        updateDeviceDbInfo(deviceDbInfo)
                    }
                }
                .toMutableMap()
    }

    val projects
        get() = sharedProjects.toMutableMap<ProjectKey<*>, Project<*>>().apply {
            put(privateProject.id, privateProject)
        }

    var isPrivateSaved
        get() = privateProjectManager.isSaved
        set(value) {
            privateProjectManager.isSaved = value
        }

    val isSharedSaved get() = sharedProjectManager.isSaved

    val isSaved get() = isPrivateSaved || isSharedSaved || rootInstanceManagers.any { it.value.isSaved }

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

    fun onSharedProjectEvent(
            deviceDbInfo: DeviceDbInfo,
            sharedProjectEvent: SharedProjectEvent,
            now: ExactTimeStamp
    ): Boolean {
        val projectKey = ProjectKey.Shared(sharedProjectEvent.key)
        val projectPair = sharedProjectManager.sharedProjectRecords[projectKey]

        if (projectPair?.second == true) {
            sharedProjectManager.setSharedProjectRecord(projectKey, Pair(projectPair.first, false))

            return true
        } else {
            when (sharedProjectEvent) {
                is SharedProjectEvent.Add -> {
                    try {
                        val projectRecord = sharedProjectManager.setProjectRecord(sharedProjectEvent.dataSnapshot)

                        rootInstanceManagers += projectRecord.taskRecords
                                .values
                                .map {
                                    it.taskKey to AndroidRootInstanceManager(
                                            it,
                                            sharedProjectEvent.snapshotInfos[it.taskKey]
                                                    ?: listOf(),
                                            factoryProvider
                                    )
                                }

                        sharedProjects[projectRecord.projectKey] = SharedProject(
                                projectRecord,
                                getSharedRootInstanceManagers(projectKey)
                        ) {
                            AndroidRootInstanceManager(it, listOf(), factoryProvider)
                        }.apply {
                            fixNotificationShown(localFactory, now)
                            updateDeviceDbInfo(deviceDbInfo)
                        }
                    } catch (onlyVisibilityPresentException: TaskRecord.OnlyVisibilityPresentException) {
                        // hack for oldestVisible being set on records removed by cloud function
                    }
                }
                is SharedProjectEvent.Change -> {
                    try {
                        val projectRecord = sharedProjectManager.setProjectRecord(sharedProjectEvent.dataSnapshot)

                        sharedProjects[projectRecord.projectKey] = SharedProject(
                                projectRecord,
                                getSharedRootInstanceManagers(projectRecord.projectKey)
                        ) {
                            AndroidRootInstanceManager(it, listOf(), factoryProvider)
                        }.apply {
                            fixNotificationShown(localFactory, now)
                            updateDeviceDbInfo(deviceDbInfo)
                        }
                    } catch (onlyVisibilityPresentException: TaskRecord.OnlyVisibilityPresentException) {
                        // hack for oldestVisible being set on records removed by cloud function
                    }
                }
                is SharedProjectEvent.Remove -> {
                    sharedProjectManager.deleteRemoteSharedProjectRecord(projectKey)
                    sharedProjects.remove(projectKey)
                }
            }

            return false
        }
    }

    fun onInstanceEvent(instanceEvent: InstanceEvent, now: ExactTimeStamp): Boolean {
        val taskKey = instanceEvent.taskKey
        val projectKey = taskKey.projectKey
        val rootInstanceManager = rootInstanceManagers[taskKey]

        checkNotNull(rootInstanceManager)

        val project = projects.getValue(projectKey)

        val newKeys = instanceEvent.snapshotInfos.map {
            val scheduleKey = RootInstanceRecord.dateTimeStringsToSchedulePair(
                    project.projectRecord,
                    it.snapshotKey.dateKey,
                    it.snapshotKey.timeKey
            ).first

            InstanceKey(taskKey, scheduleKey)
        }

        val removedKeys = rootInstanceManager.rootInstanceRecords.keys - newKeys

        removedKeys.forEach { rootInstanceManager.rootInstanceRecords.remove(it) }

        val localChanges = instanceEvent.snapshotInfos
                .map {
                    val instanceKey = InstanceKey(taskKey, it.snapshotKey.getScheduleKey(project.projectRecord))
                    val pair = rootInstanceManager.rootInstanceRecords[instanceKey]

                    if (pair?.second == true) {
                        rootInstanceManager.clearSaved(instanceKey)

                        true
                    } else {
                        rootInstanceManager.newRootInstanceRecord(it)

                        false
                    }
                }
                .all { it }

        val local = localChanges && removedKeys.isEmpty()

        if (!local) {
            when (projectKey) {
                is ProjectKey.Private -> {
                    check(privateProject.id == projectKey)

                    privateProject = PrivateProject(
                            privateProject.projectRecord,
                            getPrivateRootInstanceManagers()
                    ) {
                        AndroidRootInstanceManager(it, listOf(), factoryProvider)
                    }.apply {
                        fixNotificationShown(localFactory, now)
                    }
                }
                is ProjectKey.Shared -> {
                    val sharedProject = sharedProjects.getValue(projectKey)

                    sharedProjects[projectKey] = SharedProject(
                            sharedProject.projectRecord,
                            getSharedRootInstanceManagers(projectKey)
                    ) {
                        AndroidRootInstanceManager(it, listOf(), factoryProvider)
                    }.apply {
                        fixNotificationShown(localFactory, now)
                    }
                }
            }
        }

        return local
    }

    fun onNewPrivate(dataSnapshot: FactoryProvider.Database.Snapshot, now: ExactTimeStamp) {
        try {
            val remotePrivateProjectRecord = privateProjectManager.setProjectRecord(dataSnapshot)

            privateProject = PrivateProject(
                    remotePrivateProjectRecord,
                    getPrivateRootInstanceManagers()
            ) {
                AndroidRootInstanceManager(it, listOf(), factoryProvider)
            }.apply { fixNotificationShown(localFactory, now) }
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
        check(name.isNotEmpty())

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

        val sharedProject = SharedProject(
                sharedProjectRecord,
                getSharedRootInstanceManagers(sharedProjectRecord.projectKey)
        ) { AndroidRootInstanceManager(it, listOf(), factoryProvider) }

        check(!projects.containsKey(sharedProject.id))

        sharedProjects[sharedProject.id] = sharedProject

        return sharedProject
    }

    fun save(domainFactory: DomainFactory): Boolean {
        val privateSaved = privateProjectManager.save(domainFactory)
        val sharedSaved = sharedProjectManager.save(domainFactory)
        val instanceSaved = rootInstanceManagers.map { it.value.save() }.any { it }
        return privateSaved || sharedSaved || instanceSaved
    }

    fun getCustomTime(customTimeKey: CustomTimeKey<*>) = projects.getValue(customTimeKey.projectId).getCustomTime(customTimeKey.customTimeId)

    fun getExistingInstanceIfPresent(instanceKey: InstanceKey): Instance<*>? {
        val taskKey = instanceKey.taskKey

        return getProjectForce(taskKey).getTaskIfPresent(taskKey.taskId)?.getExistingInstanceIfPresent(instanceKey.scheduleKey)
    }

    private fun getProjectForce(taskKey: TaskKey) = getProjectIfPresent(taskKey)!!

    private fun getProjectIfPresent(taskKey: TaskKey) = projects[taskKey.projectKey]

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

    sealed class SharedProjectEvent {

        abstract val key: String

        class Change(val dataSnapshot: FactoryProvider.Database.Snapshot) : SharedProjectEvent() {

            override val key = dataSnapshot.key
        }

        class Add(
                val dataSnapshot: FactoryProvider.Database.Snapshot,
                val snapshotInfos: Map<TaskKey, List<AndroidRootInstanceManager.SnapshotInfo>>
        ) : SharedProjectEvent() {

            override val key = dataSnapshot.key
        }

        class Remove(override val key: String) : SharedProjectEvent()
    }

    class InstanceEvent(
            val taskKey: TaskKey,
            val snapshotInfos: List<AndroidRootInstanceManager.SnapshotInfo>
    )
}
