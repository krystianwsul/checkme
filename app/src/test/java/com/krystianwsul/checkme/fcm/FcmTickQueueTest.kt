package com.krystianwsul.checkme.fcm

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.subscribeOnDomain
import com.krystianwsul.checkme.ticks.Ticker
import io.mockk.every
import io.mockk.mockkObject
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.Before
import org.junit.Test

class FcmTickQueueTest {

    companion object {

        private const val DELAY = 1000L
        private const val HALF = 500L
    }

    private lateinit var tickEventsRelay: PublishRelay<TickEvent>
    private lateinit var testObserver: TestObserver<TickEvent>

    @Before
    fun before() {
        tickEventsRelay = PublishRelay.create()
        testObserver = tickEventsRelay.test()

        mockkObject(Ticker)

        every { Ticker.tick(any(), any()) } answers {
            Completable.fromCallable {
                tickEventsRelay.accept(TickEvent.BEGIN)

                Thread.sleep(DELAY)

                tickEventsRelay.accept(TickEvent.END)
            }.subscribeOnDomain()
        }
    }

    @Test
    fun testOne() {
        testObserver.assertEmpty()

        FcmTickQueue.enqueue()
        Thread.sleep(HALF)
        testObserver.assertValue(TickEvent.BEGIN)

        Thread.sleep(DELAY)
        testObserver.assertValues(TickEvent.BEGIN, TickEvent.END)
    }

    @Test
    fun testTwo() {
        testObserver.assertEmpty()

        FcmTickQueue.enqueue()
        FcmTickQueue.enqueue()

        Thread.sleep(HALF + DELAY * 2)
        testObserver.assertValues(TickEvent.BEGIN, TickEvent.END, TickEvent.BEGIN, TickEvent.END)
    }

    enum class TickEvent {

        BEGIN, END
    }
}