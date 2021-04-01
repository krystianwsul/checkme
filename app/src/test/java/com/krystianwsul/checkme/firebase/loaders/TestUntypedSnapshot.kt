package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot

open class TestUntypedSnapshot : TestSnapshot(), UntypedSnapshot {

    override val children: Iterable<UntypedSnapshot>
        get() = TODO("Not yet implemented")
}