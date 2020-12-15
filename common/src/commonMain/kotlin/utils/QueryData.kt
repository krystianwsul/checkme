package com.krystianwsul.common.utils

interface QueryData {

    val query: String
    val showAssigned: Boolean

    object Empty : QueryData {

        override val query = ""
        override val showAssigned = true
    }
}