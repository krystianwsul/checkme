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

    val projectTasks get() = projects.values.flatMap { it.projectTasks }

    fun createScheduleTopLevelTask(
            now: ExactTimeStamp.Local,
            name: String,
            scheduleDatas: List<Pair<ScheduleData, Time>>,
            note: String?,
            projectKey: ProjectKey<*>,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
            ordinal: Double?,
            assignedTo: Set<UserKey>,
    ): ProjectTask {
        return createTaskHelper(
                now,
                name,
                note,
                projectKey,
                imageUuid,
                deviceDbInfo,
                ordinal,
        ).apply {
            createSchedules(now, scheduleDatas, assignedTo, customTimeMigrationHelper, projectKey)
        }
    }

    fun createNoScheduleOrParentTask(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            projectKey: ProjectKey<*>,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo,
            ordinal: Double?,
    ) = createTaskHelper(
            now,
            name,
            note,
            projectKey,
            imageUuid,
            deviceDbInfo,
            ordinal,
    ).apply { setNoScheduleOrParent(now, projectKey) }

    private fun createTaskHelper(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            projectKey: ProjectKey<*>,
            imageUuid: String?,
            deviceDbInfo: DeviceDbInfo,
            ordinal: Double? = null,
    ) = getProjectForce(projectKey).createTask(
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

    fun getProjectTaskForce(taskKey: TaskKey.Project) = projects.getValue(taskKey.projectKey).getProjectTaskForce(taskKey)

    fun getTaskIfPresent(taskKey: TaskKey.Project) = projects[taskKey.projectKey]?.getTaskIfPresent(taskKey)

    fun updateDeviceInfo(deviceDbInfo: DeviceDbInfo) =
            sharedProjects.values.forEach { it.updateDeviceDbInfo(deviceDbInfo) }

    fun updatePhotoUrl(deviceInfo: DeviceInfo, photoUrl: String) =
            sharedProjects.values.forEach { it.updatePhotoUrl(deviceInfo, photoUrl) }

    @Suppress("UNCHECKED_CAST")
    fun <T : ProjectType> getProjectForce(projectId: ProjectKey<T>) =
            projects.getValue(projectId) as Project<T>

    fun getProjectIfPresent(projectId: String) = projects.entries
            .singleOrNull { it.key.key == projectId }
            ?.value

    fun getProjectForce(projectId: String) = getProjectIfPresent(projectId)!!
}
