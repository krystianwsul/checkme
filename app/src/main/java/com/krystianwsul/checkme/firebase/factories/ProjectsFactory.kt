package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.firebase.loaders.SharedProjectsLoader
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.checkme.utils.MapRelayProperty
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

class ProjectsFactory(
        localFactory: FactoryProvider.Local,
        private val privateProjectLoader: ProjectLoader<ProjectType.Private, PrivateProjectJson>,
        privateInitialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private, PrivateProjectJson>,
        private val sharedProjectsLoader: SharedProjectsLoader,
        sharedInitialProjectsEvent: SharedProjectsLoader.InitialProjectsEvent,
        now: ExactTimeStamp.Local,
        private val factoryProvider: FactoryProvider,
        private val domainDisposable: CompositeDisposable,
        deviceDbInfo: () -> DeviceDbInfo,
) {

    private val privateProjectFactory = PrivateProjectFactory(
            privateProjectLoader,
            privateInitialProjectEvent,
            factoryProvider,
            domainDisposable,
            deviceDbInfo
    )

    private val sharedProjectFactoriesProperty = MapRelayProperty(
            sharedInitialProjectsEvent.pairs
                    .associate { (sharedProjectLoader, sharedInitialProjectEvent) ->
                        sharedInitialProjectEvent.projectRecord.projectKey to SharedProjectFactory(
                                sharedProjectLoader,
                                sharedInitialProjectEvent,
                                factoryProvider,
                                domainDisposable,
                                deviceDbInfo
                        )
                    }
                    .toMutableMap()
    )

    private var sharedProjectFactories by sharedProjectFactoriesProperty

    val privateProject get() = privateProjectFactory.project as PrivateProject

    private val factorySharedProjects get() = sharedProjectFactories.mapValues { it.value.project as SharedProject }

    private val addedSharedProjects = mutableMapOf<ProjectKey<ProjectType.Shared>, SharedProject>()

    val sharedProjects: Map<ProjectKey<ProjectType.Shared>, SharedProject>
        get() {
            check(factorySharedProjects.keys.intersect(addedSharedProjects.keys).isEmpty())

            return factorySharedProjects + addedSharedProjects
        }

    val changeTypes: Observable<ChangeType>

    init {
        privateProject.fixNotificationShown(localFactory, now)

        val addProjectChangeTypes =
                sharedProjectsLoader.addProjectEvents.map { (changeType, addProjectEvent) ->
                    val projectKey = addProjectEvent.initialProjectEvent
                            .projectRecord
                            .projectKey

                    check(!sharedProjectFactories.containsKey(projectKey))

                    val sharedProjectFactory = SharedProjectFactory(
                            addProjectEvent.projectLoader,
                            addProjectEvent.initialProjectEvent,
                            factoryProvider,
                            domainDisposable,
                            deviceDbInfo,
                    )

                    sharedProjectFactoriesProperty[projectKey] = sharedProjectFactory

                    if (addedSharedProjects.containsKey(projectKey)) {
                        check(changeType == ChangeType.LOCAL)

                        val oldRecord = addedSharedProjects.getValue(projectKey).projectRecord
                        val newRecord = sharedProjectFactory.project.projectRecord

                        check(oldRecord == newRecord)

                        addedSharedProjects.remove(projectKey)
                    }

                    changeType
                }

        val removeProjectChangeTypes =
                sharedProjectsLoader.removeProjectEvents.map { (changeType, removeProjectEvent) ->
                    check(changeType == ChangeType.REMOTE)

                    removeProjectEvent.projectKeys.forEach {
                        check(!addedSharedProjects.containsKey(it))
                        check(sharedProjectFactories.containsKey(it))

                        sharedProjectFactoriesProperty.remove(it)
                    }

                    changeType
                }

        val sharedProjectFactoryChangeTypes = sharedProjectFactoriesProperty.observable.switchMap {
            it.values
                    .map { it.changeTypes }
                    .merge()
        }

        changeTypes = listOf(
                privateProjectFactory.changeTypes,
                sharedProjectFactoryChangeTypes,
                addProjectChangeTypes,
                removeProjectChangeTypes
        ).merge().publishImmediate(domainDisposable)
    }

    val projects get() = sharedProjects + mapOf(privateProject.projectKey to privateProject)

    val isPrivateSaved get() = privateProjectFactory.isSaved

    val isSharedSaved get() = sharedProjectFactories.values.any { it.isSaved }

    val isSaved get() = isPrivateSaved || isSharedSaved

    val tasks get() = projects.values.flatMap { it.tasks }

    val remoteCustomTimes get() = projects.values.flatMap { it.customTimes }

    val instanceCount
        get() = projects.values
                .flatMap { it.tasks }
                .asSequence()
                .map { it.existingInstances.size }
                .sum()

    val taskKeys
        get() = projects.values
                .flatMap {
                    val projectId = it.projectKey

                    it.taskIds.map { TaskKey(projectId, it) }
                }
                .toSet()

    val taskCount: Int
        get() = projects.values
                .map { it.tasks.size }
                .sum()

    val savedList get() = privateProjectFactory.savedList + sharedProjectFactories.values.flatMap { it.savedList }

    fun createScheduleRootTask(
            now: ExactTimeStamp.Local,
            name: String,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            note: String?,
            projectId: ProjectKey<*>,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo,
            ordinal: Double? = null,
            assignedTo: Set<UserKey> = setOf(),
    ): Task<*> {
        return createTaskHelper(
                now,
                name,
                note,
                projectId,
                imageUuid,
                deviceDbInfo,
                ordinal,
        ).apply { createSchedules(deviceDbInfo.key, now, scheduleDatas, assignedTo) }
    }

    fun createNoScheduleOrParentTask(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            projectKey: ProjectKey<*>,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo,
            ordinal: Double? = null,
    ) = createTaskHelper(
            now,
            name,
            note,
            projectKey,
            imageUuid,
            deviceDbInfo,
            ordinal,
    ).apply { setNoScheduleOrParent(now) }

    private fun createTaskHelper(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            projectId: ProjectKey<*>,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo,
            ordinal: Double? = null,
    ) = getProjectForce(projectId).createTask(
            now,
            imageUuid?.let { TaskJson.Image(imageUuid, deviceDbInfo.uuid) },
            name,
            note,
            ordinal,
    )

    fun createProject(
            name: String,
            now: ExactTimeStamp.Local,
            recordOf: Set<UserKey>,
            rootUser: RootUser,
            userInfo: UserInfo,
            friendsFactory: FriendsFactory,
    ): SharedProject {
        check(name.isNotEmpty())

        val friendIds = recordOf.toMutableSet()
        friendIds.remove(userInfo.key)

        val userJsons = friendsFactory.getUserJsons(friendIds).toMutableMap()
        userJsons[userInfo.key] = rootUser.userJson

        val sharedProjectJson = SharedProjectJson(
                name,
                now.long,
                now.offset,
                users = userJsons.mapKeys { it.key.key }.toMutableMap(),
        )

        val sharedProjectRecord =
                sharedProjectsLoader.projectManager.newProjectRecord(JsonWrapper(sharedProjectJson))

        val sharedProject = SharedProject(
                sharedProjectRecord,
                mapOf()
        ) { AndroidRootInstanceManager(it, null, factoryProvider) }

        check(!projects.containsKey(sharedProject.projectKey))

        addedSharedProjects[sharedProject.projectKey] = sharedProject

        return sharedProject
    }

    fun save(values: MutableMap<String, Any?>) {
        privateProjectLoader.projectManager.save(values)
        privateProjectFactory.saveInstances(values)

        sharedProjectsLoader.projectManager.save(values)
        sharedProjectFactories.forEach { it.value.saveInstances(values) }
    }

    fun getCustomTime(customTimeKey: CustomTimeKey<*>) =
            projects.getValue(customTimeKey.projectId).getCustomTime(customTimeKey.customTimeId)

    private fun getProjectForce(taskKey: TaskKey) = getProjectIfPresent(taskKey)!!

    private fun getProjectIfPresent(taskKey: TaskKey) = projects[taskKey.projectKey]

    fun getTaskForce(taskKey: TaskKey) = getProjectForce(taskKey).getTaskForce(taskKey.taskId)

    fun getTaskIfPresent(taskKey: TaskKey) =
            getProjectIfPresent(taskKey)?.getTaskIfPresent(taskKey.taskId)

    fun updateDeviceInfo(deviceDbInfo: DeviceDbInfo) = sharedProjects.values.forEach {
        it.updateDeviceDbInfo(deviceDbInfo)
    }

    fun updatePhotoUrl(deviceInfo: DeviceInfo, photoUrl: String) = sharedProjects.values.forEach {
        it.updatePhotoUrl(deviceInfo, photoUrl)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ProjectType> getProjectForce(projectId: ProjectKey<T>) =
            projects.getValue(projectId) as Project<T>

    fun getProjectIfPresent(projectId: String) = projects.entries
            .singleOrNull { it.key.key == projectId }
            ?.value

    fun getTaskHierarchy(taskHierarchyKey: TaskHierarchyKey) =
            projects.getValue(taskHierarchyKey.projectId)
                    .getTaskHierarchy(taskHierarchyKey.taskHierarchyId)

    fun getSchedule(scheduleId: ScheduleId) = projects.getValue(scheduleId.projectId)
            .getTaskForce(scheduleId.taskId)
            .schedules
            .single { it.scheduleId == scheduleId }
}
