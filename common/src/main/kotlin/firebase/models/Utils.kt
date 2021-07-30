package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.ExactTimeStamp

fun Sequence<Task>.filterSearch(search: SearchCriteria.Search?) = if (search?.hasSearch != true) {
    map { it to FilterResult.MATCHES }
} else {
    fun childHierarchyMatches(task: Task): FilterResult {
        InterruptionChecker.throwIfInterrupted()

        if (task.matchesSearch(search)) return FilterResult.MATCHES

        InterruptionChecker.throwIfInterrupted()

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

fun Sequence<Instance>.filterSearchCriteria(
        searchCriteria: SearchCriteria,
        now: ExactTimeStamp.Local,
        myUser: MyUser,
) = if (searchCriteria.isEmpty) {
    this
} else {
    fun childHierarchyMatches(instance: Instance): Boolean {
        InterruptionChecker.throwIfInterrupted()

        if (!searchCriteria.showAssignedToOthers && !instance.isAssignedToMe(now, myUser)) return false

        if (!searchCriteria.showDone && instance.done != null) return false

        if (instance.instanceKey in searchCriteria.excludedInstanceKeys) return false

        if (instance.task.matchesSearch(searchCriteria.search)) return true

        return instance.getChildInstances()
            .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true)) }
            .any(::childHierarchyMatches)
    }

    filter(::childHierarchyMatches)
}

enum class FilterResult {

    DOESNT_MATCH, CHILD_MATCHES, MATCHES
}