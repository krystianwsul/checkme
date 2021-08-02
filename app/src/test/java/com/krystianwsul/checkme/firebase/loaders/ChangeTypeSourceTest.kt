package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.checkRemote
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidRootTasksManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.roottask.*
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.SingleParamObservableSource
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.projects.SharedProjectJson
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.*

class ChangeTypeSourceTest {

    companion object {

        private const val privateProjectId = "key"
        private val privateProjectKey = ProjectKey.Private(privateProjectId)

        private val taskKey1 = TaskKey.Root("taskId1")
        private val taskKey2 = TaskKey.Root("taskId2")
        private val taskKey3 = TaskKey.Root("taskId3")

        private val sharedProjectKey = ProjectKey.Shared("sharedProjectId")

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            DomainThreadChecker.instance = mockk(relaxed = true)

            ProjectRootTaskIdTracker.instance = ProjectRootTaskIdTracker()

            mockBase64()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            ProjectRootTaskIdTracker.instance = null
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
        val rootTaskKeySource = RootTaskKeySource()

        rootTasksLoaderProvider = TestRootTasksLoaderProvider()

        val databaseWrapper = mockk<DatabaseWrapper> {
            var taskRecordId = 0
            every { newRootTaskRecordId() } answers { "rootTaskRecordId" + taskRecordId++ }

            var scheduleRecordId = 0
            every { newRootTaskScheduleRecordId(any()) } answers { "scheduleRecordId" + scheduleRecordId++ }
        }

        val rootTasksManager = AndroidRootTasksManager(databaseWrapper)

        val loadDependencyTrackerManager = LoadDependencyTrackerManager()

        rootTasksLoader = RootTasksLoader(
            rootTaskKeySource,
            rootTasksLoaderProvider,
            domainDisposable,
            rootTasksManager,
        )

        val userKeyStore = mockk<UserKeyStore> {
            every { onTasksRemoved(any()) } returns Unit
        }

        val rootTaskToRootTaskCoordinator = RootTaskDependencyCoordinator.Impl(
            rootTaskKeySource,
            userCustomTimeProviderSource,
        )

        val modelRootTaskDependencyStateContainer = RootTaskDependencyStateContainer.Impl()

        val existingInstanceChangeManager = RootModelChangeManager()

        rootTasksFactory = RootTasksFactory(
            rootTasksLoader,
            userKeyStore,
            rootTaskToRootTaskCoordinator,
            domainDisposable,
            rootTaskKeySource,
            loadDependencyTrackerManager,
            modelRootTaskDependencyStateContainer,
            existingInstanceChangeManager,
        ) { projectsFactory }

        val privateProjectManager = AndroidPrivateProjectManager(
            DomainFactoryRule.deviceDbInfo.userInfo,
            databaseWrapper,
        )

        val privateProjectLoader = ProjectLoader.Impl(
            privateProjectSnapshotObservable,
            domainDisposable,
            privateProjectManager,
            null,
            userCustomTimeProviderSource,
            rootTaskKeySource,
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
            rootTaskKeySource,
        )

        val shownFactory = mockk<Instance.ShownFactory>()

        val projectsFactorySingle = Single.zip(
            privateProjectLoader.initialProjectEvent.map {
                check(it.changeType == ChangeType.REMOTE)

                it.data
            },
            sharedProjectsLoader.initialProjectsEvent,
        ) { initialPrivateProjectEvent, initialSharedProjectsEvent ->
            ProjectsFactory(
                privateProjectLoader,
                initialPrivateProjectEvent,
                sharedProjectsLoader,
                initialSharedProjectsEvent,
                ExactTimeStamp.Local.now,
                shownFactory,
                domainDisposable,
                rootTasksFactory,
                existingInstanceChangeManager,
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

        projectEmissionChecker =
            EmissionChecker("projectsFactory", domainDisposable, projectsFactorySingle.flatMapObservable { it.changeTypes })
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

        projectEmissionChecker.checkRemote {
            acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
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

        projectEmissionChecker.checkRemote {
            acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }

        projectEmissionChecker.checkRemote {
            acceptPrivateProject(
                PrivateProjectJson(name = "nameChanged", rootTaskIds = mutableMapOf(taskKey1.taskId to true))
            )
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
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

        projectEmissionChecker.checkRemote {
            acceptPrivateProject(
                PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true, taskKey2.taskId to true))
            )
        }

        projectEmissionChecker.checkRemote {
            acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
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

        projectEmissionChecker.checkRemote {
            acceptPrivateProject(
                PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true, taskKey2.taskId to true))
            )
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
                    ),
                ),
            )
        }

        projectEmissionChecker.checkRemote {
            acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }
    }

    @Test
    fun testSingleProjectChildTask() {
        setup()

        acceptPrivateProject(PrivateProjectJson())
        checkEmpty()

        projectEmissionChecker.checkRemote {
            acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
                    ),
                    rootTaskIds = mutableMapOf(taskKey2.taskId to true),
                ),
            )
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                    startTimeOffset = 0.0,
                    taskHierarchies = mapOf(
                        "taskHierarchyId" to NestedTaskHierarchyJson(
                            startTimeOffset = 0.0,
                            parentTaskId = taskKey1.taskId,
                        ),
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

        projectEmissionChecker.checkRemote {
            acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
                    ),
                    rootTaskIds = mutableMapOf(taskKey2.taskId to true, taskKey3.taskId to true),
                ),
            )
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                    startTimeOffset = 0.0,
                    taskHierarchies = mapOf(
                        "taskHierarchyId" to NestedTaskHierarchyJson(
                            startTimeOffset = 0.0,
                            parentTaskId = taskKey1.taskId,
                        ),
                    ),
                ),
            )
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
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

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
                    ),
                ),
            )
        }

        checkEmpty()

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    name = "changedName",
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
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
                    "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                        startTimeOffset = 0.0,
                        projectId = privateProjectId,
                    ),
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

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
                    ),
                    rootTaskIds = mutableMapOf(taskKey2.taskId to true)
                ),
            )
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                    startTimeOffset = 0.0,
                    taskHierarchies = mapOf(
                        "taskHierarchyId" to NestedTaskHierarchyJson(
                            startTimeOffset = 0.0,
                            parentTaskId = taskKey1.taskId,
                        )
                    ),
                ),
            )
        }

        checkEmpty()

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun testSingleProjectUpdateChildTask() {
        setup()
        acceptPrivateProject(PrivateProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
                    ),
                    rootTaskIds = mutableMapOf(taskKey2.taskId to true)
                ),
            )
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                    startTimeOffset = 0.0,
                    taskHierarchies = mapOf(
                        "taskHierarchyId" to NestedTaskHierarchyJson(
                            startTimeOffset = 0.0,
                            parentTaskId = taskKey1.taskId,
                        )
                    ),
                ),
            )
        }

        checkEmpty()

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey2,
                RootTaskJson(
                    name = "changedName",
                    startTimeOffset = 0.0,
                    taskHierarchies = mapOf(
                        "taskHierarchyId" to NestedTaskHierarchyJson(
                            startTimeOffset = 0.0,
                            parentTaskId = taskKey1.taskId,
                        )
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
                    "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                        startTimeOffset = 0.0,
                        projectId = privateProjectId,
                    ),
                ),
            ),
        )

        rootTasksLoaderProvider.accept(
            taskKey1,
            RootTaskJson(
                name = "changedName",
                noScheduleOrParent = mapOf(
                    "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                        startTimeOffset = 0.0,
                        projectId = privateProjectId,
                    ),
                ),
            ),
        )

        projectEmissionChecker.checkRemote { timeRelay.accept(mockk()) }
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

        projectEmissionChecker.checkRemote {
            sharedProjectSnapshotRelay.accept(
                Snapshot(
                    sharedProjectKey.key,
                    JsonWrapper(
                        SharedProjectJson(
                            users = mutableMapOf("key" to UserJson()),
                            rootTaskIds = mutableMapOf(taskKey1.taskId to true),
                        )
                    ),
                )
            )
        }

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = sharedProjectKey.key,
                        ),
                    ),
                ),
            )
        }
    }

    private fun createTask(): TaskKey.Root {
        setup()

        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateProjectJson())

        val task = rootTasksFactory.createTask(
            ExactTimeStamp.Local.now,
            null,
            "task",
            null,
            null,
        ).apply {
            createSchedules(
                ExactTimeStamp.Local.now,
                listOf(Pair(ScheduleData.Single(Date.today(), TimePair(HourMinute.now)), Time.Normal(HourMinute.now))),
                setOf(),
                mockk(),
                privateProjectKey,
            )
        }
        val taskKey = task.taskKey

        rootTasksFactory.getRootTask(taskKey)

        rootTasksFactory.updateProjectRecord(privateProjectKey, setOf(taskKey))

        rootTasksFactory.getRootTask(taskKey)

        return taskKey
    }

    @Test
    fun testTaskCreate() {
        createTask()
    }

    @Test
    fun testTaskCreateThenRemoteUpdate() {
        val taskKey = createTask()

        acceptPrivateProject(
            PrivateProjectJson(
                rootTaskIds = mutableMapOf(taskKey1.taskId to true, taskKey.taskId to true)
            )
        )

        taskEmissionChecker.checkRemote {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                        ),
                    ),
                ),
            )
        }

        rootTasksFactory.getRootTask(taskKey)
    }
}