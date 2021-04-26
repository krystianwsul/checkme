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
import com.krystianwsul.checkme.utils.getCurrentValue
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.time.ExactTimeStamp
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

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            DomainThreadChecker.instance = mockk(relaxed = true)
        }
    }

    private val domainDisposable = CompositeDisposable()

    private lateinit var privateProjectSnapshotObservable: PublishRelay<Snapshot<PrivateProjectJson>>

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

        val rootTasksLoaderProvider = mockk<RootTasksLoader.Provider>()
        val rootTasksManager = mockk<RootTasksManager>()

        val rootTasksLoader = RootTasksLoader(
                rootTaskKeySource.rootTaskKeysObservable,
                rootTasksLoaderProvider,
                domainDisposable,
                rootTasksManager,
        )

        val rootTaskUserCustomTimeProviderSource = mockk<RootTaskUserCustomTimeProviderSource> {
            every { getUserCustomTimeProvider(any()) } returns mockk()
        }

        val userKeyStore = mockk<UserKeyStore>()

        val rootTaskToRootTaskCoordinator = RootTaskToRootTaskCoordinator.Impl(
                rootTaskKeySource,
                rootTasksLoader,
                domainDisposable,
                rootTaskUserCustomTimeProviderSource,
        )

        lateinit var projectsFactorySingle: Single<ProjectsFactory>

        val rootTasksFactory = RootTasksFactory(
                rootTasksLoader,
                rootTaskUserCustomTimeProviderSource,
                userKeyStore,
                rootTaskToRootTaskCoordinator,
                domainDisposable,
                rootTaskKeySource,
        ) { projectsFactorySingle.getCurrentValue() }

        val databaseWrapper = mockk<DatabaseWrapper>()

        val privateProjectManager = AndroidPrivateProjectManager(
                DomainFactoryRule.deviceDbInfo.userInfo,
                databaseWrapper,
        )

        val projectUserCustomTimeProviderSource = mockk<ProjectUserCustomTimeProviderSource> {
            every { getUserCustomTimeProvider(any()) } returns Single.just(mockk())
        }

        val projectToRootTaskCoordinator = ProjectToRootTaskCoordinator.Impl(rootTaskKeySource, rootTasksFactory)

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
}