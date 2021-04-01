package com.krystianwsul.checkme.firebase.snapshot

interface ValueSnapshot : Snapshot {

    fun <T> getValue(valueType: Class<T>): T?
}