package com.krystianwsul.checkme.utils

import com.jakewharton.rxrelay3.Relay
import com.victorrendina.rxqueue2.QueueRelay
import io.reactivex.rxjava3.core.Observer

class RxQueue<T : Any>(initialValue: T) : Relay<T>() {

    private val queueRelay = QueueRelay.createDefault(initialValue)

    var value = initialValue
        private set

    override fun accept(value: T) {
        this.value = value
        queueRelay.accept(value)
    }

    override fun hasObservers() = queueRelay.hasObservers()

    override fun subscribeActual(observer: Observer<in T>) = queueRelay.toV3().subscribe(observer)
}