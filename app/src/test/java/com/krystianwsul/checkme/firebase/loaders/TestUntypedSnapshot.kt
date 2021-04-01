package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot

open class TestUntypedSnapshot : UntypedSnapshot {

    override val key: String
        get() = TODO("Not yet implemented")

    override val children: Iterable<UntypedSnapshot>
        get() = TODO("Not yet implemented")

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override fun <T> getValue(valueType: Class<T>): T? {
        TODO("Not yet implemented")
    }
}