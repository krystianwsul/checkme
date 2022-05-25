package com.krystianwsul.checkme.firebase.factories

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.firebase.TestUserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.checkRemote
import com.krystianwsul.checkme.firebase.loaders.*
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.projects.SharedProjectJson
import com.krystianwsul.common.firebase.json.tasks.PrivateTaskJson
import com.krystianwsul.common.firebase.json.users.UserJson
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalStdlibApi
class ProjectsFactoryTest {

    companion object {

        private val userInfo = UserInfo("email", "name", "uid")
        private val deviceDbInfo = DeviceDbInfo(DeviceInfo(userInfo, "token"), "uuid")
    }

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var privateProjectRelay: PublishRelay<Snapshot<PrivateProjectJson>>
    private lateinit var factoryProvider: ProjectFactoryTest.TestFactoryProvider
    private lateinit var privateProjectManager: AndroidPrivateProjectManager
    private lateinit var privateProjectLoader: ProjectLoader.Impl<ProjectType.Private, PrivateProjectJson>
    private lateinit var projectKeysRelay: PublishRelay<Set<ProjectKey.Shared>>
    private lateinit var sharedProjectManager: AndroidSharedProjectManager
    private lateinit var sharedProjectsLoader: SharedProjectsLoader.Impl

    private var initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private, PrivateProjectJson>? = null
    private var initialProjectsEvent: SharedProjectsLoader.InitialProjectsEvent? = null

    private var _projectsFactory: ProjectsFactory? = null
    private val projectsFactory get() = _projectsFactory!!

    private var _emissionChecker: EmissionChecker<ChangeType>? = null
    private val emissionChecker get() = _emissionChecker!!

    private val userInfo = UserInfo("email", "name", "uid")

    private fun ProjectsFactory.save() = save(mockk(relaxed = true))

    @Before
    fun before() {
        mockBase64()
        ErrorLogger.instance = mockk(relaxed = true)

        rxErrorChecker = RxErrorChecker()

        privateProjectRelay = PublishRelay.create()
        factoryProvider = ProjectFactoryTest.TestFactoryProvider()
        privateProjectManager = AndroidPrivateProjectManager(userInfo)

        privateProjectLoader = ProjectLoader.Impl(
            privateProjectRelay,
            compositeDisposable,
            privateProjectManager,
            null,
            TestUserCustomTimeProviderSource(),
            mockk(relaxed = true),
        )

        initialProjectEvent = null
        privateProjectLoader.initialProjectEvent
            .subscribeBy { initialProjectEvent = it.data }
            .addTo(compositeDisposable)

        projectKeysRelay = PublishRelay.create()
        sharedProjectManager = AndroidSharedProjectManager(factoryProvider.database)

        sharedProjectsLoader = SharedProjectsLoader.Impl(
            projectKeysRelay,
            sharedProjectManager,
            compositeDisposable,
            factoryProvider.sharedProjectsProvider,
            TestUserCustomTimeProviderSource(),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )

        initialProjectsEvent = null
        sharedProjectsLoader.initialProjectsEvent
            .subscribeBy { initialProjectsEvent = it }
            .addTo(compositeDisposable)

        _projectsFactory = null
        _emissionChecker = null
    }

    @After
    fun after() {
        compositeDisposable.clear()

        rxErrorChecker.check()
    }

    private fun initProjectsFactory() {
        val existingInstanceChangeManager = RootModelChangeManager()

        _projectsFactory = ProjectsFactory(
            privateProjectLoader,
            initialProjectEvent!!,
            sharedProjectsLoader,
            initialProjectsEvent!!,
            ExactTimeStamp.Local.now,
            factoryProvider.shownFactory,
            compositeDisposable,
            mockk(relaxed = true),
            existingInstanceChangeManager,
        ) { deviceDbInfo }

        _emissionChecker = EmissionChecker("changeTypes", compositeDisposable, projectsFactory.changeTypes)
    }

    @Test
    fun testProjectEventsBeforeProjectsFactory() {
        val privateProjectKey = ProjectKey.Private("key")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(setOf())

        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")

        projectKeysRelay.accept(setOf(sharedProjectKey))
        factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(users = mutableMapOf(
            userInfo.key.key to mockk(relaxed = true) {
                every { tokens } returns mutableMapOf()
            }
        )))

        initProjectsFactory()

