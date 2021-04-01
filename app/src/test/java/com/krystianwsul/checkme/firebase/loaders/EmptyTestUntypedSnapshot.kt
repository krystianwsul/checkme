package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot

open class EmptyTestUntypedSnapshot(private val _key: String? = null) : UntypedSnapshot {

    override val key get() = _key!!

    override val children = listOf<UntypedSnapshot>()

    override fun exists() = false

    override fun <T> getValue(valueType: Class<T>): T? = null
}