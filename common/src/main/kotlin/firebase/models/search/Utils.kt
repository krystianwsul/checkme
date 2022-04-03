package com.krystianwsul.common.firebase.models.search

import android.util.Log
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.ExactTimeStamp

// todo searchContext private
fun childHierarchyMatches(task: Task, searchContext: SearchContext, onlyHierarchy: Boolean = false): FilterResult {
    InterruptionChecker.throwIfInterrupted()

    return task.getMatchResult(searchContext.searchCriteria.search).let {
        it.getFilterResult() ?: run {
            if (searchContext.searchingChildrenOfQueryMatch) {
                FilterResult.Include(false)
            } else {
                val childTasks = if (onlyHierarchy) task.getHierarchyChildTasks() else task.getChildTasks()

                if (childTasks.any { !childHierarchyMatches(it, searchContext, onlyHierarchy).doesntMatch })
                    FilterResult.Include(false)
                else
                    FilterResult.Exclude
            }
        }
    }
}

// todo this could return the task.matchesSearch result to optimize building child searchCriteria in the calling function
fun Sequence<Instance>.filterSearchCriteria(
    searchContext: SearchContext,
    now: ExactTimeStamp.Local,
    myUser: MyUser,
    assumeChild: Boolean,
): Sequence<Pair<Instance, FilterResult>> = if (searchContext.searchCriteria.isEmpty) {
    this.map { it to FilterResult.NoSearch("i") }
} else {
    fun childHierarchyMatches(instance: Instance, assumeChild: Boolean): FilterResult {
        InterruptionChecker.throwIfInterrupted()

        if (!assumeChild && !searchContext.searchCriteria.showAssignedToOthers && !instance.isAssignedToMe(myUser))
            return FilterResult.Exclude

        if (!searchContext.searchCriteria.showDone && instance.done != null)
            return FilterResult.Exclude

        if (instance.instanceKey in searchContext.searchCriteria.excludedInstanceKeys)
            return FilterResult.Exclude

        return instance.task.getMatchResult(searchContext.searchCriteria.search).let {
            it.getFilterResult() ?: run {
                if (searchContext.searchingChildrenOfQueryMatch) {
                    FilterResult.Include(false)
                } else {
                    if (
                        instance.getChildInstances()
                            .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true)) }
                            .any { !childHierarchyMatches(it, true).doesntMatch }
                    ) {
                        FilterResult.Include(false)
                    } else {
                        FilterResult.Exclude
                    }
                }
            }
        }
    }

    map { it to childHierarchyMatches(it, assumeChild) }.filter { !it.second.doesntMatch }
}

// todo taskKey remove
private fun Task.getChain(chain: MutableList<String>) {
    chain.add(0, name)

    if (isTopLevelTask()) {
        chain.add(0, project.name)
    } else {
        parentTask!!.getChain(chain)
    }
}

fun logFilterResult(task: Task, filterResult: FilterResult) {
    val chain = mutableListOf<String>()
    task.getChain(chain)

    Log.e("asdf", "magic " + chain.filter { it.isNotEmpty() }.joinToString("/") + " filterResult: " + filterResult)
}