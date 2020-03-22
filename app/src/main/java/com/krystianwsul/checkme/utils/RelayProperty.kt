package com.krystianwsul.checkme.utils

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class RelayProperty<R, T : Any>(
        @Suppress("UNUSED_PARAMETER") owner: R,
        initialValue: T
) : ReadWriteProperty<R, T> {

    private val relay = BehaviorRelay.createDefault(initialValue)

    val observable = relay as Observable<T>

    var value
        get() = relay.value!!
        set(value) {
            relay.accept(value)
        }

    protected fun mutate(mutator: T.() -> T) {
        value = mutator(value)
    }

    override fun getValue(thisRef: R, property: KProperty<*>) = value

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        this.value = value
    }
}