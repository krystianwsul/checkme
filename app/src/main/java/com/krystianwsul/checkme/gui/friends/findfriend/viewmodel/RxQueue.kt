package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.jakewharton.rxrelay3.Relay
import com.krystianwsul.checkme.utils.toV3
import com.victorrendina.rxqueue2.QueueRelay
import io.reactivex.rxjava3.core.Observer

class RxQueue<T : Any> : Relay<T>() {

    private val queueRelay = QueueRelay.create<T>()

    override fun accept(value: T) = queueRelay.accept(value)

    override fun hasObservers() = queueRelay.hasObservers()

    override fun subscribeActual(observer: Observer<in T>) {
        queueRelay.toV3().subscribe(observer)
    }
}