package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.jakewharton.rxrelay3.PublishRelay
import com.jakewharton.rxrelay3.Relay
import com.jakewharton.rxrelay3.ReplayRelay
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.ConcurrentLinkedQueue

class RxQueue<T : Any> : Relay<T>() {

    private val queue = ConcurrentLinkedQueue<T>()

    private val relay = PublishRelay.create<T>()

    override fun accept(value: T) {
        val replayRelay = ReplayRelay.create<T>()

        replayRelay.cleanupBuffer()

        if (relay.hasObservers()) {
            when (queue.size) {
                0 -> queue.add(value)
                1 -> {
                    queue.clear()
                    queue.add(value)
                }
                else -> throw IllegalStateException()
            }

            relay.accept(value)
        } else {
            queue.add(value)
        }
    }

    override fun hasObservers() = relay.hasObservers()

    override fun subscribeActual(observer: Observer<in T>) {
        if (relay.hasObservers()) {
            observer.onSubscribe(Disposable.empty())
            observer.onError(IllegalStateException())
        } else {
            relay.subscribe(observer)

            repeat((0 until queue.size - 1).count()) { relay.accept(queue.poll()) }
            if (queue.isNotEmpty()) relay.accept(queue.peek())
        }
    }
}