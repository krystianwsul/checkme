package com.krystianwsul.checkme.firebase.snapshot

interface Snapshot<T : Any> {

    val key: String

    fun exists(): Boolean

    fun getValue(): T?
}