package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.IndicatorSnapshot

@Suppress("UNCHECKED_CAST")
class ValueTestIndicatorSnapshot<T : Any>(private val value: T, override val key: String) : IndicatorSnapshot<T> {

    override fun exists() = true

    override fun getValue() = value
}