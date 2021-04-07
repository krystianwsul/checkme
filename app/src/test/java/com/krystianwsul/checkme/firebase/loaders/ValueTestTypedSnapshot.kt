package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.common.firebase.json.Parsable

@Suppress("UNCHECKED_CAST")
class ValueTestTypedSnapshot<T : Parsable>(private val value: T, override val key: String) : TypedSnapshot<T> {

    override fun exists() = true

    override fun getValue() = value
}