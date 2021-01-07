package com.krystianwsul.checkme.utils

import com.jakewharton.rxrelay3.BehaviorRelay
import io.reactivex.rxjava3.core.Observable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class NonNullRelayProperty<T : Any>(
        initialValue: T,
        private val beforeSet: ((T) -> Unit)? = null,
) : ReadWriteProperty<Any, T> {

    private val relay = BehaviorRelay.createDefault(initialValue)

    val observable = relay as Observable<T>

    var value
        get() = relay.value!!
        set(value) {
            beforeSet?.invoke(value)
            relay.accept(value)
        }

    fun mutate(mutator: (T) -> T) {
        value = mutator(value)
    }

    override fun getValue(thisRef: Any, property: KProperty<*>) = value

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }
}