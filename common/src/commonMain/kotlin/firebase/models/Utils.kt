package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.interrupt.throwIfInterrupted
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

fun <T : ProjectType> Sequence<Task<out T>>.filterQuery(query: String?) = if (query.isNullOrEmpty()) {
    map { it to FilterResult.MATCHES }
} else {
    fun childHierarchyMatches(task: Task<out T>): FilterResult {
        throwIfInterrupted()
        if (task.matchesQuery(query)) return FilterResult.MATCHES

        throwIfInterrupted()

        if (
                task.childHierarchyIntervals.any {
                    childHierarchyMatches(it.taskHierarchy.childTask) != FilterResult.DOESNT_MATCH
                }
        ) {
            return FilterResult.CHILD_MATCHES
        }

        return FilterResult.DOESNT_MATCH
    }

    map { it to childHierarchyMatches(it) }.filter { it.second != FilterResult.DOESNT_MATCH }
}

fun <T : ProjectType> Sequence<Instance<out T>>.filterSearchCriteria(
        searchCriteria: SearchCriteria,
        now: ExactTimeStamp.Local,
        myUser: MyUser,
) = if (searchCriteria.isEmpty) {
    this
} else {
    fun childHierarchyMatches(instance: Instance<out T>): Boolean {
        throwIfInterrupted()

        if (!searchCriteria.showAssignedToOthers && !instance.isAssignedToMe(now, myUser)) return false

        if (instance.task.matchesQuery(searchCriteria.query)) return true
        return instance.getChildInstances(now).any { childHierarchyMatches(it.first) }
    }

    filter(::childHierarchyMatches)
}

enum class FilterResult {

    DOESNT_MATCH, CHILD_MATCHES, MATCHES
}