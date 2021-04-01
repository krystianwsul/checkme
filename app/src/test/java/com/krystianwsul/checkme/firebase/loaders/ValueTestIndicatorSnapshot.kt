package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.checkme.firebase.snapshot.IndicatorSnapshot

@Suppress("UNCHECKED_CAST")
class ValueTestIndicatorSnapshot<T : Any>(private val value: T, override val key: String) :
        ValueTestSnapshot(value, key), IndicatorSnapshot<T> {

    override fun exists() = true

    override fun <T> getValue(valueType: Class<T>) = value as T

    override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>) = value as T
}