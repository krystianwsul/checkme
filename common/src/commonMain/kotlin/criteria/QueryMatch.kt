package com.krystianwsul.common.criteria

interface QueryMatch {

    val normalizedName: String
    val normalizedNote: String?

    fun matchesQuery(query: String): Boolean {
        if (query.isEmpty()) return true

        if (normalizedName.contains(query)) return true
        if (normalizedNote?.contains(query) == true) return true

        return false
    }
}