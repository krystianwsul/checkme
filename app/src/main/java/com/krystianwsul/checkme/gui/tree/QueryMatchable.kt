package com.krystianwsul.checkme.gui.tree

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.utils.TaskKey

interface QueryMatchable { // todo optimization delete

    val normalizedFields: List<String>

    fun matchesTaskKey(taskKey: TaskKey): Boolean = throw UnsupportedOperationException()

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