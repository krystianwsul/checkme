package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.checkme.firebase.loaders.snapshot.Snapshot

open class EmptyTestSnapshot(private val _key: String? = null) : Snapshot {

    override val key get() = _key!!

    override val children = listOf<Snapshot>()

    override fun exists() = false

    override fun <T> getValue(valueType: Class<T>): T? = null

    override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>): T? = null
}