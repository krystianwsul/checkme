package com.krystianwsul.checkme.fcm

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.ticks.Ticker
import io.reactivex.rxjava3.core.BackpressureStrategy

class FcmTickQueue {

    private val queue = PublishRelay.create<Unit>()

    fun subscribe() = queue.toFlowable(BackpressureStrategy.LATEST)
            .flatMapCompletable(
                    { Ticker.tick("MyFirebaseMessagingService", true) },
                    false,
                    1,
            )
            .subscribe()!!

    fun enqueue() = queue.accept(Unit)
}