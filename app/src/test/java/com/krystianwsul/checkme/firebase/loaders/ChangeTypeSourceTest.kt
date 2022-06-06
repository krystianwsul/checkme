package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.dependencies.RequestMerger
import com.krystianwsul.checkme.firebase.dependencies.RootTaskKeyStore
import com.krystianwsul.checkme.firebase.dependencies.UserKeyStore
import com.krystianwsul.checkme.firebase.factories.OwnedProjectsFactory
import com.krystianwsul.checkme.firebase.foreignProjects.ForeignProjectCoordinator
import com.krystianwsul.checkme.firebase.foreignProjects.ForeignProjectsFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidRootTasksManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.roottask.RootTaskDependencyCoordinator
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.firebase.roottask.RootTasksLoader
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.SingleParamObservableSource
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.firebase.json.projects.SharedOwnedProjectJson
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.json.users.UserJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.ScheduleId
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

    private inner class TestRootTasksLoaderProvider : RootTasksLoader.Provider {

        private val singleParamObservableSource = SingleParamObservableSource<TaskKey.Root, Snapshot<RootTaskJson>>()

        override fun getRootTaskObservable(taskKey: TaskKey.Root) =
            singleParamObservableSource.getObservable(taskKey)

        fun accept(taskKey: TaskKey.Root, json: RootTaskJson) {
            singleParamObservableSource.accept(taskKey, Snapshot(taskKey.taskId, json))

            triggerRelay.accept(Unit)
        }
    }

    private val domainDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var triggerRelay: PublishRelay<Unit>

    private lateinit var privateProjectSnapshotObservable: PublishRelay<Snapshot<PrivateOwnedProjectJson>>
    private lateinit var rootTasksLoaderProvider: TestRootTasksLoaderProvider

    private lateinit var rootTasksLoader: RootTasksLoader
    private lateinit var projectsFactory: OwnedProjectsFactory

    private lateinit var sharedProjectKeysRelay: PublishRelay<Set<ProjectKey.Shared>>
    private lateinit var sharedProjectSnapshotRelay: PublishRelay<Snapshot<JsonWrapper>>

    private lateinit var rootTasksFactory: RootTasksFactory

    private lateinit var changeTypeSource: ChangeTypeSource

    private lateinit var projectEmissionChecker: EmissionChecker<Unit>
    private lateinit var taskEmissionChecker: EmissionChecker<Unit>

    @Before
    fun before() {
        rxErrorChecker = RxErrorChecker()

        privateProjectSnapshotObservable = PublishRelay.create()

        triggerRelay = PublishRelay.create()
    }

    @After
    fun after() {
        domainDisposable.clear()

        rxErrorChecker.check()
    }

    private fun immediateUserCustomTimeProviderSource() = object : UserCustomTimeProviderSource {

        override fun getUserCustomTimeProvider(projectRecord: ProjectRecord<*>) =
            mockk<JsonTime.UserCustomTimeProvider>()

        override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord) = mockk<JsonTime.UserCustomTimeProvider>()
    }

    private fun setup() {
        val triggerSource = mockk<RequestMerger.TriggerSource> {
            every { trigger } returns triggerRelay
        }

        val rootTaskKeySource = RootTaskKeyStore(triggerSource)

        val userCustomTimeProviderSource = immediateUserCustomTimeProviderSource()

        rootTasksLoaderProvider = TestRootTasksLoaderProvider()

        val databaseWrapper = mockk<DatabaseWrapper> {
            var taskRecordId = 0
            every { newRootTaskRecordId() } answers { "rootTaskRecordId" + taskRecordId++ }

            var scheduleRecordId = 0
            every { newRootTaskScheduleRecordId(any()) } answers {
                ScheduleId("scheduleRecordId" + scheduleRecordId++)
            }
        }

        val rootTasksManager = AndroidRootTasksManager(databaseWrapper)

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

        val existingInstanceChangeManager = RootModelChangeManager()

        val foreignProjectCoordinator = mockk<ForeignProjectCoordinator>(relaxed = true)
        val foreignProjectsFactory = mockk<ForeignProjectsFactory>(relaxed = true)

        val shownFactory = mockk<Instance.ShownFactory>()

        rootTasksFactory = RootTasksFactory(
            rootTasksLoader,
            userKeyStore,
            rootTaskToRootTaskCoordinator,
            domainDisposable,
            rootTaskKeySource,
            existingInstanceChangeManager,
            foreignProjectCoordinator,
            foreignProjectsFactory,
            Single.just(shownFactory),
        ) { projectsFactory }

        val privateProjectManager = AndroidPrivateProjectManager(DomainFactoryRule.deviceDbInfo.userInfo)

        val privateProjectLoader = ProjectLoader.Impl(
            privateProjectKey,
            privateProjectSnapshotObservable,
            domainDisposable,
            privateProjectManager,
            null,
            userCustomTimeProviderSource,
        ) {
            rootTaskKeySource.onProjectAddedOrUpdated(it.projectKey, it.rootTaskParentDelegate.rootTaskKeys)
        }

        val sharedProjectManager = AndroidSharedProjectManager(databaseWrapper)

        sharedProjectSnapshotRelay = PublishRelay.create()

        val sharedProjectsProvider = mockk<SharedProjectsProvider> {
            every { getProjectObservable(any()) } returns sharedProjectSnapshotRelay
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

        val projectsFactorySingle = Single.zip(
            privateProjectLoader.initialProjectEvent.map {
                check(it.changeType == ChangeType.REMOTE)

                it.data
            },
            sharedProjectsLoader.initialProjectsEvent,
        ) { initialPrivateProjectEvent, initialSharedProjectsEvent ->
            OwnedProjectsFactory(
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
            foreignProjectsFactory,
        )

        projectEmissionChecker =
            EmissionChecker(
                "projectsFactory",
                domainDisposable,
                projectsFactorySingle.flatMapObservable { it.remoteChanges })
        taskEmissionChecker = EmissionChecker("rootTasksFactory", domainDisposable, rootTasksFactory.remoteChanges)

        acceptSharedProjectKeys(setOf())
    }

    private fun acceptSharedProjectKeys(sharedProjectKeys: Set<ProjectKey.Shared>) =
        sharedProjectKeysRelay.accept(sharedProjectKeys)

    @Test
    fun testInitial() {
        setup()
        checkEmpty()
    }

    private fun acceptPrivateProject(privateProjectJson: PrivateOwnedProjectJson) {
        privateProjectSnapshotObservable.accept(Snapshot(privateProjectId, privateProjectJson))

        triggerRelay.accept(Unit)
    }

    private fun checkEmpty() {
        projectEmissionChecker.checkEmpty()
        taskEmissionChecker.checkEmpty()
    }

    @Test
    fun testSingleProjectEmission() {
        setup()

        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateOwnedProjectJson())
        checkEmpty()

        projectEmissionChecker.checkOne { acceptPrivateProject(PrivateOwnedProjectJson(defaultTimesCreated = true)) }
    }

    @Test
    fun testSingleProjectSingleTask() {
        setup()

        acceptPrivateProject(PrivateOwnedProjectJson())
        checkEmpty()

        projectEmissionChecker.checkOne {
            acceptPrivateProject(PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun testSingleProjectSingleTaskChangeProjectBeforeTask() {
        setup()

        acceptPrivateProject(PrivateOwnedProjectJson())
        checkEmpty()

        projectEmissionChecker.checkOne {
            acceptPrivateProject(PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }

        projectEmissionChecker.checkOne {
            acceptPrivateProject(
                PrivateOwnedProjectJson(startTime = 1, rootTaskIds = mutableMapOf(taskKey1.taskId to true))
            )
        }

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun testSingleProjectSingleTaskChangeProjectBeforeTaskByStrippingOutSecondTask() {
        setup()

        acceptPrivateProject(PrivateOwnedProjectJson())
        checkEmpty()

        projectEmissionChecker.checkOne {
            acceptPrivateProject(
                PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true, taskKey2.taskId to true))
            )
        }

        projectEmissionChecker.checkOne {
            acceptPrivateProject(PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun testSingleProjectSingleTaskChangeProjectBeforeTaskByStrippingOutSecondTaskDifferentOrder() {
        setup()

        acceptPrivateProject(PrivateOwnedProjectJson())
        checkEmpty()

        projectEmissionChecker.checkOne {
            acceptPrivateProject(
                PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true, taskKey2.taskId to true))
            )
        }

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                ),
            )
        }

        projectEmissionChecker.checkOne {
            acceptPrivateProject(PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }
    }

    @Test
    fun testSingleProjectChildTask() {
        setup()

        acceptPrivateProject(PrivateOwnedProjectJson())
        checkEmpty()

        projectEmissionChecker.checkOne {
            acceptPrivateProject(PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                    rootTaskIds = mutableMapOf(taskKey2.taskId to true),
                ),
            )
        }

        taskEmissionChecker.checkOne {
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

        acceptPrivateProject(PrivateOwnedProjectJson())
        checkEmpty()

        projectEmissionChecker.checkOne {
            acceptPrivateProject(PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))
        }

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                    rootTaskIds = mutableMapOf(taskKey2.taskId to true, taskKey3.taskId to true),
                ),
            )
        }

        taskEmissionChecker.checkOne {
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

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
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

        acceptPrivateProject(PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                ),
            )
        }

        checkEmpty()

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    name = "changedName",
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun testSingleProjectRemoveTaskFromProject() {
        setup()
        acceptPrivateProject(PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                ),
            )
        }

        checkEmpty()

        projectEmissionChecker.checkOne { acceptPrivateProject(PrivateOwnedProjectJson()) }
    }

    @Test
    fun testSingleProjectRemoveChildTask() {
        setup()
        acceptPrivateProject(PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                    rootTaskIds = mutableMapOf(taskKey2.taskId to true)
                ),
            )
        }

        taskEmissionChecker.checkOne {
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

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun testSingleProjectUpdateChildTask() {
        setup()
        acceptPrivateProject(PrivateOwnedProjectJson(rootTaskIds = mutableMapOf(taskKey1.taskId to true)))

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                    rootTaskIds = mutableMapOf(taskKey2.taskId to true)
                ),
            )
        }

        taskEmissionChecker.checkOne {
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

        taskEmissionChecker.checkOne {
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
    fun testSingleSharedProjectEmission() {
        setup()

        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateOwnedProjectJson())
        checkEmpty()

        sharedProjectKeysRelay.accept(setOf(sharedProjectKey))

        projectEmissionChecker.checkOne {
            sharedProjectSnapshotRelay.accept(
                Snapshot(
                    sharedProjectKey.key,
                    JsonWrapper(SharedOwnedProjectJson(users = mutableMapOf("key" to UserJson()))),
                ),
            )
        }
    }

    @Test
    fun testSingleSharedProjectSingleTask() {
        setup()

        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateOwnedProjectJson())
        checkEmpty()

        sharedProjectKeysRelay.accept(setOf(sharedProjectKey))

        projectEmissionChecker.checkOne {
            sharedProjectSnapshotRelay.accept(
                Snapshot(
                    sharedProjectKey.key,
                    JsonWrapper(
                        SharedOwnedProjectJson(
                            users = mutableMapOf("key" to UserJson()),
                            rootTaskIds = mutableMapOf(taskKey1.taskId to true),
                        )
                    ),
                )
            )

            triggerRelay.accept(Unit)
        }

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = sharedProjectKey.key,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                ),
            )
        }
    }

    private fun createTask(): TaskKey.Root {
        setup()

        // first load event for projectsFactory doesn't emit a change... apparently.
        acceptPrivateProject(PrivateOwnedProjectJson())

        val task = rootTasksFactory.createTask(
            ExactTimeStamp.Local.now,
            null,
            "task",
            null,
            null,
        ).apply {
            createSchedules(
                ExactTimeStamp.Local.now,
                listOf(ScheduleData.Single(Date.today(), TimePair(HourMinute.now))),
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

        projectEmissionChecker.checkOne {
            acceptPrivateProject(
                PrivateOwnedProjectJson(
                    rootTaskIds = mutableMapOf(taskKey1.taskId to true, taskKey.taskId to true)
                )
            )
        }

        taskEmissionChecker.checkOne {
            rootTasksLoaderProvider.accept(
                taskKey1,
                RootTaskJson(
                    noScheduleOrParent = mapOf(
                        "noScheduleOrParentId" to RootNoScheduleOrParentJson(
                            startTimeOffset = 0.0,
                            projectId = privateProjectId,
                            projectKey = privateProjectKey.toJson(),
                        ),
                    ),
                ),
            )
        }

        rootTasksFactory.getRootTask(taskKey)
    }
}