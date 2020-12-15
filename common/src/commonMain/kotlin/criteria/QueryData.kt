package com.krystianwsul.common.criteria

interface QueryData {

    val query: String
    val showAssigned: Boolean

    object Empty : QueryData {

        override val query = ""
        override val showAssigned = true
    }
}