package com.krystianwsul.checkme.domainmodel.update

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.mockk.mockk
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AndroidDomainUpdaterTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private var counter = 0

    private lateinit var isReady: PublishRelay<NullableWrapper<DomainFactory>>
    private lateinit var queue: AndroidDomainUpdater.Queue
    private lateinit var queueDisposable: Disposable
    private lateinit var results: MutableList<Int>

    @Before
    fun before() {
        counter = 0

        isReady = PublishRelay.create()
        queue = AndroidDomainUpdater.Queue(isReady)
        queueDisposable = queue.subscribe()
        results = mutableListOf()
    }

    @After
    fun after() {
        queueDisposable.dispose()
    }

    @Test
    fun testInitQueue() {
        isReady.accept(NullableWrapper())
    }

    private fun addItem(): TestObserver<String> {
        val myCounter = ++counter

        return queue.add { _, _ ->
            results.add(myCounter)

            DomainUpdater.Result(myCounter.toString(), mockk<DomainUpdater.Params>(relaxed = true))
        }.test()
    }

    @Test
    fun testNotReady() {
        isReady.accept(NullableWrapper())

        val testObserver1 = addItem()

        assertEquals(listOf<Int>(), results)
        testObserver1.assertEmpty()

        val testObserver2 = addItem()

        assertEquals(listOf<Int>(), results)
        testObserver2.assertEmpty()
    }

    private fun mockedDomainFactory() = mockk<DomainFactory>(relaxed = true)

    @Test
    fun testInitiallyReady() {
        isReady.accept(NullableWrapper(mockedDomainFactory()))

        val testObserver1 = addItem()

        assertEquals(listOf(1), results)
        testObserver1.assertValue("1")

        val testObserver2 = addItem()

        assertEquals(listOf(1, 2), results)
        testObserver2.assertValue("2")
    }
}