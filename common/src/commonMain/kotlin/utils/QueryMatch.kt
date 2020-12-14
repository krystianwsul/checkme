package com.krystianwsul.common.utils

interface QueryMatch {

    val normalizedName: String
    val normalizedNote: String?

    fun matchesQuery(queryData: QueryData): Boolean {
        if (queryData.query.isEmpty()) return true

        if (normalizedName.contains(queryData.query)) return true

        if (normalizedNote?.contains(queryData.query) == true) return true

        return false
    }
}