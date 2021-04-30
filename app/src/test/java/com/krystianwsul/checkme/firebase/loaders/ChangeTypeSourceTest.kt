package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.jakewharton.rxrelay3.Relay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.checkRemote
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.managers.RootTasksManager
import com.krystianwsul.checkme.firebase.roottask.*
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.SingleParamObservableSource
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.projects.SharedProjectJson
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectKey
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

        private val sharedProjectKey = ProjectKey.Shared("sharedProjectId")

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            DomainThreadChecker.instance = mockk(relaxed = true)

            mockBase64()
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

    private lateinit var sharedProjectKeysRelay: PublishRelay<Set<ProjectKey.Shared>>
    private lateinit var sharedProjectSnapshotRelay: PublishRelay<Snapshot<JsonWrapper>>

    private lateinit var rootTasksFactory: RootTasksFactory

    private lateinit var changeTypeSource: ChangeTypeSource

    private lateinit var projectEmissionChecker: EmissionChecker<ChangeType>
    private lateinit var taskEmissionChecker: EmissionChecker<ChangeType>

    private class TestUserCustomTimeProviderSource : UserCustomTimeProviderSource {

        private val relayMap = mutableMapOf<TaskKey.Root, Relay<JsonTime.UserCustomTimeProvider>>()
        private val providerMap = mutableMapOf<TaskKey.Root, JsonTime.UserCustomTimeProvider>()

        private val trigger = PublishRelay.create<Unit>()!!

        override fun getUserCustomTimeProvider(projectRecord: ProjectRecord<*>) =
                Single.just<JsonTime.UserCustomTimeProvider>(mockk())!!

        private fun getRelay(taskKey: TaskKey.Root) = relayMap.getOrPut(taskKey) { BehaviorRelay.create() }

        override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord) =
                getRelay(rootTaskRecord.taskKey).firstOrError()!!

        override fun hasCustomTimes(rootTaskRecord: RootTaskRecord) = providerMap.containsKey(rootTaskRecord.taskKey)

        override fun getTimeChangeObservable() = trigger

        fun accept(taskKey: TaskKey.Root, provider: JsonTime.UserCustomTimeProvider) {
            check((relayMap[taskKey] as? BehaviorRelay<*>)?.hasValue() != true)

            getRelay(taskKey).accept(provider)

            providerMap[taskKey] = provider
            trigger.accept(Unit)
        }
    }

    @Before
    fun before() {
        privateProjectSnapshotObservable = PublishRelay.create()
    }

    @After
    fun after() {
        domainDisposable.clear()
    }

    private fun immediateUserCustomTimeProviderSource() = object : UserCustomTimeProviderSource {

        override fun getTimeChangeObservable() = Observable.just(Unit)

        override fun getUserCustomTimeProvider(projectRecord: ProjectRecord<*>) =
                Single.just(mockk<JsonTime.UserCustomTimeProvider>())

        override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord) =
                Single.just(mockk<JsonTime.UserCustomTimeProvider>())

        override fun hasCustomTimes(rootTaskRecord: RootTaskRecord) = true
    }

    private fun setup(
            userCustomTimeProviderSource: UserCustomTimeProviderSource = immediateUserCustomTimeProviderSource(),
    ) {
        val rootTaskKeySource = RootTaskKeySource(domainDisposable)

        rootTasksLoaderProvider = TestRootTasksLoaderProvider()

        val databaseWrapper = mockk<DatabaseWrapper> {
            var taskRecordId = 0
            every { newRootTaskRecordId() } answers { "rootTaskRecordId" + taskRecordId++ }
        }

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

        val taskRecordLoader = TaskRecordsLoadedTracker.Impl(rootTasksLoader, domainDisposable)

        val rootTaskToRootTaskCoordinator = RootTaskDependencyCoordinator.Impl(
                rootTaskKeySource,
                rootTasksLoader,
                userCustomTimeProviderSource,
                taskRecordLoader,
        )

        rootTasksFactory = RootTasksFactory(
                rootTasksLoader,
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
                userCustomTimeProviderSource,
                projectToRootTaskCoordinator,
                loadDependencyTrackerManager,
        )

        val sharedProjectManager = AndroidSharedProjectManager(databaseWrapper)

        sharedProjectSnapshotRelay = PublishRelay.create()

        val sharedProjectsProvider = mockk<SharedProjectsProvider> {
            every { getSharedProjectObservable(any()) } returns sharedProjectSnapshotRelay
        }

        sharedProjectKeysRelay = PublishRelay.create()

        val sharedProjectsLoader = SharedProjectsLoader.Impl(
                sharedProjectKeysRelay,
                sharedProjectManager,
                domainDisposable,
                sharedProjectsProvider,
                userCustomTimeProviderSource,
                userKeyStore,
                projectToRootTaskCoordinator,
                rootTaskKeySource,
                loadDependencyTrackerManager,
        )

        val localFactory = mockk<LocalFactory>()

        val factoryProvider = mockk<FactoryProvider> {
            every { shownFactory } returns mockk()
        }

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

        acceptSharedProjectKeys(setOf())
    }

    private fun acceptSharedProjectKeys(sharedProjectKeys: Set<ProjectKey.Shared>) =
            sharedProjectKeysRelay.accept(sharedProjectKeys)

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
        setup()

        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        projectEmissionChecker.checkRemote { acceptPrivateProject(PrivateProjectJson("name")) }
    }

    @Test
    fun testSingleProjectSingleTask() {
        setup()

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
        setup()

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
        setup()

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
        setup()

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
        setup()

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
    fun testSingleProjectTwoChildTasksButOneRemovedSwitchOrder() {
        setup()

        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                        noScheduleOrParent = mapOf(
                                "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                        ),
                        rootTaskIds = mutableMapOf(taskKey2.taskId to true, taskKey3.taskId to true),
                ),
        )

        rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                        startTimeOffset = 0.0,
                        taskHierarchies = mapOf(
                                "taskHierarchyId" to NestedTaskHierarchyJson(parentTaskId = taskKey1.taskId),
                        ),
                ),
        )

        projectEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                    taskKey1,
                    RootTaskJson(
                            noScheduleOrParent = mapOf(
                                    "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = privateProjectId),
                            ),
                            rootTaskIds = mutableMapOf(taskKey2.taskId to true),
                    ),
            )
        }
    }

    @Test
    fun testSingleProjectTaskChange() {
        setup()

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
        setup()
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
        setup()
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
        setup()
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
                object : UserCustomTimeProviderSource {

                    override fun getUserCustomTimeProvider(projectRecord: ProjectRecord<*>) = timeRelay.firstOrError()

                    override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord) =
                            Single.just<JsonTime.UserCustomTimeProvider>(mockk())

                    override fun hasCustomTimes(rootTaskRecord: RootTaskRecord) = true

                    override fun getTimeChangeObservable() = Observable.just(Unit)
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
        val timeSource = TestUserCustomTimeProviderSource()
        setup(userCustomTimeProviderSource = timeSource)

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
    fun testTaskTimesSingleProjectChildTaskUpdateParentBeforeTime() {
        val timeSource = TestUserCustomTimeProviderSource()
        setup(userCustomTimeProviderSource = timeSource)

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
        val timeSource = TestUserCustomTimeProviderSource()
        setup(userCustomTimeProviderSource = timeSource)

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

        projectEmissionChecker.checkRemote { timeSource.accept(taskKey2, mockk()) }
    }

    @Test
    fun testTaskTimesSingleProjectChildTaskTimesDelayed() {
        val timeSource = TestUserCustomTimeProviderSource()
        setup(userCustomTimeProviderSource = timeSource)

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

        timeSource.accept(taskKey1, mockk())
        projectEmissionChecker.checkRemote { timeSource.accept(taskKey2, mockk()) }
    }

    @Test
    fun testTaskTimesSingleProjectChildTaskTimesDelayedSwapped() {
        val timeSource = TestUserCustomTimeProviderSource()
        setup(userCustomTimeProviderSource = timeSource)

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

        timeSource.accept(taskKey2, mockk())
        projectEmissionChecker.checkRemote { timeSource.accept(taskKey1, mockk()) }
    }

    @Test
    fun testSingleSharedProjectEmission() {
        setup()

        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        sharedProjectKeysRelay.accept(setOf(sharedProjectKey))

        projectEmissionChecker.checkRemote {
            sharedProjectSnapshotRelay.accept(
                    Snapshot(
                            sharedProjectKey.key,
                            JsonWrapper(SharedProjectJson(users = mutableMapOf("key" to UserJson()))),
                    ),
            )
        }
    }

    @Test
    fun testSingleSharedProjectSingleTask() {
        setup()

        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        sharedProjectKeysRelay.accept(setOf(sharedProjectKey))

        sharedProjectSnapshotRelay.accept(Snapshot(
                sharedProjectKey.key,
                JsonWrapper(SharedProjectJson(
                        users = mutableMapOf("key" to UserJson()),
                        rootTaskIds = mutableMapOf(taskKey1.taskId to true),
                )),
        ))

        projectEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                    taskKey1,
                    RootTaskJson(
                            noScheduleOrParent = mapOf(
                                    "noScheduleOrParentId" to NoScheduleOrParentJson(projectId = sharedProjectKey.key),
                            ),
                    ),
            )
        }
    }

    @Test
    fun testTaskCreate() {
        setup()

        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateProjectJson())

        val task = rootTasksFactory.createTask(
                ExactTimeStamp.Local.now,
                null,
                "task",
                null,
                null,
        )

        rootTasksFactory.getRootTask(task.taskRecord.taskKey)
    }

    @Test
    fun testTaskCreateThenRemoteUpdate() {
        testTaskCreate()

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
}