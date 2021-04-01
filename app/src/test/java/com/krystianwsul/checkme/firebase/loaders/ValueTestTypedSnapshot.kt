package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot

@Suppress("UNCHECKED_CAST")
class ValueTestTypedSnapshot<T : Any>(private val value: T, override val key: String) : TypedSnapshot<T> {

    override fun exists() = true

    override fun <T> getValue(valueType: Class<T>) = value as T
}