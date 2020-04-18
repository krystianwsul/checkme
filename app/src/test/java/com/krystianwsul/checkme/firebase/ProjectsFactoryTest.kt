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
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.mockk.mockk
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class ProjectsFactoryTest {

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    @Before
    fun before() {
        mockBase64()

        rxErrorChecker = RxErrorChecker()
    }

    @After
    fun after() {
        compositeDisposable.clear()

        rxErrorChecker.check()
    }

    @Test
    fun projectEventsBeforeProjectsFactory() {
        val privateProjectRelay = PublishRelay.create<Snapshot>()

        val factoryProvider = ProjectFactoryTest.TestFactoryProvider()

        val userInfo = UserInfo("email", "name")

        val privateProjectManager = AndroidPrivateProjectManager(userInfo, factoryProvider.database)

        val privateProjectLoader = ProjectLoader.Impl(
                privateProjectRelay,
                compositeDisposable,
                factoryProvider.projectProvider,
                privateProjectManager
        )

        var initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private>? = null
        privateProjectLoader.initialProjectEvent
                .subscribeBy { initialProjectEvent = it.data }
                .addTo(compositeDisposable)

        val projectKeysRelay = PublishRelay.create<ChangeWrapper<Set<ProjectKey.Shared>>>()

        val sharedProjectManager = AndroidSharedProjectManager(factoryProvider.database)

        val sharedProjectsLoader = SharedProjectsLoader.Impl(
                projectKeysRelay,
                sharedProjectManager,
                compositeDisposable,
                factoryProvider.sharedProjectsProvider
        )

        var initialProjectsEvent: SharedProjectsLoader.InitialProjectsEvent? = null
        sharedProjectsLoader.initialProjectsEvent
                .subscribeBy { initialProjectsEvent = it }
                .addTo(compositeDisposable)

        val privateProjectKey = ProjectKey.Private("projectKey")
        privateProjectRelay.accept(ValueTestSnapshot(PrivateProjectJson(), privateProjectKey.key))

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))

        val name = "privateProject"
        privateProjectRelay.accept(ValueTestSnapshot(PrivateProjectJson(name), privateProjectKey.key))

        val projectsFactory = ProjectsFactory(
                mockk(),
                privateProjectLoader,
                initialProjectEvent!!,
                sharedProjectsLoader,
                initialProjectsEvent!!,
                ExactTimeStamp.now,
                factoryProvider,
                compositeDisposable
        ) { DeviceDbInfo(DeviceInfo(UserInfo("email", "name"), "token"), "uuid") }

        assertEquals(name, projectsFactory.privateProject.name)
    }
}