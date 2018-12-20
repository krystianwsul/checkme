package com.krystianwsul.treeadapter

interface Movable {

    fun getOrdinal(): Double = throw UnsupportedOperationException()

    fun setOrdinal(ordinal: Double): Unit = throw UnsupportedOperationException()
}