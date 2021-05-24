package com.krystianwsul.treeadapter

interface Matchable {

    fun normalize() = Unit

    fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) = true

    fun getMatchResult(query: String) = ModelNode.MatchResult.ALWAYS_VISIBLE
}