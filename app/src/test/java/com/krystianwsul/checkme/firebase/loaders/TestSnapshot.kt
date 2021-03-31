package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.checkme.firebase.loaders.snapshot.Snapshot

open class TestSnapshot : Snapshot {

    override val key: String
        get() = TODO("Not yet implemented")

    override val children: Iterable<Snapshot>
        get() = TODO("Not yet implemented")

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override fun <T> getValue(valueType: Class<T>): T? {
        TODO("Not yet implemented")
    }

    override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>): T? {
        TODO("Not yet implemented")
    }
}