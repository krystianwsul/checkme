package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot

open class TestUntypedSnapshot : TestSnapshot(), UntypedSnapshot {

    override val children: Iterable<UntypedSnapshot>
        get() = TODO("Not yet implemented")

    override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>): T? {
        TODO("Not yet implemented")
    }
}