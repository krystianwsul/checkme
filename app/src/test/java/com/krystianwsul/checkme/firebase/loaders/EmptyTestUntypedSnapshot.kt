package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot

open class EmptyTestUntypedSnapshot(private val _key: String? = null) : EmptyTestSnapshot(), UntypedSnapshot {

    override val children = listOf<UntypedSnapshot>()
}