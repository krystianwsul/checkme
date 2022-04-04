package search

import com.krystianwsul.common.firebase.models.search.FilterResult

enum class MatchResult(val matches: Boolean = false) {

    NO_SEARCH {

        override fun getFilterResult() = FilterResult.NoSearch
    },

    QUERY_NOMATCH {

        override fun getFilterResult(): FilterResult? = null
    },

    QUERY_MATCH(matches = true) {

        override fun getFilterResult() = FilterResult.Include(true)
    },

    TASKKEY_NOMATCH {

        override fun getFilterResult() = FilterResult.Include(false)
    },

    TASKKEY_MATCH(matches = true) {

        override fun getFilterResult() = FilterResult.Include(true)
    };

    // null means check children
    abstract fun getFilterResult(): FilterResult?
}