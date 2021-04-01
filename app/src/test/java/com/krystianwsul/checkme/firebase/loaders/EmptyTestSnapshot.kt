package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.ValueSnapshot

open class EmptyTestSnapshot(private val _key: String? = null) : ValueSnapshot {

    override val key get() = _key!!

    override fun exists() = false

    override fun <T> getValue(valueType: Class<T>): T? = null
}