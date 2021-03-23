package com.krystianwsul.checkme.fcm

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.completeOnDomain
import com.krystianwsul.checkme.ticks.Ticker
import io.mockk.mockkObject
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Test

class FcmTickQueueTest {

    companion object {

        private const val DELAY = 100L
        private const val HALF = DELAY / 2
    }

    private var ordinal = 0

    private lateinit var fcmTickQueue: FcmTickQueue<Int>
    private lateinit var disposable: Disposable

    private lateinit var tickEventsRelay: PublishRelay<TickEvent>
    private lateinit var testObserver: TestObserver<TickEvent>

    @Before
    fun before() {
        ordinal = 0

        fcmTickQueue = FcmTickQueue {
            completeOnDomain {
                tickEventsRelay.accept(TickEvent.Begin(it))

                Thread.sleep(DELAY)

                tickEventsRelay.accept(TickEvent.End(it))
            }
        }

        disposable = fcmTickQueue.subscribe()

        tickEventsRelay = PublishRelay.create()
        testObserver = tickEventsRelay.test()

        mockkObject(Ticker)
    }

    @After
    fun after() {
        disposable.dispose()
    }

    private fun enqueue() {
        fcmTickQueue.enqueue(ordinal++)
    }

    @Test
    fun testOne() {
        testObserver.assertEmpty()

        enqueue()
        Thread.sleep(HALF)
        testObserver.assertValue(TickEvent.Begin(0))

        Thread.sleep(DELAY)
        testObserver.assertValues(TickEvent.Begin(0), TickEvent.End(0))
    }

    @Test
    fun testTwo() {
        testObserver.assertEmpty()

        enqueue()
        enqueue()

        Thread.sleep(HALF + DELAY * 2)

        testObserver.assertValues(
                TickEvent.Begin(0),
                TickEvent.End(0),
                TickEvent.Begin(1),
                TickEvent.End(1),
        )
    }

    @Test
    fun testThree() {
        testObserver.assertEmpty()

        enqueue()
        enqueue()
        enqueue()

        Thread.sleep(HALF + DELAY * 2)

        testObserver.assertValues(
                TickEvent.Begin(0),
                TickEvent.End(0),
                TickEvent.Begin(2),
                TickEvent.End(2),
        )
    }

    @Test
    fun testThreeThenDelayed() {
        testObserver.assertEmpty()

        enqueue()
        enqueue()
        enqueue()

        Thread.sleep(HALF + DELAY * 2)

        testObserver.assertValues(
                TickEvent.Begin(0),
                TickEvent.End(0),
                TickEvent.Begin(2),
                TickEvent.End(2),
        )

        enqueue()

        Thread.sleep(HALF + DELAY)

        testObserver.assertValues(
                TickEvent.Begin(0),
                TickEvent.End(0),
                TickEvent.Begin(2),
                TickEvent.End(2),
                TickEvent.Begin(3),
                TickEvent.End(3),
        )
    }

    sealed class TickEvent {

        data class Begin(val ordinal: Int) : TickEvent()
        data class End(val ordinal: Int) : TickEvent()
    }
}