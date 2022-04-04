package com.krystianwsul.common.firebase.models.search

sealed interface FilterResult {

    val include: Boolean get() = true

    val matchesSearch get() = false

    object NoSearch : FilterResult

    object Exclude : FilterResult {

        override val include = false
    }

    class Include(override val matchesSearch: Boolean) : FilterResult
}