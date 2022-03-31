package com.krystianwsul.common.criteria

import com.krystianwsul.common.firebase.models.FilterResult
import com.krystianwsul.common.utils.TaskKey

interface DomainQueryMatchable {

    val normalizedFields: List<String>

    fun matchesTaskKey(taskKey: TaskKey): Boolean

    // todo optimization check calls where FilterResult was already obtained earlier, like for instances
    fun getFilterResult(search: SearchCriteria.Search?): FilterResult.Task {
        fun Boolean.toFilterResult() = if (this) FilterResult.Matches else FilterResult.Include

        return when (search) {
            is SearchCriteria.Search.Query -> {
                if (search.query.isEmpty()) {
                    FilterResult.NoSearch
                } else {
                    normalizedFields.any { it.contains(search.query) }.toFilterResult()
                }
            }
            is SearchCriteria.Search.TaskKey -> matchesTaskKey(search.taskKey).toFilterResult()
            null -> FilterResult.NoSearch
        }
    }
}