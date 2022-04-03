package com.krystianwsul.common.firebase.models.search

sealed interface FilterResult {

    val doesntMatch: Boolean

    val matchesSearch get() = false

    object DoesntMatch : FilterResult {

        override val doesntMatch = true

        override fun toString() = "FilterResult.DoesntMatch" // todo taskKey
    }

    class NoSearch(val source: String) : FilterResult { // todo taskKey remove source, revert to object

        override val doesntMatch = false

        override fun toString() = "FilterResult.NoSearch source: $source" // todo taskKey
    }

    object Include : FilterResult {

        override val doesntMatch = false

        override fun toString() = "FilterResult.Include" // todo taskKey
    }

    // todo taskKey this naming is awful
    class Matches(override val matchesSearch: Boolean) : FilterResult {

        override val doesntMatch = false

        override fun toString() = "FilterResult.Matches matchesSearch: $matchesSearch" // todo taskKey
    }
}