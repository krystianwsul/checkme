package com.krystianwsul.common.utils

interface QueryData {

    val query: String

    val hasQuery: Boolean

    object Empty : QueryData {

        override val query = ""

        override val hasQuery = false
    }
}