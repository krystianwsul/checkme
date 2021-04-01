package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot

@Suppress("UNCHECKED_CAST")
class ValueTestUntypedSnapshot(private val value: Any, override val key: String) :
        ValueTestSnapshot(value, key), UntypedSnapshot {

    override fun exists() = true

    override val children: Iterable<UntypedSnapshot>
        get() = TODO("Not yet implemented")

    override fun <T> getValue(valueType: Class<T>) = value as T
}