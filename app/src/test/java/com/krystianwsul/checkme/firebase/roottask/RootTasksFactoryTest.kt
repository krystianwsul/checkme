package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.firebase.loaders.EmissionChecker
import com.krystianwsul.checkme.firebase.loaders.checkOne
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Test

class RootTasksFactoryTest {

    private val domainDisposable = CompositeDisposable()

    @After
    fun after() {
        domainDisposable.clear()
    }

    @Test
    fun testInitialEvent() {
        val addChangeEventsRelay = PublishRelay.create<RootTasksLoader.AddChangeEvent>()
        val removeEventsRelay = PublishRelay.create<RootTasksLoader.RemoveEvent>()

        val rootTasksFactory = RootTasksFactory(
                mockk {
                    every { addChangeEvents } returns addChangeEventsRelay
                    every { removeEvents } returns removeEventsRelay
                },
                mockk {
                    every { getUserCustomTimeProvider(any()) } returns Single.just(mockk())
                },
                mockk(),
                mockk {
                    every { getRootTasks(any()) } returns Completable.complete()
                },
                domainDisposable,
                mockk(),
                mockk(relaxed = true),
                mockk(),
        )

        val emissionChecker =
                EmissionChecker("unfilteredChanges", domainDisposable, rootTasksFactory.unfilteredChanges)

        val record = mockk<RootTaskRecord>(relaxed = true) {
            every { taskKey } returns TaskKey.Root("taskKey")
        }

        emissionChecker.checkOne {
            addChangeEventsRelay.accept(RootTasksLoader.AddChangeEvent(record, false))
        }
    }
}