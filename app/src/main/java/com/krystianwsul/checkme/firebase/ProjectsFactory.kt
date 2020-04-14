package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.firebase.loaders.SharedProjectsLoader
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo

class ProjectsFactory(
        localFactory: FactoryProvider.Local,
        privateProjectLoader: ProjectLoader<ProjectType.Private>,
        privateInitialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private>,
        private val sharedProjectsLoader: SharedProjectsLoader,
        sharedInitialProjectsEvent: SharedProjectsLoader.InitialProjectsEvent,
        now: ExactTimeStamp,
        private val factoryProvider: FactoryProvider,
        private val domainDisposable: CompositeDisposable,
        deviceDbInfo: () -> DeviceDbInfo
) : Project.Parent {

    private val privateProjectFactory = PrivateProjectFactory(
            privateProjectLoader,
            privateInitialProjectEvent,
            factoryProvider,
            domainDisposable
    )

    private var sharedProjectFactories = sharedInitialProjectsEvent.pairs
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

    val privateProject get() = privateProjectFactory.project as PrivateProject

    val sharedProjects get() = sharedProjectFactories.mapValues { it.value.project as SharedProject }

    init {
        privateProject.fixNotificationShown(localFactory, now)

        sharedProjectsLoader.addProjectEvents // todo instances pass these events to domainfactory
                .subscribe {
                    val projectKey = it.initialProjectEvent
                            .projectRecord
                            .projectKey

                    check(!sharedProjectFactories.containsKey(projectKey))

                    sharedProjectFactories[projectKey] = SharedProjectFactory(
                            it.projectLoader,
                            it.initialProjectEvent,
                            factoryProvider,
                            domainDisposable,
                            deviceDbInfo
                    )
                }
                .addTo(domainDisposable)

        sharedProjectsLoader.removeProjectEvents // todo instances pass these events to domainfactory
                .subscribe {
                    it.projectKeys.forEach {
                        check(sharedProjectFactories.containsKey(it))

                        sharedProjectFactories.remove(it)
                    }
                }
                .addTo(domainDisposable)
    }

    val projects
        get() = sharedProjects.toMutableMap<ProjectKey<*>, Project<*>>().apply {
            put(privateProject.id, privateProject)
        }

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

    fun createScheduleRootTask(
            now: ExactTimeStamp,
            name: String,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            note: String?,
            projectId: ProjectKey<*>,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo
    ) = createTaskHelper(now, name, note, projectId, imageUuid, deviceDbInfo).apply {
        createSchedules(deviceDbInfo.key, now, scheduleDatas)
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

        val sharedProjectRecord = sharedProjectsLoader.projectManager.newProjectRecord(JsonWrapper(sharedProjectJson))

        val sharedProject = SharedProject(
                sharedProjectRecord,
                mapOf()
        ) { AndroidRootInstanceManager(it, listOf(), factoryProvider) }

        check(!projects.containsKey(sharedProject.id))

        /*
            todo instances once I have isSaved and local events figured out, find a way to transmit
            this through the user manager and sequence of events in general.  And test it
         */
        //sharedProjects[sharedProject.id] = sharedProject

        return sharedProject
    }

    fun save(domainFactory: DomainFactory): Boolean {
        val privateSaved = privateProjectFactory.save(domainFactory)
        val sharedSaved = sharedProjectFactories.map { it.value.save(domainFactory) }.any { it }
        return privateSaved || sharedSaved
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
        // todo instances same as for createProject

        //val projectId = project.id

        //check(projects.containsKey(projectId))
        //sharedProjects.remove(projectId)
    }

    fun getTaskHierarchy(taskHierarchyKey: TaskHierarchyKey) = projects.getValue(taskHierarchyKey.projectId).getTaskHierarchy(taskHierarchyKey.taskHierarchyId)

    fun getSchedule(scheduleId: ScheduleId) = projects.getValue(scheduleId.projectId)
            .getTaskForce(scheduleId.taskId)
            .schedules
            .single { it.scheduleId == scheduleId }
}
