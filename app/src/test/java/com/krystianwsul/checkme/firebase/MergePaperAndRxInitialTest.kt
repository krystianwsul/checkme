package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.Before
import org.junit.Test

class MergePaperAndRxInitialTest {

    @Suppress("MoveLambdaOutsideParentheses")
    @Before
    fun before() {

        mockkObject(MyApplication)
        every { MyApplication.context } returns mockk(relaxed = true)

        mockkObject(MyCrashlytics)
        every { MyCrashlytics.logException(any()) } returns Unit
    }

    @Test
    fun testFirebaseBefore() {
        val paperSubject = PublishRelay.create<Int>()
        val firebaseSubject = BehaviorRelay.createDefault(1)

        val testObserver = mergePaperAndRx(paperSubject.firstElement(), firebaseSubject, Converter({ it }, { it })).test()

        testObserver.assertValues(1)

        testObserver.dispose()
    }
}