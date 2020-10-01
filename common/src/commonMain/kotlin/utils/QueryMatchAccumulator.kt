package com.krystianwsul.common.utils

class QueryMatchAccumulator(private val query: String) {

    var hasMore = false
        private set

    fun accumulate(queryMatch: QueryMatch, hasMore: Boolean) {
        if (queryMatch.matchesQuery(query)) {
            this.hasMore = this.hasMore || hasMore
        }
    }
}