package com.krystianwsul.checkme.firebase.factories

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
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

class ProjectsFactory(
    private val privateProjectLoader: ProjectLoader<ProjectType.Private, PrivateProjectJson>,
    privateInitialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private, PrivateProjectJson>,
    private val sharedProjectsLoader: SharedProjectsLoader,
    sharedInitialProjectsEvent: SharedProjectsLoader.InitialProjectsEvent,
    now: ExactTimeStamp.Local,
    private val shownFactory: Instance.ShownFactory,
    private val domainDisposable: CompositeDisposable,
    rootTaskProvider: Project.RootTaskProvider,
    rootModelChangeManager: RootModelChangeManager,
    deviceDbInfo: () -> DeviceDbInfo,
) {

    private val privateProjectFactory = PrivateProjectFactory(
        privateProjectLoader,
        privateInitialProjectEvent,
        shownFactory,
        domainDisposable,
        rootTaskProvider,
        rootModelChangeManager,
        deviceDbInfo,
    )

    private val sharedProjectFactoriesProperty = MapRelayProperty(
        sharedInitialProjectsEvent.initialProjectDatas
            .associate { (sharedProjectLoader, sharedInitialProjectEvent) ->
                sharedInitialProjectEvent.projectRecord.projectKey to SharedProjectFactory(
                    sharedProjectLoader,
                    sharedInitialProjectEvent,
                    shownFactory,
                    domainDisposable,
                    rootTaskProvider,
                    rootModelChangeManager,
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
        privateProject.fixNotificationShown(shownFactory, now)

        val addProjectChangeTypes = sharedProjectsLoader.addProjectEvents.mapNotNull { (changeType, addProjectEvent) ->
            val projectKey = addProjectEvent.initialProjectEvent
                .projectRecord
                .projectKey

            check(!sharedProjectFactories.containsKey(projectKey))

            val sharedProjectFactory = SharedProjectFactory(
                addProjectEvent.projectLoader,
                addProjectEvent.initialProjectEvent,
                shownFactory,
                domainDisposable,
                rootTaskProvider,
                rootModelChangeManager,
                deviceDbInfo,
            )

            sharedProjectFactories[projectKey]?.project
                ?.clearableInvalidatableManager
                ?.clear()

            rootModelChangeManager.invalidateProjects()

            sharedProjectFactoriesProperty[projectKey] = sharedProjectFactory

            changeType.takeIf { it == ChangeType.REMOTE } // filtering out internal events for adding project
        }

        val removeProjectChangeTypes = sharedProjectsLoader.removeProjectEvents.map {
            it.projectKeys.forEach {
                check(sharedProjectFactories.containsKey(it))

                sharedProjectFactories.getValue(it)
                    .project
                    .clearableInvalidatableManager
                    .clear()

                rootModelChangeManager.invalidateProjects()

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

    val allTasks get() = projects.values.flatMap { it.getAllTasks() }

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
