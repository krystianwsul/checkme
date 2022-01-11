package com.krystianwsul.checkme

import com.krystianwsul.checkme.firebase.loaders.RxErrorChecker
import com.krystianwsul.common.utils.singleOrEmpty
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx3.asObservable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CoroutinesTest {

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    @Before
    fun before() {
        rxErrorChecker = RxErrorChecker()
    }

    @After
    fun after() {
        compositeDisposable.clear()

        rxErrorChecker.check()
    }

    @Test
    fun testSharedFlowAsPublishRelayEmit() {
        var eventPassedThrough = false

        val sharedFlow = MutableSharedFlow<Unit>()

        sharedFlow.asObservable()
            .subscribe {
                check(!eventPassedThrough)

                eventPassedThrough = true
            }
            .addTo(compositeDisposable)

        runBlocking { sharedFlow.emit(Unit) }

        assertTrue(eventPassedThrough)
    }

    @Test
    fun testSharedFlowAsPublishRelayTryEmit() {
        var eventPassedThrough = false

        val sharedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        sharedFlow.asObservable()
            .subscribe {
                check(!eventPassedThrough)

                eventPassedThrough = true
            }
            .addTo(compositeDisposable)

        sharedFlow.tryEmit(Unit)

        assertTrue(eventPassedThrough)
    }

    @Test
    fun testSharedFlowAsBehaviorRelay() {
        val sharedFlow = MutableSharedFlow<Int>(replay = 1)

        var lastEmission = 0

        sharedFlow.asObservable()
            .subscribe { lastEmission = it }
            .addTo(compositeDisposable)

        sharedFlow.tryEmit(1)
        assertEquals(1, lastEmission)

        sharedFlow.tryEmit(2)
        assertEquals(2, lastEmission)

        var newLastEmission = 0

        val disposable = sharedFlow.asObservable().subscribe {
            check(newLastEmission == 0)
            newLastEmission = it
        }

        assertEquals(2, newLastEmission)
        disposable.dispose()

        assertEquals(2, sharedFlow.replayCache.singleOrEmpty())
    }
}