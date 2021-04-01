package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.Snapshot

open class TestSnapshot : Snapshot {

    override val key: String
        get() = TODO("Not yet implemented")

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override fun <T> getValue(valueType: Class<T>): T? {
        TODO("Not yet implemented")
    }
}