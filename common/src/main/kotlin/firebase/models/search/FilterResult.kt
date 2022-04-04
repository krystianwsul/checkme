package com.krystianwsul.common.firebase.models.search

sealed interface FilterResult {

    val doesntMatch: Boolean

    val matchesSearch get() = false

    object NoSearch : FilterResult {

        override val doesntMatch = false
    }

    object Exclude : FilterResult {

        override val doesntMatch = true
    }

    class Include(override val matchesSearch: Boolean) : FilterResult {

        override val doesntMatch = false
    }
}