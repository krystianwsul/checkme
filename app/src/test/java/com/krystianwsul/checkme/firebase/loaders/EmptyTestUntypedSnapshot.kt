package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot

open class EmptyTestUntypedSnapshot(private val _key: String? = null) : EmptyTestSnapshot(), UntypedSnapshot {

    override val children = listOf<UntypedSnapshot>()

    override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>): T? = null
}