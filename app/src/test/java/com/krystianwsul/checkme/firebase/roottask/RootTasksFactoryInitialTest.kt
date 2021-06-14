package com.krystianwsul.checkme.firebase.roottask

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

        val rootTasksFactory = RootTasksFactory(
            rootTasksLoader,
            mockk(),
            mockk(),
            compositeDisposable,
            mockk(),
            mockk(),
            mockk(),
            mockk(), // todo load
        )

        assertTrue(rootTasksFactory.rootTasks.isEmpty())
    }
}