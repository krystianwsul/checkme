package com.krystianwsul.common.criteria

import com.krystianwsul.common.utils.TaskKey

interface DomainQueryMatchable {

    val normalizedFields: List<String>

    fun matchesTaskKey(taskKey: TaskKey): Boolean

    fun matchesSearch(search: SearchCriteria.Search?): Boolean {
        return when (search) {
            is SearchCriteria.Search.Query -> {
                if (search.query.isEmpty()) {
                    true
                } else {
                    normalizedFields.any { it.contains(search.query) }
                }
            }
            is SearchCriteria.Search.TaskKey -> matchesTaskKey(search.taskKey)
            null -> true
        }
    }
}