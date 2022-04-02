package com.krystianwsul.common.firebase.models.search

import com.krystianwsul.common.criteria.SearchCriteria
import search.MatchResult

class SearchContext private constructor(val searchCriteria: SearchCriteria) {

    companion object {

        fun startSearch(searchCriteria: SearchCriteria) = SearchContext(searchCriteria)
    }

    fun getChildrenSearchContext(filterResult: FilterResult) = when (filterResult) {
        FilterResult.DoesntMatch -> this
        is FilterResult.NoSearch -> this
        FilterResult.Include -> this
        is FilterResult.Matches -> if (filterResult.matchesSearch) SearchContext(searchCriteria.clear()) else this
    }

    fun getChildrenSearchContext(matchResult: MatchResult) =
        if (matchResult.matches) SearchContext(searchCriteria.clear()) else this
}