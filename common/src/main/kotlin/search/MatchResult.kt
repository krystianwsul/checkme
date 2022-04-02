package search

import com.krystianwsul.common.firebase.models.search.FilterResult

enum class MatchResult(
    val matches: Boolean = false,
    val includeWithoutChildren: Boolean = true,
    val continueSearchingChildren: Boolean = false, // todo taskKey this logic is spread out across a few places.  Clean up after unifying instance filtering.
) {

    NO_SEARCH {

        override fun getFilterResult() = FilterResult.NoSearch("a")
    },

    QUERY_NOMATCH(includeWithoutChildren = false) {

        override fun getFilterResult(): FilterResult? = null
    },

    QUERY_MATCH(matches = true, continueSearchingChildren = true) {

        override fun getFilterResult() = FilterResult.Matches(true)
    },

    TASKKEY_NOMATCH {

        override fun getFilterResult() = FilterResult.Matches(false)
    },

    TASKKEY_MATCH(matches = true) {

        override fun getFilterResult() = FilterResult.Matches(true)
    };

    // null means check children
    abstract fun getFilterResult(): FilterResult?
}