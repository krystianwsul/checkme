package com.krystianwsul.treeadapter

interface Sortable {

    val sortable: Boolean get() = true

    fun getOrdinal(): Double

    fun setOrdinal(ordinal: Double)
}