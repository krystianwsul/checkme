package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.ProjectUserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.RootTasksManager
import com.krystianwsul.checkme.firebase.roottask.*
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.getCurrentValue
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.time.ExactTimeStamp
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Test

class ChangeTypeSourceTest {

    private val domainDisposable = CompositeDisposable()

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

        val privateProjectSnapshotObservable = PublishRelay.create<Snapshot<PrivateProjectJson>>()

        val privateProjectManager = mockk<AndroidPrivateProjectManager>()

        val projectUserCustomTimeProviderSource = mockk<ProjectUserCustomTimeProviderSource>()

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
            every { initialProjectsEvent } returns Single.just(mockk())
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
            ) { mockk() }
        }.cache()

        val changeTypeSource = ChangeTypeSource(
                projectsFactorySingle,
                Single.never(),
                DatabaseRx(domainDisposable, Observable.never()),
                Single.never(),
                rootTasksFactory,
        )

        val emissionChecker = EmissionChecker("changeTypes", domainDisposable, changeTypeSource.changeTypes)

        emissionChecker.checkEmpty()
    }
}