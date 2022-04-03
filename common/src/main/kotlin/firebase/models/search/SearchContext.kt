package com.krystianwsul.common.firebase.models.search

import com.krystianwsul.common.criteria.SearchCriteria
import search.MatchResult

class SearchContext private constructor(val searchCriteria: SearchCriteria, val searchingChildrenOfQueryMatch: Boolean) {

    companion object {

        fun startSearch(searchCriteria: SearchCriteria) = SearchContext(searchCriteria, false)
    }

    fun getChildrenSearchContext(filterResult: FilterResult) = when (filterResult) {
        FilterResult.Exclude -> this
        is FilterResult.NoSearch -> this
        is FilterResult.Include -> if (filterResult.matchesSearch) {
            when (searchCriteria.search!!) {
                is SearchCriteria.Search.Query -> SearchContext(searchCriteria, true)
                is SearchCriteria.Search.TaskKey -> SearchContext(searchCriteria.clear(), false)
            }
        } else {
            this
        }
    }

    fun getChildrenSearchContext(matchResult: MatchResult) =
        if (matchResult.matches) {
            if (matchResult.continueSearchingChildren) {
                SearchContext(searchCriteria, true)
            } else {
                SearchContext(searchCriteria.clear(), false)
            }
        } else {
            this
        }

    // todo taskKey
    override fun toString() =
        "SearchContext searchingChildrenOfQueryMatch: $searchingChildrenOfQueryMatch, criteria: $searchCriteria"
}