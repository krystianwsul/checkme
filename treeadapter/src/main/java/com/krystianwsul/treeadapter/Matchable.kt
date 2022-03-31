package com.krystianwsul.treeadapter

import com.krystianwsul.common.criteria.SearchCriteria

interface Matchable { // todo optimization delete

    fun normalize() = Unit

    fun getMatchResult(search: SearchCriteria.Search) = ModelNode.MatchResult.ALWAYS_VISIBLE
}