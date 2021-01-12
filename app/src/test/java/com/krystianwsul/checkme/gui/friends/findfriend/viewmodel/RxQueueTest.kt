package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import io.reactivex.rxjava3.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Test

class RxQueueTest {

    @Test
    fun testQueuedUpEvents() {
        val rxQueue = RxQueue<Int>()

        rxQueue.accept(1)
        rxQueue.accept(2)
        rxQueue.accept(3)

        val testObserver = TestObserver<Int>()
        rxQueue.subscribe(testObserver)

        assertEquals(listOf(1, 2, 3), testObserver.values())
    }

    @Test
    fun testReEmitLast() {
        val rxQueue = RxQueue<Int>()

        rxQueue.accept(1)
        rxQueue.accept(2)
        rxQueue.accept(3)

        val testObserver1 = TestObserver<Int>()
        rxQueue.subscribe(testObserver1)

        assertEquals(listOf(1, 2, 3), testObserver1.values())

        testObserver1.dispose()

        val testObserver2 = TestObserver<Int>()
        rxQueue.subscribe(testObserver2)

        assertEquals(listOf(3), testObserver2.values())
    }
}