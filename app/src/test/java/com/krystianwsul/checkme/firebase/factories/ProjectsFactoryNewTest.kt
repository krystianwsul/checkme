package com.krystianwsul.checkme.firebase.factories

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.firebase.checkLocal
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
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey
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
import org.junit.BeforeClass
import org.junit.Test

@ExperimentalStdlibApi
class ProjectsFactoryNewTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun beforeClassStatic() {
            Task.USE_ROOT_INSTANCES = true
        }
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var privateProjectRelay: PublishRelay<Snapshot<PrivateProjectJson>>
    private lateinit var factoryProvider: ProjectFactoryNewTest.TestFactoryProvider
    private lateinit var privateProjectManager: AndroidPrivateProjectManager
    private lateinit var privateProjectLoader: ProjectLoader.Impl<ProjectType.Private, PrivateProjectJson>
    private lateinit var projectKeysRelay: PublishRelay<ChangeWrapper<Set<ProjectKey.Shared>>>
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
        factoryProvider = ProjectFactoryNewTest.TestFactoryProvider()
        privateProjectManager = AndroidPrivateProjectManager(userInfo, factoryProvider.database)

        privateProjectLoader = ProjectLoader.Impl(
                privateProjectRelay,
                compositeDisposable,
                factoryProvider.projectProvider,
                privateProjectManager
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
                factoryProvider.sharedProjectsProvider
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
        _projectsFactory = ProjectsFactory(
                mockk(),
                privateProjectLoader,
                initialProjectEvent!!,
                sharedProjectsLoader,
                initialProjectsEvent!!,
                ExactTimeStamp.Local.now,
                factoryProvider,
                compositeDisposable
        ) { DeviceDbInfo(DeviceInfo(userInfo, "token"), "uuid") }

        _emissionChecker = EmissionChecker("changeTypes", compositeDisposable, projectsFactory.changeTypes)
    }

    @Test
    fun testProjectEventsBeforeProjectsFactory() {
        val privateProjectKey = ProjectKey.Private("key")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        val name = "privateProject"
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson(name)))

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(sharedProjectKey)))
        factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(users = mutableMapOf(
                userInfo.key.key to mockk(relaxed = true) {
                    every { tokens } returns mutableMapOf()
                }
        )))

        initProjectsFactory()

        assertEquals(name, projectsFactory.privateProject.name)
        assertTrue(projectsFactory.sharedProjects.isNotEmpty())
    }

    @Test
    fun testLocalPrivateProjectChange() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        val name1 = "name1"

        emissionChecker.checkRemote {
            privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson(name1)))
        }
        assertEquals(projectsFactory.privateProject.name, name1)

        val name2 = "name2"

        projectsFactory.privateProject.name = name2
        projectsFactory.save()

        emissionChecker.checkLocal {
            privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson(name2)))
        }
        assertEquals(projectsFactory.privateProject.name, name2)
    }

    @Test
    fun testRemotePrivateAddTaskNoInstances() {
        val privateProjectKey = ProjectKey.Private("key")
        val taskKey = TaskKey(privateProjectKey, "taskKey")

        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        privateProjectRelay.accept(Snapshot(
                privateProjectKey.key,
                PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task"))),
        ))

        emissionChecker.checkRemote {
            factoryProvider.projectProvider.acceptInstance(privateProjectKey.key, taskKey.taskId, mapOf())
        }
        assertEquals(projectsFactory.privateProject.tasks.size, 1)
    }

    @Test
    fun testRemotePrivateAddTaskWithInstances() {
        val privateProjectKey = ProjectKey.Private("key")
        val taskKey = TaskKey(privateProjectKey, "taskKey")

        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        privateProjectRelay.accept(Snapshot(
                privateProjectKey.key,
                PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task"))),
        ))

        val date = Date.today()
        val hourMinute = HourMinute.now
        val done = ExactTimeStamp.Local.now
        val scheduleKey = ScheduleKey(date, TimePair(hourMinute))

        emissionChecker.checkRemote {
            factoryProvider.projectProvider.acceptInstance(
                    privateProjectKey.key,
                    taskKey.taskId,
                    mapOf(
                            InstanceRecord.scheduleKeyToDateString(scheduleKey, true) to mapOf(
                                    Pair(
                                            InstanceRecord.scheduleKeyToTimeString(scheduleKey, true) as String,
                                            InstanceJson(done = done.long)
                                    )
                            )
                    )
            )
        }
        assertEquals(projectsFactory.privateProject.tasks.size, 1)
    }

    @Test
    fun testLocalPrivateAddTaskNoInstances() {
        val privateProjectKey = ProjectKey.Private("key")

        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        val taskJson = PrivateTaskJson("task")

        val taskKey = projectsFactory.privateProject
                .newTask(taskJson)
                .taskKey

        projectsFactory.save()

        privateProjectRelay.accept(Snapshot(
                privateProjectKey.key,
                PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to taskJson)),
        ))

        emissionChecker.checkLocal {
            factoryProvider.projectProvider.acceptInstance(privateProjectKey.key, taskKey.taskId, mapOf())
        }
    }

    @Test
    fun testRemotePrivateChangeTask() {
        val privateProjectKey = ProjectKey.Private("key")
        val taskKey = TaskKey(privateProjectKey, "taskKey")

        privateProjectRelay.accept(
                Snapshot(
                        privateProjectKey.key,
                        PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task")))),
        )

        factoryProvider.projectProvider.acceptInstance(privateProjectKey.key, taskKey.taskId, mapOf())

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        val name = "task1"

        emissionChecker.checkRemote {
            privateProjectRelay.accept(Snapshot(
                    privateProjectKey.key,
                    PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson(name))),
            ))
        }
        assertEquals(projectsFactory.privateProject.tasks.single().name, name)
    }

    @Test
    fun testLocalPrivateChangeTask() {
        val privateProjectKey = ProjectKey.Private("key")
        val taskKey = TaskKey(privateProjectKey, "taskKey")

        privateProjectRelay.accept(
                Snapshot(
                        privateProjectKey.key,
                        PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task")))),
        )

        factoryProvider.projectProvider.acceptInstance(privateProjectKey.key, taskKey.taskId, mapOf())

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        val name = "task1"

        projectsFactory.privateProject
                .tasks
                .single()
                .setName(name, null)
        projectsFactory.save()

        emissionChecker.checkLocal {
            privateProjectRelay.accept(Snapshot(
                    privateProjectKey.key,
                    PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson(name))),
            ))
        }
        assertEquals(projectsFactory.privateProject.tasks.single().name, name)
    }

    @Test
    fun testPrivateRemoveTask() {
        val privateProjectKey = ProjectKey.Private("key")
        val taskKey = TaskKey(privateProjectKey, "taskKey")

        privateProjectRelay.accept(
                Snapshot(
                        privateProjectKey.key,
                        PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task")))),
        )

        factoryProvider.projectProvider.acceptInstance(privateProjectKey.key, taskKey.taskId, mapOf())

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        emissionChecker.checkRemote {
            privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))
        }
        assertTrue(projectsFactory.privateProject.tasks.isEmpty())
    }

    @Test
    fun testLocalPrivateInstanceChange() {
        val privateProjectKey = ProjectKey.Private("key")

        val taskKey = TaskKey(privateProjectKey, "taskKey")

        privateProjectRelay.accept(Snapshot(
                privateProjectKey.key,
                PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task"))),
        ))

        factoryProvider.projectProvider.acceptInstance(privateProjectKey.key, taskKey.taskId, mapOf())

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        val date = Date.today()
        val hourMinute = HourMinute.now

        val instance = projectsFactory.privateProject
                .tasks
                .single()
                .getInstance(DateTime(date, Time.Normal(hourMinute)))

        val done = ExactTimeStamp.Local.now

        instance.setDone(mockk(relaxed = true), true, done)
        projectsFactory.save()

        val scheduleKey = ScheduleKey(date, TimePair(hourMinute))

        emissionChecker.checkLocal {
            factoryProvider.projectProvider.acceptInstance(
                    privateProjectKey.key,
                    taskKey.taskId,
                    mapOf(
                            InstanceRecord.scheduleKeyToDateString(scheduleKey, true) to mapOf(
                                    Pair(
                                            InstanceRecord.scheduleKeyToTimeString(scheduleKey, true) as String,
                                            InstanceJson(done = done.long)
                                    )
                            )
                    )
            )
        }
    }

    @Test
    fun testRemotePrivateInstanceChange() {
        val privateProjectKey = ProjectKey.Private("key")

        val taskKey = TaskKey(privateProjectKey, "taskKey")

        privateProjectRelay.accept(Snapshot(
                privateProjectKey.key,
                PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to PrivateTaskJson("task"))),
        ))

        factoryProvider.projectProvider.acceptInstance(privateProjectKey.key, taskKey.taskId, mapOf())

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        val date = Date.today()
        val hourMinute = HourMinute.now
        val done = ExactTimeStamp.Local.now
        val scheduleKey = ScheduleKey(date, TimePair(hourMinute))

        emissionChecker.checkRemote {
            factoryProvider.projectProvider.acceptInstance(
                    privateProjectKey.key,
                    taskKey.taskId,
                    mapOf(
                            InstanceRecord.scheduleKeyToDateString(scheduleKey, true) to mapOf(
                                    Pair(
                                            InstanceRecord.scheduleKeyToTimeString(scheduleKey, true) as String,
                                            InstanceJson(done = done.long)
                                    )
                            )
                    )
            )
        }
    }

    @Test
    fun testAddSharedProjectRemote() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(sharedProjectKey)))

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

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        initProjectsFactory()

        val sharedProject = projectsFactory.createProject(
                "sharedProject",
                ExactTimeStamp.Local.now,
                setOf(),
                mockk(relaxed = true) {
                    every { userJson } returns UserJson()
                },
                userInfo,
                mockk(relaxed = true)
        )
        projectsFactory.save()

        projectKeysRelay.accept(ChangeWrapper(ChangeType.LOCAL, setOf(sharedProject.projectKey)))

        emissionChecker.checkLocal {
            factoryProvider.acceptSharedProject(sharedProject.projectKey, SharedProjectJson(users = mutableMapOf(
                    userInfo.key.key to mockk(relaxed = true) {
                        every { tokens } returns mutableMapOf()
                    }
            )))
        }
    }

    @Test
    fun testRemoveSharedProjectRemote() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(sharedProjectKey)))

        factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(users = mutableMapOf(
                userInfo.key.key to mockk(relaxed = true) {
                    every { tokens } returns mutableMapOf()
                }
        )))

        initProjectsFactory()

        emissionChecker.checkRemote {
            projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))
        }
    }

    @Test
    fun testChangeSharedProjectRemote() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(sharedProjectKey)))

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

    @Test
    fun testChangeSharedProjectRemoteAddRemoteTaskWithInstances() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(sharedProjectKey)))

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
                name
        )

        val taskKey = TaskKey(privateProjectKey, "taskKey")

        factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(
                users = mutableMapOf(
                        userInfo.key.key to mockk(relaxed = true) {
                            every { tokens } returns mutableMapOf()
                        }
                ),
                tasks = mutableMapOf(taskKey.taskId to SharedTaskJson("task"))
        ))

        val date = Date.today()
        val hourMinute = HourMinute.now
        val done = ExactTimeStamp.Local.now
        val scheduleKey = ScheduleKey(date, TimePair(hourMinute))

        emissionChecker.checkRemote {
            factoryProvider.projectProvider.acceptInstance(
                    sharedProjectKey.key,
                    taskKey.taskId,
                    mapOf(
                            InstanceRecord.scheduleKeyToDateString(scheduleKey, true) to mapOf(
                                    Pair(
                                            InstanceRecord.scheduleKeyToTimeString(scheduleKey, true) as String,
                                            InstanceJson(done = done.long)
                                    )
                            )
                    )
            )
        }
        assertEquals(
                1,
                projectsFactory.sharedProjects
                        .values
                        .single()
                        .tasks
                        .size
        )
        assertEquals(
                1,
                projectsFactory.sharedProjects
                        .values
                        .single()
                        .tasks
                        .single()
                        .existingInstances
                        .size
        )
    }

    @Test
    fun testChangeSharedProjectLocal() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(Snapshot(privateProjectKey.key, PrivateProjectJson()))

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(sharedProjectKey)))

        factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(users = mutableMapOf(
                userInfo.key.key to mockk(relaxed = true) {
                    every { tokens } returns mutableMapOf()
                }
        )))

        initProjectsFactory()

        val name = "name"

        projectsFactory.sharedProjects
                .values
                .single()
                .name = name

        projectsFactory.save()

        emissionChecker.checkLocal {
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
                name
        )
    }
}