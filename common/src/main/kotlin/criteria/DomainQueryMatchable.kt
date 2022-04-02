package com.krystianwsul.common.criteria

import com.krystianwsul.common.firebase.models.search.FilterResult
import com.krystianwsul.common.firebase.models.search.SearchContext
import com.krystianwsul.common.utils.TaskKey

interface DomainQueryMatchable {

    val normalizedFields: List<String>

    fun matchesTaskKey(taskKey: TaskKey): Boolean

    // todo optimization check calls where FilterResult was already obtained earlier, like for instances
    fun getMatchResult(search: SearchCriteria.Search?): MatchResult {
        return when (search) {
            is SearchCriteria.Search.Query -> {
                when {
                    search.query.isEmpty() -> MatchResult.NO_SEARCH
                    normalizedFields.any { it.contains(search.query) } -> MatchResult.QUERY_MATCH
                    else -> MatchResult.QUERY_NOMATCH
                }
            }
            is SearchCriteria.Search.TaskKey ->
                if (matchesTaskKey(search.taskKey)) MatchResult.TASKKEY_MATCH else MatchResult.TASKKEY_NOMATCH
            null -> MatchResult.NO_SEARCH
        }
    }

    enum class MatchResult(val matches: Boolean = false, val includeWithoutChildren: Boolean = true) {

        NO_SEARCH {

            override fun getFilterResult() = FilterResult.NoSearch("a")
        },

        QUERY_NOMATCH(includeWithoutChildren = false) {

            override fun getFilterResult(): FilterResult? = null
        },

        QUERY_MATCH(matches = true) {

            override fun getFilterResult() = FilterResult.Matches(true)
        },

        TASKKEY_NOMATCH {

            override fun getFilterResult() = FilterResult.Matches(false)
        },

        TASKKEY_MATCH(matches = true) {

            override fun getFilterResult() = FilterResult.Matches(true)
        };

        fun getChildrenSearchContext(searchContext: SearchContext) =
            if (matches) SearchContext(searchContext.searchCriteria.clear()) else searchContext

        // null means check children
        abstract fun getFilterResult(): FilterResult?
    }
}