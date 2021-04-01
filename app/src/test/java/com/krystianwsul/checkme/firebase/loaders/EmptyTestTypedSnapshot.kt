package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot

open class EmptyTestTypedSnapshot<T : Any>(private val _key: String? = null) : TypedSnapshot<T> {

    override val key get() = _key!!

    override fun exists() = false

    override fun <T> getValue(valueType: Class<T>): T? = null
}