package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.checkme.firebase.snapshot.IndicatorSnapshot

open class EmptyTestIndicatorSnapshot<T : Any>(private val _key: String? = null) : EmptyTestSnapshot(),
        IndicatorSnapshot<T> {

    override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>): T? = null
}