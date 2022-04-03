package com.krystianwsul.common.firebase.models.search

sealed interface FilterResult {

    val doesntMatch: Boolean

    val matchesSearch get() = false

    class NoSearch(val source: String) : FilterResult { // todo taskKey remove source, revert to object

        override val doesntMatch = false

        override fun toString() = "FilterResult.NoSearch source: $source" // todo taskKey
    }

    object Exclude : FilterResult {

        override val doesntMatch = true

        override fun toString() = "FilterResult.DoesntMatch" // todo taskKey
    }

    // todo taskKey this naming is awful
    class Include(override val matchesSearch: Boolean) : FilterResult {

        override val doesntMatch = false

        override fun toString() = "FilterResult.Matches matchesSearch: $matchesSearch" // todo taskKey
    }
}