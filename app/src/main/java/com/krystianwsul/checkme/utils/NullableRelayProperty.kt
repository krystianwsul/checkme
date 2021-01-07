package com.krystianwsul.checkme.utils

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.rxjava3.core.Observable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class NullableRelayProperty<T : Any>(initialValue: T?, private val beforeSet: ((T?) -> Unit)? = null) : ReadWriteProperty<Any, T?> {

    private val relay = BehaviorRelay.createDefault(NullableWrapper(initialValue))

    val observable = relay as Observable<NullableWrapper<T>>

    var value
        get() = relay.value!!.value
        set(value) {
            if (value == relay.value!!) return

            beforeSet?.invoke(value)
            relay.accept(NullableWrapper(value))
        }

    override fun getValue(thisRef: Any, property: KProperty<*>) = value

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        this.value = value
    }
}