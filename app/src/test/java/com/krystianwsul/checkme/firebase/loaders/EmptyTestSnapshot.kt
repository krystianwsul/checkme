package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.Snapshot

open class EmptyTestSnapshot(private val _key: String? = null) : Snapshot {

    override val key get() = _key!!

    override fun exists() = false

    override fun <T> getValue(valueType: Class<T>): T? = null
}