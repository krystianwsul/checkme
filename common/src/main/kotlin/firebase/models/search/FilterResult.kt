package com.krystianwsul.common.firebase.models.search

sealed interface FilterResult {

    val doesntMatch: Boolean

    val matchesSearch get() = false

    object DoesntMatch : FilterResult {

        override val doesntMatch = true

        override fun toString() = "FilterResult.DoesntMatch" // todo taskKey
    }

    sealed class Task : FilterResult {

        override val doesntMatch = false

        override fun toString() = "FilterResult.Task" // todo taskKey
    }

    class NoSearch(val source: String) : Task() { // todo taskKey remove source, revert to object


        override fun toString() = "FilterResult.NoSearch source: $source" // todo taskKey
    }

    object Include : Task() {


        override fun toString() = "FilterResult.Include" // todo taskKey
    }

    // todo taskKey this naming is awful
    class Matches(override val matchesSearch: Boolean) : Task() {

        override fun toString() = "FilterResult.Matches matchesSearch: $matchesSearch" // todo taskKey
    }
}