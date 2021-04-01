package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Test

class UtilsKtTest {

    private lateinit var paperSubject: PublishRelay<Int>
    private lateinit var firebaseSubject: PublishRelay<Int>

    private lateinit var testObserver: TestObserver<Int>

    @Before
    fun before() {
        paperSubject = PublishRelay.create()
        firebaseSubject = PublishRelay.create()

        testObserver = mergePaperAndRx(
                paperSubject.firstElement(),
                firebaseSubject,
                { it },
                { paper, firebase -> paper == firebase },
        ).test()
    }

    @After
    fun after() {
        testObserver.dispose()
    }

    @Test
    fun testFirebaseOnly() {
        firebaseSubject.accept(1)
        firebaseSubject.accept(2)
        firebaseSubject.accept(3)

        testObserver.assertValues(1, 2, 3)
    }

    @Test
    fun testFirebaseFirst() {
        firebaseSubject.accept(1)

        paperSubject.accept(10)

        firebaseSubject.accept(2)
        firebaseSubject.accept(3)

        testObserver.assertValues(1, 2, 3)
    }

    @Test
    fun testPaperOnly() {
        paperSubject.accept(1)

        testObserver.assertValues(1)
    }

    @Test
    fun testPaperFirstSameVal() {
        paperSubject.accept(1)

        firebaseSubject.accept(1)
        firebaseSubject.accept(2)
        firebaseSubject.accept(3)

        testObserver.assertValues(1, 2, 3)
    }

    @Test
    fun testPaperFirstDifferentVal() {
        paperSubject.accept(1)

        firebaseSubject.accept(2)
        firebaseSubject.accept(3)

        testObserver.assertValues(1, 2, 3)
    }
}