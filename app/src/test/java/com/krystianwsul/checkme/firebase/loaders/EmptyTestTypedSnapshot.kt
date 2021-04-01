package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot

open class EmptyTestTypedSnapshot<T : Any>(private val _key: String? = null) : EmptyTestSnapshot(), TypedSnapshot<T>