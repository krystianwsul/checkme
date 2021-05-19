package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Test

class MergePaperAndRxTest {

    private lateinit var paperSubject: PublishRelay<Int>
    private lateinit var firebaseSubject: PublishRelay<Int>

    private lateinit var testObserver: TestObserver<Int>

    @Suppress("MoveLambdaOutsideParentheses")
    @Before
    fun before() {
        paperSubject = PublishRelay.create()
        firebaseSubject = PublishRelay.create()

        testObserver = mergePaperAndRx(
            paperSubject.firstElement(),
            firebaseSubject,
            Converter({ it }, { it }, true),
            "",
        ).test()

        mockkObject(MyApplication)
        every { MyApplication.context } returns mockk(relaxed = true)

        mockkObject(MyCrashlytics)
        every { MyCrashlytics.logException(any()) } returns Unit
    }

    @After
    fun after() {
        testObserver.dispose()
    }

    @Test
    fun testFirebaseOnly() {
        firebaseSubject.accept(1)
        testObserver.assertValues(1)

        firebaseSubject.accept(2)
        testObserver.assertValues(1, 2)

        firebaseSubject.accept(3)
        testObserver.assertValues(1, 2, 3)

        verify(exactly = 0) { MyCrashlytics.logException(any()) }
    }

    @Test
    fun testFirebaseFirst() {
        firebaseSubject.accept(1)
        testObserver.assertValues(1)

        paperSubject.accept(10)

        firebaseSubject.accept(2)
        testObserver.assertValues(1, 2)

        firebaseSubject.accept(3)
        testObserver.assertValues(1, 2, 3)
    }

    @Test
    fun testPaperOnly() {
        paperSubject.accept(1)
        testObserver.assertValues(1)

        verify(exactly = 0) { MyCrashlytics.logException(any()) }
    }

    @Test
    fun testPaperFirstSameVal() {
        paperSubject.accept(1)
        testObserver.assertValues(1)

        firebaseSubject.accept(1)
        testObserver.assertValues(1)

        firebaseSubject.accept(2)
        testObserver.assertValues(1, 2)

        firebaseSubject.accept(3)
        testObserver.assertValues(1, 2, 3)

        verify(exactly = 0) { MyCrashlytics.logException(any()) }
    }

    @Test
    fun testPaperFirstDifferentVal() {
        paperSubject.accept(1)
        testObserver.assertValues(1)

        firebaseSubject.accept(2)
        testObserver.assertValues(1, 2)
        verify(exactly = 1) { MyCrashlytics.logException(any()) }

        firebaseSubject.accept(3)
        testObserver.assertValues(1, 2, 3)
    }
}