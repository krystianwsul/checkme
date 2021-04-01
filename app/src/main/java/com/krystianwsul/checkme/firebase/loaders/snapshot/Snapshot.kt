package com.krystianwsul.checkme.firebase.loaders.snapshot

interface Snapshot {

    val key: String

    fun exists(): Boolean

    fun <T> getValue(valueType: Class<T>): T?
}