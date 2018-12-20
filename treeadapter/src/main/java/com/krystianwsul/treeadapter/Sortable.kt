package com.krystianwsul.treeadapter

interface Sortable {

    fun getOrdinal(): Double = throw UnsupportedOperationException()

    fun setOrdinal(ordinal: Double): Unit = throw UnsupportedOperationException()
}