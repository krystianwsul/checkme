package com.krystianwsul.common.firebase.models.search

import com.krystianwsul.common.criteria.DomainQueryMatchable
import com.krystianwsul.common.criteria.SearchCriteria

class SearchContext(val searchCriteria: SearchCriteria) {

    fun getChildrenSearchContext(filterResult: FilterResult) = filterResult.getChildrenSearchContext(this)

    fun getChildrenSearchContext(matchResult: DomainQueryMatchable.MatchResult) = matchResult.getChildrenSearchContext(this)
}