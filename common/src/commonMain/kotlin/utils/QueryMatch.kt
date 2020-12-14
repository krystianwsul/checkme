package com.krystianwsul.common.utils

interface QueryMatch {

    val normalizedName: String
    val normalizedNote: String?

    fun matchesQuery(queryData: QueryData): Boolean {
        log("magic $this matchesQuery 1")
        if (queryData.query.isEmpty()) return true

        log("magic $this matchesQuery 2")
        if (normalizedName.contains(queryData.query)) return true

        log("magic $this matchesQuery 3")
        if (normalizedNote?.contains(queryData.query) == true) return true

        log("magic $this matchesQuery 4")
        return false
    }
}