package com.krystianwsul.common.criteria

import com.krystianwsul.common.firebase.models.FilterResult
import com.krystianwsul.common.utils.TaskKey

interface DomainQueryMatchable {

    val normalizedFields: List<String>

    fun matchesTaskKey(taskKey: TaskKey): Boolean

    fun getFilterResult(search: SearchCriteria.Search?): FilterResult {
        fun Boolean.toFilterResult() = if (this) FilterResult.MATCHES else FilterResult.DOESNT_MATCH

        return when (search) {
            is SearchCriteria.Search.Query -> {
                if (search.query.isEmpty()) {
                    FilterResult.NO_SEARCH
                } else {
                    normalizedFields.any { it.contains(search.query) }.toFilterResult()
                }
            }
            is SearchCriteria.Search.TaskKey -> matchesTaskKey(search.taskKey).toFilterResult()
            null -> FilterResult.NO_SEARCH
        }
    }
}