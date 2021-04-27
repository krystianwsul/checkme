package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.ProjectUserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.checkRemote
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.RootTasksManager
import com.krystianwsul.checkme.firebase.roottask.*
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.SingleParamObservableSource
import com.krystianwsul.checkme.utils.getCurrentValue
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class ChangeTypeSourceTest {

    companion object {

        private const val privateProjectId = "privateProjectId"

        private val taskKey1 = TaskKey.Root("taskId1")
        private val taskKey2 = TaskKey.Root("taskId2")

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            DomainThreadChecker.instance = mockk(relaxed = true)
        }
    }

    private class TestRootTasksLoaderProvider : RootTasksLoader.Provider {

        private val singleParamObservableSource = SingleParamObservableSource<TaskKey.Root, Snapshot<RootTaskJson>>()

        override fun getRootTaskObservable(rootTaskKey: TaskKey.Root) =
                singleParamObservableSource.getObservable(rootTaskKey)

        fun accept(taskKey: TaskKey.Root, json: RootTaskJson) =
                singleParamObservableSource.accept(taskKey, Snapshot(taskKey.taskId, json))
    }

    private val domainDisposable = CompositeDisposable()

    private lateinit var privateProjectSnapshotObservable: PublishRelay<Snapshot<PrivateProjectJson>>
    private lateinit var rootTasksLoaderProvider: TestRootTasksLoaderProvider

    private lateinit var rootTasksLoader: RootTasksLoader
    private lateinit var rootTasksFactory: RootTasksFactory

    private lateinit var changeTypeSource: ChangeTypeSource

    private lateinit var emissionChecker: EmissionChecker<ChangeType>

    @Before
    fun before() {
        privateProjectSnapshotObservable = PublishRelay.create()
    }

    @After
    fun after() {
        domainDisposable.clear()
    }

    @Test
    fun testInitial() {
        val rootTaskKeySource = RootTaskKeySource(domainDisposable)

        rootTasksLoaderProvider = TestRootTasksLoaderProvider()

        val databaseWrapper = mockk<DatabaseWrapper>()

        val rootTasksManager = RootTasksManager(databaseWrapper)

        rootTasksLoader = RootTasksLoader(
                rootTaskKeySource.rootTaskKeysObservable,
                rootTasksLoaderProvider,
                domainDisposable,
                rootTasksManager,
        )

        val rootTaskUserCustomTimeProviderSource = mockk<RootTaskUserCustomTimeProviderSource> {
            every { getUserCustomTimeProvider(any()) } returns Single.just(mockk())
        }

        val userKeyStore = mockk<UserKeyStore> {
            every { onTasksRemoved(any()) } returns Unit
        }

        val rootTaskToRootTaskCoordinator = RootTaskToRootTaskCoordinator.Impl(
                rootTaskKeySource,
                rootTasksLoader,
                domainDisposable,
                rootTaskUserCustomTimeProviderSource,
        )

        lateinit var projectsFactorySingle: Single<ProjectsFactory>

        val projectDependencyLoadTrackerManager = ProjectDependencyLoadTrackerManager()

        rootTasksFactory = RootTasksFactory(
                rootTasksLoader,
                rootTaskUserCustomTimeProviderSource,
                userKeyStore,
                rootTaskToRootTaskCoordinator,
                domainDisposable,
                rootTaskKeySource,
                projectDependencyLoadTrackerManager,
        ) { projectsFactorySingle.getCurrentValue() }

        val privateProjectManager = AndroidPrivateProjectManager(
                DomainFactoryRule.deviceDbInfo.userInfo,
                databaseWrapper,
        )

        val projectUserCustomTimeProviderSource = mockk<ProjectUserCustomTimeProviderSource> {
            every { getUserCustomTimeProvider(any()) } returns Single.just(mockk())
        }

        val projectToRootTaskCoordinator = ProjectToRootTaskCoordinator.Impl(
                rootTaskKeySource,
                rootTasksFactory,
                projectDependencyLoadTrackerManager,
        )

        val privateProjectLoader = ProjectLoader.Impl(
                privateProjectSnapshotObservable,
                domainDisposable,
                privateProjectManager,
                null,
                projectUserCustomTimeProviderSource,
                projectToRootTaskCoordinator,
        )

        val sharedProjectsLoader = mockk<SharedProjectsLoader> {
            every { initialProjectsEvent } returns Single.just(SharedProjectsLoader.InitialProjectsEvent(emptyList()))

            every { addProjectEvents } returns Observable.never()

            every { removeProjectEvents } returns Observable.never()
        }

        val localFactory = mockk<LocalFactory>()
        val factoryProvider = mockk<FactoryProvider>()

        projectsFactorySingle = Single.zip(
                privateProjectLoader.initialProjectEvent.map {
                    check(it.changeType == ChangeType.REMOTE)

                    it.data
                },
                sharedProjectsLoader.initialProjectsEvent,
        ) { initialPrivateProjectEvent, initialSharedProjectsEvent ->
            ProjectsFactory(
                    localFactory,
                    privateProjectLoader,
                    initialPrivateProjectEvent,
                    sharedProjectsLoader,
                    initialSharedProjectsEvent,
                    ExactTimeStamp.Local.now,
                    factoryProvider,
                    domainDisposable,
                    rootTasksFactory,
            ) { DomainFactoryRule.deviceDbInfo }
        }.cache()

        changeTypeSource = ChangeTypeSource(
                projectsFactorySingle,
                Single.never(),
                DatabaseRx(domainDisposable, Observable.never()),
                Single.never(),
                rootTasksFactory,
                domainDisposable,
        )

        emissionChecker = EmissionChecker("changeTypes", domainDisposable, changeTypeSource.changeTypes)

        emissionChecker.checkEmpty()
    }

    private fun acceptPrivateProject(privateProjectJson: PrivateProjectJson) =
            privateProjectSnapshotObservable.accept(Snapshot(privateProjectId, privateProjectJson))

    @Test
    fun testSingleProjectEmission() {
        testInitial()
        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateProjectJson())

        emissionChecker.checkRemote {
            acceptPrivateProject(PrivateProjectJson("name"))
        }
    }

    @Test
    fun testSingleProjectSingleTask() {
        testInitial()
        acceptPrivateProject(PrivateProjectJson())

        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        emissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                    taskKey1,
                    RootTaskJson(
                            noScheduleOrParent = mapOf(
                                    "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                            ),
                    ),
            )
        }
    }

    @Test
    fun testSingleProjectRecursiveTask() {
        testInitial()
        acceptPrivateProject(PrivateProjectJson())

        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                        noScheduleOrParent = mapOf(
                                "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                        ),
                        rootTaskIds = mutableMapOf(taskKey2.taskId to true)
                ),
        )

        emissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                    taskKey2,
                    RootTaskJson(
                            taskHierarchies = mapOf(
                                    "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                            ),
                    ),
            )
        }
    }

    @Test
    fun testSingleProjectTaskChange() {
        testInitial()
        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        // initial event ignored for project
        rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                        noScheduleOrParent = mapOf(
                                "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                        ),
                ),
        )

        emissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                    taskKey1,
                    RootTaskJson(
                            name = "changedName",
                            noScheduleOrParent = mapOf(
                                    "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                            ),
                    ),
            )
        }
    }

    @Test
    fun testSingleProjectRemoveTaskFromProject() {
        testInitial()
        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        // initial event ignored for project
        rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                        noScheduleOrParent = mapOf(
                                "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                        ),
                ),
        )

        emissionChecker.checkRemote {
            acceptPrivateProject(PrivateProjectJson())
        }
    }

    @Test
    fun testSingleProjectRemoveRecursiveTask() { // todo task change this test is incomplete
        testInitial()
        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                        noScheduleOrParent = mapOf(
                                "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                        ),
                        rootTaskIds = mutableMapOf(taskKey2.taskId to true)
                ),
        )

        rootTasksLoaderProvider.accept(
                // todo task change I need to add recurrent changes to tracking
                taskKey2,
                RootTaskJson(
                        taskHierarchies = mapOf(
                                "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                        ),
                ),
        )
    }
}