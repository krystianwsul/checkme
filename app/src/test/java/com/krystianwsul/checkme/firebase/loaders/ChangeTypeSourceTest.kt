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
import com.krystianwsul.checkme.utils.SingleParamSingleSource
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.JsonTime
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
        private val taskKey3 = TaskKey.Root("taskId3")

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
    private lateinit var projectsFactory: ProjectsFactory
    private lateinit var rootTasksFactory: RootTasksFactory

    private lateinit var changeTypeSource: ChangeTypeSource

    private lateinit var projectEmissionChecker: EmissionChecker<ChangeType>
    private lateinit var taskEmissionChecker: EmissionChecker<ChangeType>

    private class TestRootTaskUserCustomTimeProviderSource : RootTaskUserCustomTimeProviderSource {

        val source = SingleParamSingleSource<TaskKey.Root, JsonTime.UserCustomTimeProvider>(true)

        override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord) =
                source.getSingle(rootTaskRecord.taskKey)
    }

    @Before
    fun before() {
        privateProjectSnapshotObservable = PublishRelay.create()
    }

    @After
    fun after() {
        domainDisposable.clear()
    }

    private fun immediateProjectUserCustomTimeProviderSource() = mockk<ProjectUserCustomTimeProviderSource> {
        every { getUserCustomTimeProvider(any()) } returns Single.just(mockk())
    }

    private fun immediateRootTaskUserCustomTimeProviderSource() = mockk<RootTaskUserCustomTimeProviderSource> {
        every { getUserCustomTimeProvider(any()) } returns Single.just(mockk())
    }

    private fun setup(
            projectUserCustomTimeProviderSource: ProjectUserCustomTimeProviderSource =
                    immediateProjectUserCustomTimeProviderSource(),
            rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource =
                    immediateRootTaskUserCustomTimeProviderSource(),
    ) {
        val rootTaskKeySource = RootTaskKeySource(domainDisposable)

        rootTasksLoaderProvider = TestRootTasksLoaderProvider()

        val databaseWrapper = mockk<DatabaseWrapper>()

        val rootTasksManager = RootTasksManager(databaseWrapper)

        val loadDependencyTrackerManager = LoadDependencyTrackerManager()

        rootTasksLoader = RootTasksLoader(
                rootTaskKeySource.rootTaskKeysObservable,
                rootTasksLoaderProvider,
                domainDisposable,
                rootTasksManager,
                loadDependencyTrackerManager,
        )

        val userKeyStore = mockk<UserKeyStore> {
            every { onTasksRemoved(any()) } returns Unit
        }

        val rootTaskToRootTaskCoordinator = RootTaskToRootTaskCoordinator.Impl(
                rootTaskKeySource,
                rootTasksLoader,
                domainDisposable,
                rootTaskUserCustomTimeProviderSource,
        )

        rootTasksFactory = RootTasksFactory(
                rootTasksLoader,
                rootTaskUserCustomTimeProviderSource,
                userKeyStore,
                rootTaskToRootTaskCoordinator,
                domainDisposable,
                rootTaskKeySource,
                loadDependencyTrackerManager,
        ) { projectsFactory }

        val privateProjectManager = AndroidPrivateProjectManager(
                DomainFactoryRule.deviceDbInfo.userInfo,
                databaseWrapper,
        )

        val projectToRootTaskCoordinator = ProjectToRootTaskCoordinator.Impl(
                rootTaskKeySource,
                rootTasksFactory,
        )

        val privateProjectLoader = ProjectLoader.Impl(
                privateProjectSnapshotObservable,
                domainDisposable,
                privateProjectManager,
                null,
                projectUserCustomTimeProviderSource,
                projectToRootTaskCoordinator,
                loadDependencyTrackerManager,
        )

        val sharedProjectsLoader = mockk<SharedProjectsLoader> {
            every { initialProjectsEvent } returns Single.just(SharedProjectsLoader.InitialProjectsEvent(emptyList()))

            every { addProjectEvents } returns Observable.never()

            every { removeProjectEvents } returns Observable.never()
        }

        val localFactory = mockk<LocalFactory>()
        val factoryProvider = mockk<FactoryProvider>()

        val projectsFactorySingle = Single.zip(
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
            ) { DomainFactoryRule.deviceDbInfo }.also { projectsFactory = it }
        }.cache()

        changeTypeSource = ChangeTypeSource(
                projectsFactorySingle,
                Single.never(),
                DatabaseRx(domainDisposable, Observable.never()),
                Single.never(),
                rootTasksFactory,
                domainDisposable,
        )

        projectEmissionChecker = EmissionChecker("projectsFactory", domainDisposable, projectsFactorySingle.flatMapObservable { it.changeTypes })
        taskEmissionChecker = EmissionChecker("rootTasksFactory", domainDisposable, rootTasksFactory.changeTypes)
    }

    @Test
    fun testInitial() {
        setup()
        checkEmpty()
    }

    private fun acceptPrivateProject(privateProjectJson: PrivateProjectJson) =
            privateProjectSnapshotObservable.accept(Snapshot(privateProjectId, privateProjectJson))

    private fun checkEmpty() {
        projectEmissionChecker.checkEmpty()
        taskEmissionChecker.checkEmpty()
    }

    @Test
    fun testSingleProjectEmission() {
        testInitial()

        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        projectEmissionChecker.checkRemote { acceptPrivateProject(PrivateProjectJson("name")) }
    }

    @Test
    fun testSingleProjectSingleTask() {
        testInitial()

        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        projectEmissionChecker.checkRemote {
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
    fun testSingleProjectSingleTaskChangeProjectBeforeTask() {
        testInitial()

        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        acceptPrivateProject(
                PrivateProjectJson(name = "nameChanged", rootTaskIds = mutableMapOf(taskKey1.taskId to true))
        )

        projectEmissionChecker.checkRemote {
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
    fun testSingleProjectSingleTaskChangeProjectBeforeTaskByStrippingOutSecondTask() {
        testInitial()

        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        acceptPrivateProject(
                PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true, taskKey2.taskId to true))
        )

        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        projectEmissionChecker.checkRemote {
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
    fun testSingleProjectSingleTaskChangeProjectBeforeTaskByStrippingOutSecondTaskDifferentOrder() {
        testInitial()

        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        acceptPrivateProject(
                PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true, taskKey2.taskId to true))
        )
        rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                        noScheduleOrParent = mapOf(
                                "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                        ),
                ),
        )

        projectEmissionChecker.checkRemote {
            acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }
    }

    @Test
    fun testSingleProjectChildTask() {
        testInitial()

        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                        noScheduleOrParent = mapOf(
                                "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                        ),
                        rootTaskIds = mutableMapOf(taskKey2.taskId to true),
                ),
        )

        projectEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                    taskKey2,
                    RootTaskJson(
                            startTimeOffset = 0.0,
                            taskHierarchies = mapOf(
                                    "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId),
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

        checkEmpty()

        taskEmissionChecker.checkRemote {
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
        checkEmpty()

        projectEmissionChecker.checkRemote { acceptPrivateProject(PrivateProjectJson()) }
    }

    @Test
    fun testSingleProjectRemoveChildTask() {
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
                taskKey2,
                RootTaskJson(
                        startTimeOffset = 0.0,
                        taskHierarchies = mapOf(
                                "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                        ),
                ),
        )
        checkEmpty()

        taskEmissionChecker.checkRemote {
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
    fun testSingleProjectUpdateChildTask() {
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
                taskKey2,
                RootTaskJson(
                        startTimeOffset = 0.0,
                        taskHierarchies = mapOf(
                                "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                        ),
                ),
        )
        checkEmpty()

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                    taskKey2,
                    RootTaskJson(
                            name = "changedName",
                            startTimeOffset = 0.0,
                            taskHierarchies = mapOf(
                                    "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                            ),
                    ),
            )
        }
    }

    @Test
    fun testSingleProjectSingleTaskWithTaskChangeBeforeTimes() {
        val timeRelay = PublishRelay.create<JsonTime.UserCustomTimeProvider>()

        setup(
                object : ProjectUserCustomTimeProviderSource {

                    override fun getUserCustomTimeProvider(projectRecord: ProjectRecord<*>) = timeRelay.firstOrError()
                }
        )

        acceptPrivateProject(PrivateProjectJson())
        timeRelay.accept(mockk())
        checkEmpty()

        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                        noScheduleOrParent = mapOf(
                                "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                        ),
                ),
        )

        rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                        name = "changedName",
                        noScheduleOrParent = mapOf(
                                "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                        ),
                ),
        )

        projectEmissionChecker.checkRemote { timeRelay.accept(mockk()) }
    }

    @Test
    fun testTaskTimesSingleProjectChildTaskImmediate() {
        val timeSource = TestRootTaskUserCustomTimeProviderSource()
        setup(rootTaskUserCustomTimeProviderSource = timeSource)

        // to get the initial event out of the way
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
        timeSource.source.accept(taskKey1, mockk())

        rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                        startTimeOffset = 0.0,
                        taskHierarchies = mapOf(
                                "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                        ),
                ),
        )

        projectEmissionChecker.checkRemote { timeSource.source.accept(taskKey2, mockk()) }
    }

    @Test
    fun testTaskTimesSingleProjectChildTaskUpdateParentBeforeTime() {
        val timeSource = SingleParamSingleSource<TaskKey.Root, JsonTime.UserCustomTimeProvider>(true)

        setup(
                rootTaskUserCustomTimeProviderSource = object : RootTaskUserCustomTimeProviderSource {

                    override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord) =
                            timeSource.getSingle(rootTaskRecord.taskKey)
                }
        )

        // to get the initial event out of the way
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

        rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                        "task1Changed",
                        noScheduleOrParent = mapOf(
                                "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                        ),
                        rootTaskIds = mutableMapOf(taskKey2.taskId to true)
                ),
        )

        timeSource.accept(taskKey1, mockk())

        rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                        startTimeOffset = 0.0,
                        taskHierarchies = mapOf(
                                "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                        ),
                ),
        )

        projectEmissionChecker.checkRemote { timeSource.accept(taskKey2, mockk()) }
    }

    @Test
    fun testTaskTimesSingleProjectChildTaskUpdateChildBeforeTime() {
        val timeSource = TestRootTaskUserCustomTimeProviderSource()
        setup(rootTaskUserCustomTimeProviderSource = timeSource)

        // to get the initial event out of the way
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
        timeSource.source.accept(taskKey1, mockk())

        rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                        startTimeOffset = 0.0,
                        taskHierarchies = mapOf(
                                "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                        ),
                ),
        )

        rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                        name = "changedName",
                        startTimeOffset = 0.0,
                        taskHierarchies = mapOf(
                                "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                        ),
                ),
        )

        projectEmissionChecker.checkRemote { timeSource.source.accept(taskKey2, mockk()) }
    }

    @Test
    fun testTaskTimesSingleProjectChildTaskTimesDelayed() {
        val timeSource = TestRootTaskUserCustomTimeProviderSource()
        setup(rootTaskUserCustomTimeProviderSource = timeSource)

        // to get the initial event out of the way
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

        rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                        startTimeOffset = 0.0,
                        taskHierarchies = mapOf(
                                "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                        ),
                ),
        )

        timeSource.source.accept(taskKey1, mockk())
        projectEmissionChecker.checkRemote { timeSource.source.accept(taskKey2, mockk()) }
    }

    @Test
    fun testTaskTimesSingleProjectChildTaskTimesDelayedSwapped() {
        val timeSource = TestRootTaskUserCustomTimeProviderSource()
        setup(rootTaskUserCustomTimeProviderSource = timeSource)

        // to get the initial event out of the way
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

        rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                        startTimeOffset = 0.0,
                        taskHierarchies = mapOf(
                                "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId)
                        ),
                ),
        )

        timeSource.source.accept(taskKey2, mockk())
        projectEmissionChecker.checkRemote { timeSource.source.accept(taskKey1, mockk()) }
    }
}