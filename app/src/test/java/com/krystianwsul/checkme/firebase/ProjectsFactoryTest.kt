package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.loaders.*
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey
import com.krystianwsul.common.utils.TaskKey
import io.mockk.every
import io.mockk.mockk
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class ProjectsFactoryTest {

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var privateProjectRelay: PublishRelay<Snapshot>
    private lateinit var factoryProvider: ProjectFactoryTest.TestFactoryProvider
    private lateinit var privateProjectManager: AndroidPrivateProjectManager
    private lateinit var privateProjectLoader: ProjectLoader.Impl<ProjectType.Private>
    private lateinit var projectKeysRelay: PublishRelay<ChangeWrapper<Set<ProjectKey.Shared>>>
    private lateinit var sharedProjectManager: AndroidSharedProjectManager
    private lateinit var sharedProjectsLoader: SharedProjectsLoader.Impl

    private var initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private>? = null
    private var initialProjectsEvent: SharedProjectsLoader.InitialProjectsEvent? = null

    private val userInfo = UserInfo("email", "name")

    @Before
    fun before() {
        mockBase64()
        ErrorLogger.instance = mockk(relaxed = true)

        rxErrorChecker = RxErrorChecker()

        privateProjectRelay = PublishRelay.create()
        factoryProvider = ProjectFactoryTest.TestFactoryProvider()
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
    }

    @After
    fun after() {
        compositeDisposable.clear()

        rxErrorChecker.check()
    }

    @Test
    fun testProjectEventsBeforeProjectsFactory() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(ValueTestSnapshot(PrivateProjectJson(), privateProjectKey.key))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        val name = "privateProject"
        privateProjectRelay.accept(ValueTestSnapshot(PrivateProjectJson(name), privateProjectKey.key))

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(sharedProjectKey)))
        factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(users = mutableMapOf(
                userInfo.key.key to mockk(relaxed = true) {
                    every { tokens } returns mutableMapOf()
                }
        )))

        val projectsFactory = ProjectsFactory(
                mockk(),
                privateProjectLoader,
                initialProjectEvent!!,
                sharedProjectsLoader,
                initialProjectsEvent!!,
                ExactTimeStamp.now,
                factoryProvider,
                compositeDisposable
        ) { DeviceDbInfo(DeviceInfo(userInfo, "token"), "uuid") }

        assertEquals(name, projectsFactory.privateProject.name)
        assertTrue(projectsFactory.sharedProjects.isNotEmpty())
    }

    @Test
    fun testLocalPrivateProjectChange() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(ValueTestSnapshot(PrivateProjectJson(), privateProjectKey.key))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        val projectsFactory = ProjectsFactory(
                mockk(),
                privateProjectLoader,
                initialProjectEvent!!,
                sharedProjectsLoader,
                initialProjectsEvent!!,
                ExactTimeStamp.now,
                factoryProvider,
                compositeDisposable
        ) { DeviceDbInfo(DeviceInfo(userInfo, "token"), "uuid") }

        val emissionChecker = EmissionChecker("changeTypes", compositeDisposable, projectsFactory.changeTypes)

        val name1 = "name1"

        emissionChecker.checkRemote()
        privateProjectRelay.accept(ValueTestSnapshot(PrivateProjectJson(name1), privateProjectKey.key))
        emissionChecker.checkEmpty()
        assertEquals(projectsFactory.privateProject.name, name1)

        val name2 = "name2"

        privateProjectManager.privateProjectRecords
                .single()
                .name = name2
        privateProjectManager.save(mockk(relaxed = true))

        emissionChecker.checkLocal()
        privateProjectRelay.accept(ValueTestSnapshot(PrivateProjectJson(name2), privateProjectKey.key))
        emissionChecker.checkEmpty()
        assertEquals(projectsFactory.privateProject.name, name2)
    }

    @Test
    fun testLocalPrivateInstanceChange() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")

        val taskKey = TaskKey(privateProjectKey, "taskKey")

        privateProjectRelay.accept(ValueTestSnapshot(
                PrivateProjectJson(tasks = mutableMapOf(taskKey.taskId to TaskJson("task"))),
                privateProjectKey.key
        ))

        factoryProvider.projectProvider.acceptInstance(privateProjectKey.key, taskKey.taskId, mapOf())

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        val projectsFactory = ProjectsFactory(
                mockk(),
                privateProjectLoader,
                initialProjectEvent!!,
                sharedProjectsLoader,
                initialProjectsEvent!!,
                ExactTimeStamp.now,
                factoryProvider,
                compositeDisposable
        ) { DeviceDbInfo(DeviceInfo(userInfo, "token"), "uuid") }

        val emissionChecker = EmissionChecker("changeTypes", compositeDisposable, projectsFactory.changeTypes)

        val date = Date.today()
        val hourMinute = HourMinute.now

        val instance = projectsFactory.privateProject
                .tasks
                .single()
                .getInstance(DateTime(date, Time.Normal(hourMinute)))

        val done = ExactTimeStamp.now

        instance.setDone("uuid", mockk(), true, done)
        projectsFactory.save(mockk(relaxed = true))

        val scheduleKey = ScheduleKey(date, TimePair(hourMinute))

        emissionChecker.checkLocal()
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
        emissionChecker.checkEmpty()
    }

    @Test
    fun testAddSharedProjectRemote() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(ValueTestSnapshot(PrivateProjectJson(), privateProjectKey.key))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        val projectsFactory = ProjectsFactory(
                mockk(),
                privateProjectLoader,
                initialProjectEvent!!,
                sharedProjectsLoader,
                initialProjectsEvent!!,
                ExactTimeStamp.now,
                factoryProvider,
                compositeDisposable
        ) { DeviceDbInfo(DeviceInfo(userInfo, "token"), "uuid") }

        val emissionChecker = EmissionChecker("changeTypes", compositeDisposable, projectsFactory.changeTypes)

        val sharedProjectKey = ProjectKey.Shared("sharedProjectKey")

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(sharedProjectKey)))

        emissionChecker.checkRemote()
        factoryProvider.acceptSharedProject(sharedProjectKey, SharedProjectJson(users = mutableMapOf(
                userInfo.key.key to mockk(relaxed = true) {
                    every { tokens } returns mutableMapOf()
                }
        )))
        emissionChecker.checkEmpty()
    }

    @Test
    fun testAddSharedProjectLocal() {
        val privateProjectKey = ProjectKey.Private("privateProjectKey")
        privateProjectRelay.accept(ValueTestSnapshot(PrivateProjectJson(), privateProjectKey.key))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        val projectsFactory = ProjectsFactory(
                mockk(),
                privateProjectLoader,
                initialProjectEvent!!,
                sharedProjectsLoader,
                initialProjectsEvent!!,
                ExactTimeStamp.now,
                factoryProvider,
                compositeDisposable
        ) { DeviceDbInfo(DeviceInfo(userInfo, "token"), "uuid") }

        val emissionChecker = EmissionChecker("changeTypes", compositeDisposable, projectsFactory.changeTypes)

        val sharedProject = projectsFactory.createProject(
                "sharedProject",
                ExactTimeStamp.now,
                setOf(),
                mockk(relaxed = true) {
                    every { userJson } returns UserJson()
                },
                userInfo,
                mockk(relaxed = true)
        )
        projectsFactory.save(mockk(relaxed = true))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.LOCAL, setOf(sharedProject.id)))

        emissionChecker.checkLocal()
        factoryProvider.acceptSharedProject(sharedProject.id, SharedProjectJson(users = mutableMapOf(
                userInfo.key.key to mockk(relaxed = true) {
                    every { tokens } returns mutableMapOf()
                }
        )))
        emissionChecker.checkEmpty()
    }
}