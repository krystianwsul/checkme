package com.krystianwsul.checkme.domainmodel.update

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.mockk.mockk
import io.reactivex.rxjava3.disposables.Disposable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AndroidDomainUpdaterTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private lateinit var isReady: PublishRelay<NullableWrapper<DomainFactory>>
    private lateinit var queue: AndroidDomainUpdater.Queue
    private lateinit var queueDisposable: Disposable
    private lateinit var results: MutableList<Int>

    @Before
    fun before() {
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

    @Test
    fun testSingleItemNotReady() {
        isReady.accept(NullableWrapper())

        val testObserver = queue.add { _, _ ->
            results.add(0)

            DomainUpdater.Result("a", mockk<DomainUpdater.Params>())
        }.test()

        assertEquals(listOf<Int>(), results)
        testObserver.assertEmpty()
    }
}