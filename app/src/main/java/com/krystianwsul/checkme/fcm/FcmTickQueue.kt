package com.krystianwsul.checkme.fcm

import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable

class FcmTickQueue<T : Any>(private val completable: (T) -> Completable) {

    private val queue = PublishRelay.create<T>()

    fun subscribe() = queue.toFlowable(BackpressureStrategy.LATEST)
            .flatMapCompletable(completable, false, 1)
            .subscribe()!!

    fun enqueue(value: T) = queue.accept(value)
}