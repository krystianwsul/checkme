package com.krystianwsul.checkme.domainmodel.update

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AndroidDomainUpdaterTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    @Test
    fun testInitQueue() {
        val isReady = PublishRelay.create<NullableWrapper<DomainFactory>>()

        val queue = AndroidDomainUpdater.Queue(isReady)
        val disposable = queue.subscribe()

        val results = mutableListOf<Int>()

        isReady.accept(NullableWrapper())

        disposable.dispose()
    }

    @Test
    fun testSingleItemNotReady() {
        val isReady = PublishRelay.create<NullableWrapper<DomainFactory>>()

        val queue = AndroidDomainUpdater.Queue(isReady)
        val disposable = queue.subscribe()

        val results = mutableListOf<Int>()

        isReady.accept(NullableWrapper())

        val testObserver = queue.add { domainFactory, local ->
            results += 0

            DomainUpdater.Result("a", mockk<DomainUpdater.Params>())
        }.test()

        assertEquals(listOf<Int>(), results)
        testObserver.assertEmpty()

        disposable.dispose()
    }
}