        assertTrue(projectsFactory.sharedProjects.isNotEmpty())
    }

    @Test
    fun testLocalPrivateProjectChange() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(setOf())

        initProjectsFactory()

        emissionChecker.checkRemote {
            privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson(defaultTimesCreated = true)))
        }
        assertEquals(projectsFactory.privateProject.defaultTimesCreated, true)

        projectsFactory.privateProject.defaultTimesCreated = false
        projectsFactory.save()

        // doesn't emit ChangeType.LOCAL
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson(defaultTimesCreated = false)))
        assertEquals(projectsFactory.privateProject.defaultTimesCreated, false)
    }

    @Test
    fun testRemotePrivateAddTaskNoInstances() {
        val privateProjectKey = ProjectKey.Private("key")
        val taskKey = TaskKey.Project(privateProjectKey, "taskKey")

        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(setOf())

        initProjectsFactory()

        emissionChecker.checkRemote {
            privateProjectRelay.accept(
                Snapshot(
                    privateProjectKey.key,
                    PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task"))),
                )
            )
        }
        assertEquals(projectsFactory.privateProject.projectTasks.size, 1)
    }

    @Test
    fun testRemotePrivateAddTaskWithInstances() {
        val privateProjectKey = ProjectKey.Private("key")
        val taskKey = TaskKey.Project(privateProjectKey, "taskKey")

        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(setOf())

        initProjectsFactory()

        emissionChecker.checkRemote {
            privateProjectRelay.accept(
                Snapshot(
                    privateProjectKey.key,
                    PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task"))),
                )
            )
        }
        assertEquals(projectsFactory.privateProject.projectTasks.size, 1)
    }

    @Test
    fun testRemotePrivateChangeTask() {
        val privateProjectKey = ProjectKey.Private("key")
        val taskKey = TaskKey.Project(privateProjectKey, "taskKey")

        privateProjectRelay.accept(
            Snapshot(
                privateProjectKey.key,
                PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task"))),
            )
        )

        projectKeysRelay.accept(setOf())

        initProjectsFactory()

        val name = "task1"

        emissionChecker.checkRemote {
            privateProjectRelay.accept(
                Snapshot(
                    privateProjectKey.key,
                    PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson(name))),
                )
            )
        }
        assertEquals(projectsFactory.privateProject.projectTasks.single().name, name)
    }

    @Test
    fun testPrivateRemoveTask() {
        val privateProjectKey = ProjectKey.Private("key")
        val taskKey = TaskKey.Project(privateProjectKey, "taskKey")

        privateProjectRelay.accept(
            Snapshot(
                privateProjectKey.key,
                PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task"))),
            )
        )

        projectKeysRelay.accept(setOf())

        initProjectsFactory()

        emissionChecker.checkRemote {
            privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))
        }
        assertTrue(projectsFactory.privateProject.projectTasks.isEmpty())
    }

    @Test
    fun testLocalPrivateInstanceChange() {
        val privateProjectKey = ProjectKey.Private("key")

        val taskKey = TaskKey.Project(privateProjectKey, "taskKey")

        privateProjectRelay.accept(
            Snapshot(
                privateProjectKey.key,
                PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task"))),
            )
        )

        projectKeysRelay.accept(setOf())

        initProjectsFactory()

        val date = Date.today()
        val hourMinute = HourMinute.now

        val instance = projectsFactory.privateProject
            .projectTasks
            .single()
            .getInstance(DateTime(date, Time.Normal(hourMinute)))

        val done = ExactTimeStamp.Local.now

        instance.setDone(mockk(relaxed = true), true, done)
        projectsFactory.save()
    }

    @Test
    fun testRemotePrivateInstanceChange() {
        val privateProjectKey = ProjectKey.Private("key")

        val taskKey = TaskKey.Project(privateProjectKey, "taskKey")

        privateProjectRelay.accept(
            Snapshot(
                privateProjectKey.key,
                PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task"))),
            )
        )

        projectKeysRelay.accept(setOf())

        initProjectsFactory()
    }

    @Test
    fun testAddSharedProjectRemote() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(setOf())

        initProjectsFactory()

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")

        projectKeysRelay.accept(setOf(sharedProjectKey))

        emissionChecker.checkRemote {
            factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(users = mutableMapOf(
                userInfo.key.key to mockk(relaxed = true) {
                    every { tokens } returns mutableMapOf()
                }
            )))
        }
    }

    @Test
    fun testAddSharedProjectLocal() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(setOf())

        initProjectsFactory()

        val name = "sharedProject"
        val now = ExactTimeStamp.Local.now

        // doesn't emit ChangeType.LOCAL
        val sharedProject = projectsFactory.createProject(
            name,
            now,
            setOf(),
            mockk(relaxed = true) {
                every { userJson } returns UserJson()
            },
            userInfo,
            mockk(relaxed = true),
        )
        projectsFactory.save()

        // doesn't emit ChangeType.LOCAL
        factoryProvider.acceptSharedProject(
            sharedProject.projectKey,
            SharedProjectJson(
                name,
                now.long,
                now.offset,
                users = mutableMapOf(userInfo.key.key to newUserJson()),
            ),
        )
    }

    @Test
    fun testRemoveSharedProjectRemote() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")
        projectKeysRelay.accept(setOf(sharedProjectKey))

        factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(users = mutableMapOf(
            userInfo.key.key to mockk(relaxed = true) {
                every { tokens } returns mutableMapOf()
            }
        )))

        initProjectsFactory()

        emissionChecker.checkRemote { projectKeysRelay.accept(setOf()) }
    }

    @Test
    fun testChangeSharedProjectRemote() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")
        projectKeysRelay.accept(setOf(sharedProjectKey))

        factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(users = mutableMapOf(
            userInfo.key.key to mockk(relaxed = true) {
                every { tokens } returns mutableMapOf()
            }
        )))

        initProjectsFactory()

        val name = "name"

        emissionChecker.checkRemote {
            factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(
                name = name,
                users = mutableMapOf(
                    userInfo.key.key to mockk(relaxed = true) {
                        every { tokens } returns mutableMapOf()
                    }
                )
            ))
        }
        assertEquals(
            projectsFactory.sharedProjects
                .values
                .single()
                .name,
            name,
        )
    }

    private fun newUserJson() =
        UserJson(name = userInfo.name, tokens = mutableMapOf(deviceDbInfo.uuid to deviceDbInfo.token))

    @Test
    fun testChangeSharedProjectLocal() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")
        projectKeysRelay.accept(setOf(sharedProjectKey))

        factoryProvider.acceptSharedProject(
            sharedProjectKey,
            SharedProjectJson(users = mutableMapOf(userInfo.key.key to newUserJson())),
        )

        initProjectsFactory()

        val name = "name"

        projectsFactory.sharedProjects
            .values
            .single()
            .name = name

        projectsFactory.save()

        // doesn't emit ChangeType.LOCAL
        factoryProvider.acceptSharedProject(
            sharedProjectKey,
            SharedProjectJson(
                name = name,
                users = mutableMapOf(userInfo.key.key to newUserJson()),
            ),
        )
        assertEquals(
            projectsFactory.sharedProjects
                .values
                .single()
                .name,
            name,
        )
    }
}