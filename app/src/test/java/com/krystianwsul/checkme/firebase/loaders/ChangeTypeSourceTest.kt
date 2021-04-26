package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.managers.RootTasksManager
import com.krystianwsul.checkme.firebase.roottask.*
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

        val rootTaskUserCustomTimeProviderSource = mockk<RootTaskUserCustomTimeProviderSource>()
        val userKeyStore = mockk<UserKeyStore>()
        val rootTaskToRootTaskCoordinator = mockk<RootTaskToRootTaskCoordinator>()

        val rootTasksFactory = RootTasksFactory(
                rootTasksLoader,
                rootTaskUserCustomTimeProviderSource,
                userKeyStore,
                rootTaskToRootTaskCoordinator,
                domainDisposable,
                rootTaskKeySource,
        ) { mockk() }

        val changeTypeSource = ChangeTypeSource(
                Single.never(),
                Single.never(),
                DatabaseRx(domainDisposable, Observable.never()),
                Single.never(),
                rootTasksFactory,
        )

        val emissionChecker = EmissionChecker("changeTypes", domainDisposable, changeTypeSource.changeTypes)

        emissionChecker.checkEmpty()
    }
}