package com.krystianwsul.checkme.gui.utils

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SavedStateProperty<T : Any>(private val savedStateHandle: SavedStateHandle, private val key: String) :
        ReadWriteProperty<ViewModel, T?> {

    private var value: T? = savedStateHandle[key]

    override fun getValue(thisRef: ViewModel, property: KProperty<*>) = value

    override fun setValue(thisRef: ViewModel, property: KProperty<*>, value: T?) {
        this.value = value
        savedStateHandle[key] = value
    }
}