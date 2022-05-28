package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.foreignProjects.ForeignProjectCoordinator
import com.krystianwsul.checkme.firebase.foreignProjects.ForeignProjectsFactory
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

class RootTasksFactoryInitialTest {

    private val compositeDisposable = CompositeDisposable()

    @After
    fun after() {
        compositeDisposable.clear()
    }

    @Test
    fun testInitialHasMap() {
        val rootTasksLoader = mockk<RootTasksLoader> {
            every { addChangeEvents } returns Observable.never()
            every { removeEvents } returns Observable.never()
        }

        val rootModelChangeManager = RootModelChangeManager()

        val foreignProjectCoordinator = mockk<ForeignProjectCoordinator>(relaxed = true)
        val foreignProjectsFactory = mockk<ForeignProjectsFactory>(relaxed = true)

        val rootTasksFactory = RootTasksFactory(
            rootTasksLoader,
            mockk(),
            mockk(),
            compositeDisposable,
            mockk(),
            rootModelChangeManager,
            foreignProjectCoordinator,
            foreignProjectsFactory,
            mockk(),
        )

        assertTrue(rootTasksFactory.rootTasks.isEmpty())
    }
}