package com.krystianwsul.checkme.firebase.snapshot

interface Snapshot {

    val key: String

    fun exists(): Boolean
}