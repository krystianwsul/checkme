package com.krystianwsul.treeadapter

import com.krystianwsul.common.utils.Ordinal

interface Sortable {

    val sortable: Boolean get() = true
    val reversedOrdinal: Boolean get() = false

    fun getOrdinal(): Ordinal
    fun setOrdinal(ordinal: Ordinal)
}