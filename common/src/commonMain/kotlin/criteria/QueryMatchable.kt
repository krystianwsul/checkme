package com.krystianwsul.common.criteria

interface QueryMatchable {

    val normalizedFields: List<String>

    fun matchesSearch(search: SearchCriteria.Search?): Boolean {
        val query = search?.query ?: "" // todo expand

        if (query.isEmpty()) return true

        return normalizedFields.any { it.contains(query) }
    }
}