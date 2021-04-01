package com.krystianwsul.checkme.firebase.loaders

@Suppress("UNCHECKED_CAST")
open class ValueTestSnapshot(private val value: Any, override val key: String) : TestSnapshot() {

    override fun exists() = true

    override fun <T> getValue(valueType: Class<T>) = value as T
}