package com.krystianwsul.common.criteria

import com.krystianwsul.common.utils.TaskKey
import search.MatchResult

interface DomainQueryMatchable {

    val normalizedFields: List<String>

    fun matchesTaskKey(taskKey: TaskKey): Boolean

    fun getMatchResult(search: SearchCriteria.Search): MatchResult {
        return when (search) {
            is SearchCriteria.Search.Query -> {
                when {
                    search.isEmpty -> MatchResult.NO_SEARCH
                    normalizedFields.any { it.contains(search.query) } -> MatchResult.QUERY_MATCH
                    else -> MatchResult.QUERY_NOMATCH
                }
            }
            is SearchCriteria.Search.TaskKey ->
                if (matchesTaskKey(search.taskKey)) MatchResult.TASKKEY_MATCH else MatchResult.TASKKEY_NOMATCH
        }
    }
}