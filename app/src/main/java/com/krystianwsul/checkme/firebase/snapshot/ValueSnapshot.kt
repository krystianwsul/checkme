package com.krystianwsul.checkme.firebase.snapshot

interface ValueSnapshot<T : Any> : Snapshot {

    fun getValue(): T?
}