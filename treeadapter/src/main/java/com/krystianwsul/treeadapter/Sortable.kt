package com.krystianwsul.treeadapter

interface Sortable {

    val sortable: Boolean get() = true
    val reversedOrdinal: Boolean get() = false

    fun getOrdinal(): Double
    fun setOrdinal(ordinal: Double)
}