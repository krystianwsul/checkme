package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.interrupt.throwIfInterrupted
import com.krystianwsul.common.utils.ProjectType

fun <T : ProjectType> Sequence<Task<out T>>.filterQuery(query: String?) = query?.let {
    fun filterQuery(task: Task<out T>): FilterResult {
        throwIfInterrupted()

        if (task.matchesQuery(it)) return FilterResult.MATCHES

        if (task.childHierarchyIntervals.any { filterQuery(it.taskHierarchy.childTask) != FilterResult.DOESNT_MATCH })
            return FilterResult.CHILD_MATCHES

        return FilterResult.DOESNT_MATCH
    }

    map { it to filterQuery(it) }.filter { it.second != FilterResult.DOESNT_MATCH }
} ?: map { it to FilterResult.MATCHES }

enum class FilterResult {

    DOESNT_MATCH, CHILD_MATCHES, MATCHES
}