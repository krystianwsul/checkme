package com.krystianwsul.checkme.utils

import com.jakewharton.rxrelay3.PublishRelay
import com.jakewharton.rxrelay3.Relay
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable
import kotlin.properties.Delegates.observable

class SingleCacheRelay<T : Any>(private val callback: ((T?) -> Unit)? = null) : Relay<T>() {

    var value by observable<T?>(null) { _, _, newValue -> callback?.invoke(newValue) }
        private set

    private val relay = PublishRelay.create<T>()

    override fun accept(value: T) {
        check(this.value == null)

        if (relay.hasObservers()) {
            relay.accept(value)
        } else {
            this.value = value
        }
    }

    override fun hasObservers() = relay.hasObservers()

    override fun subscribeActual(observer: Observer<in T>) {
        if (hasObservers()) {
            EmptyDisposable.error(IllegalStateException("Only a single observer at a time allowed."), observer)
        } else {
            relay.subscribe(observer)

            value?.let {
                value = null
                relay.accept(it)
            }
        }
    }
}