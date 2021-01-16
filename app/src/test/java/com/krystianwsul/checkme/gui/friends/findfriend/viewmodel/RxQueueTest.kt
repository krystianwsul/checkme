package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import io.reactivex.rxjava3.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Test

class RxQueueTest {

    @Test
    fun testRegularEvents() {
        val rxQueue = RxQueue<Int>()

        val testObserver = TestObserver<Int>()
        rxQueue.subscribe(testObserver)

        rxQueue.accept(1)
        rxQueue.accept(2)
        rxQueue.accept(3)

        assertEquals(listOf(1, 2, 3), testObserver.values())
    }

    @Test
    fun testMultipleObservers() {
        val rxQueue = RxQueue<Int>()

        rxQueue.subscribe(TestObserver())

        val testObserver = TestObserver<Int>()
        rxQueue.subscribe(testObserver)

        testObserver.assertError(IllegalStateException::class.java)
    }

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
        testObserver2.dispose()

        val testObserver3 = TestObserver<Int>()
        rxQueue.subscribe(testObserver3)
        assertEquals(listOf(3), testObserver3.values())
        testObserver3.dispose()

        rxQueue.accept(4)
        rxQueue.accept(5)

        val testObserver4 = TestObserver<Int>()
        rxQueue.subscribe(testObserver4)
        assertEquals(listOf(3, 4, 5), testObserver4.values())
    }
}