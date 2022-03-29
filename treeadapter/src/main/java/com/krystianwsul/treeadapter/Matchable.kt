package com.krystianwsul.treeadapter

import com.krystianwsul.common.criteria.SearchCriteria

interface Matchable {

    fun normalize() = Unit

    fun getMatchResult(search: SearchCriteria.Search) = ModelNode.MatchResult.ALWAYS_VISIBLE
}