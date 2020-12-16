package com.krystianwsul.common.criteria

interface QueryMatchable {

    val normalizedFields: List<String>

    fun matchesQuery(query: String): Boolean {
        if (query.isEmpty()) return true

        return normalizedFields.any { it.contains(query) }
    }
}