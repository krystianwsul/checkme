package com.krystianwsul.common.firebase.models.search

import com.krystianwsul.common.criteria.DomainQueryMatchable
import com.krystianwsul.common.criteria.SearchCriteria

class SearchContext(val searchCriteria: SearchCriteria) {

    fun getChildrenSearchContext(filterResult: FilterResult) = when (filterResult) {
        FilterResult.DoesntMatch -> this
        is FilterResult.NoSearch -> this
        FilterResult.Include -> this
        is FilterResult.Matches -> if (filterResult.matchesSearch) SearchContext(searchCriteria.clear()) else this
    }

    fun getChildrenSearchContext(matchResult: DomainQueryMatchable.MatchResult) = matchResult.getChildrenSearchContext(this)
}