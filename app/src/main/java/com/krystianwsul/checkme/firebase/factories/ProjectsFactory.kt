package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.firebase.loaders.SharedProjectsLoader
import com.krystianwsul.checkme.utils.MapRelayProperty
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.projects.SharedProjectJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
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
        rootTaskProvider: Project.RootTaskProvider,
        deviceDbInfo: () -> DeviceDbInfo,
) {

    private val privateProjectFactory = PrivateProjectFactory(
            privateProjectLoader,
            privateInitialProjectEvent,
            factoryProvider,
            domainDisposable,
            rootTaskProvider,
            deviceDbInfo,
    )

    private val sharedProjectFactoriesProperty = MapRelayProperty(
            sharedInitialProjectsEvent.initialProjectDatas
                    .associate { (sharedProjectLoader, sharedInitialProjectEvent) ->
                        sharedInitialProjectEvent.projectRecord.projectKey to SharedProjectFactory(
                                sharedProjectLoader,
                                sharedInitialProjectEvent,
                                factoryProvider,
                                domainDisposable,
                                rootTaskProvider,
                                deviceDbInfo,
                        )
                    }
                    .toMutableMap()
    )

    private var sharedProjectFactories by sharedProjectFactoriesProperty

    val privateProject get() = privateProjectFactory.project as PrivateProject

    val sharedProjects get() = sharedProjectFactories.mapValues { it.value.project as SharedProject }

    val changeTypes: Observable<ChangeType>

    init {
        privateProject.fixNotificationShown(localFactory, now)

        val addProjectChangeTypes = sharedProjectsLoader.addProjectEvents.mapNotNull { (changeType, addProjectEvent) ->
            val projectKey = addProjectEvent.initialProjectEvent
                    .projectRecord
                    .projectKey

            check(!sharedProjectFactories.containsKey(projectKey))

            val sharedProjectFactory = SharedProjectFactory(
                    addProjectEvent.projectLoader,
                    addProjectEvent.initialProjectEvent,
                    factoryProvider,
                    domainDisposable,
                    rootTaskProvider,
                    deviceDbInfo,
            )

            sharedProjectFactoriesProperty[projectKey] = sharedProjectFactory

            changeType.takeIf { it == ChangeType.REMOTE } // filtering out internal events for adding project
        }

        val removeProjectChangeTypes =
                sharedProjectsLoader.removeProjectEvents.map {
                    it.projectKeys.forEach {
                        check(sharedProjectFactories.containsKey(it))

                        sharedProjectFactoriesProperty.remove(it)
                    }

                    ChangeType.REMOTE
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
                removeProjectChangeTypes,
        ).merge().publishImmediate(domainDisposable)
    }

    val projects get() = sharedProjects + mapOf(privateProject.projectKey to privateProject)

    val tasks get() = projects.values.flatMap { it.projectTasks }

    val taskKeys
        get() = projects.values
                .flatMap {
                    val projectId = it.projectKey

                    it.projectTaskIds.map { TaskKey.Project(projectId, it) } // todo task fetch
                }
                .toSet()

    val taskCount: Int // todo task fetch
        get() = projects.values
                .map { it.projectTasks.size } // todo task fetch
                .sum()

    fun createScheduleTopLevelTask(
            now: ExactTimeStamp.Local,
            name: String,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            note: String?,
            projectId: ProjectKey<*>,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
            ordinal: Double? = null,
            assignedTo: Set<UserKey> = setOf(),
    ): ProjectTask {
        return createTaskHelper(
                now,
                name,
                note,
                projectId,
                imageUuid,
                deviceDbInfo,
                ordinal,
        ).apply { createSchedules(deviceDbInfo.key, now, scheduleDatas, assignedTo, customTimeMigrationHelper) }
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

        val sharedProjectRecord = sharedProjectsLoader.addProject(JsonWrapper(sharedProjectJson))

        return sharedProjects.getValue(sharedProjectRecord.projectKey)
    }

    fun save(values: MutableMap<String, Any?>) {
        privateProjectLoader.projectManager.save(values)
        sharedProjectsLoader.projectManager.save(values)
    }

    fun getCustomTime(customTimeKey: CustomTimeKey.Project<*>) =
            projects.getValue(customTimeKey.projectId).getUntypedProjectCustomTime(customTimeKey.customTimeId)

    private fun getProjectForce(taskKey: TaskKey) = getProjectIfPresent(taskKey)!! // todo task fetch

    private fun getProjectIfPresent(taskKey: TaskKey) = projects[(taskKey as TaskKey.Project).projectKey] // todo task fetch

    fun getTaskForce(taskKey: TaskKey) = getProjectForce(taskKey).getTaskForce((taskKey as TaskKey.Project).taskId) // todo task fetch

    fun getTaskIfPresent(taskKey: TaskKey) =
            getProjectIfPresent(taskKey)?.getTaskIfPresent((taskKey as TaskKey.Project).taskId) // todo task fetch

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

    fun getProjectForce(projectId: String) = getProjectIfPresent(projectId)!!

    fun getTaskHierarchy(taskHierarchyKey: TaskHierarchyKey): TaskHierarchy {
        return when (taskHierarchyKey) {
            is TaskHierarchyKey.Project -> projects.getValue(taskHierarchyKey.projectId)
                    .getProjectTaskHierarchy(taskHierarchyKey.taskHierarchyId)
            is TaskHierarchyKey.Nested -> projects.getValue((taskHierarchyKey.childTaskKey as TaskKey.Project).projectKey) // todo task fetch
                    .getTaskForce((taskHierarchyKey.childTaskKey as TaskKey.Project).taskId) // todo task fetch
                    .nestedParentTaskHierarchies.getValue(taskHierarchyKey.taskHierarchyId)
            else -> throw UnsupportedOperationException() // compilation
        }
    }
}
