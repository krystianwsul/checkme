package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.loaders.*
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
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
    fun projectEventsBeforeProjectsFactory() {
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
}