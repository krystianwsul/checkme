package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.GenericTypeIndicator

@Suppress("UNCHECKED_CAST")
class ValueTestSnapshot(private val value: Any, override val key: String) : TestSnapshot() {

    override fun exists() = true

    override fun <T> getValue(valueType: Class<T>) = value as T

    override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>) = value as T
}