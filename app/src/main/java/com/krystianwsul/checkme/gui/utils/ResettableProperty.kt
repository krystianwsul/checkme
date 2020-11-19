package com.krystianwsul.checkme.gui.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ResettableProperty<T : Any> : ReadWriteProperty<Any, T> {

    var value: T? = null

    override fun getValue(thisRef: Any, property: KProperty<*>) = value!!

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun reset() {
        value = null
    }
}