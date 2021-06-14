package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.firebase.loaders.EmissionChecker
import com.krystianwsul.checkme.firebase.loaders.checkOne
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.junit.Test

class RootTasksFactoryTest {

    companion object {

        private val taskKey1 = TaskKey.Root("taskId1")
        private val taskKey2 = TaskKey.Root("taskId2")
    }

    private val domainDisposable = CompositeDisposable()

    private lateinit var addChangeEventsRelay: PublishRelay<RootTasksLoader.AddChangeEvent>

    private lateinit var emissionChecker: EmissionChecker<Unit>

    @Before
    fun before() {
        addChangeEventsRelay = PublishRelay.create()
        val removeEventsRelay = PublishRelay.create<RootTasksLoader.RemoveEvent>()

        val rootTasksFactory = RootTasksFactory(
            mockk {
                every { addChangeEvents } returns addChangeEventsRelay
                every { removeEvents } returns removeEventsRelay
            },
            mockk(),
            mockk {
                every { getDependencies(any()) } returns Single.just(mockk())
            },
            domainDisposable,
            mockk(),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(),
        )

        emissionChecker =
            EmissionChecker("unfilteredChanges", domainDisposable, rootTasksFactory.unfilteredChanges)
    }

    @After
    fun after() {
        domainDisposable.clear()
    }

    private fun newRecord(taskKey: TaskKey.Root) = mockk<RootTaskRecord>(relaxed = true) {
        every { this@mockk.taskKey } returns taskKey
    }

    @Test
    fun testInitialEvent() {
        val record = newRecord(taskKey1)

        emissionChecker.checkOne {
            addChangeEventsRelay.accept(RootTasksLoader.AddChangeEvent(record, false))
        }
    }

    @Test
    fun testInitialTwoAdds() {
        val record1 = newRecord(taskKey1)
        emissionChecker.checkOne {
            addChangeEventsRelay.accept(RootTasksLoader.AddChangeEvent(record1, false))
        }

        val record2 = newRecord(taskKey2)
        emissionChecker.checkOne {
            addChangeEventsRelay.accept(RootTasksLoader.AddChangeEvent(record2, false))
        }
    }

    @Test
    fun testInitialTwoAddsChangeFirst() {
        val record1 = newRecord(taskKey1)
        emissionChecker.checkOne {
            addChangeEventsRelay.accept(RootTasksLoader.AddChangeEvent(record1, false))
        }

        val record2 = newRecord(taskKey2)
        emissionChecker.checkOne {
            addChangeEventsRelay.accept(RootTasksLoader.AddChangeEvent(record2, false))
        }

        emissionChecker.checkOne {
            addChangeEventsRelay.accept(RootTasksLoader.AddChangeEvent(record1, false))
        }
    }

    @Test
    fun testInitialTwoAddsChangeSecond() {
        val record1 = newRecord(taskKey1)
        emissionChecker.checkOne {
            addChangeEventsRelay.accept(RootTasksLoader.AddChangeEvent(record1, false))
        }

        val record2 = newRecord(taskKey2)
        emissionChecker.checkOne {
            addChangeEventsRelay.accept(RootTasksLoader.AddChangeEvent(record2, false))
        }

        emissionChecker.checkOne {
            addChangeEventsRelay.accept(RootTasksLoader.AddChangeEvent(record2, false))
        }
    }
}