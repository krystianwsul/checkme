package com.krystianwsul.common.firebase.models.search

import android.util.Log
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.ExactTimeStamp

private fun childHierarchyMatches(task: Task, searchContext: SearchContext, onlyHierarchy: Boolean = false): FilterResult {
    InterruptionChecker.throwIfInterrupted()

    return task.getMatchResult(searchContext.searchCriteria.search).let {
        it.getFilterResult() ?: run {
            if (searchContext.searchingChildrenOfQueryMatch) {
                FilterResult.Include
            } else {
                val childTasks = if (onlyHierarchy) task.getHierarchyChildTasks() else task.getChildTasks()

                if (childTasks.any { !childHierarchyMatches(it, searchContext, onlyHierarchy).doesntMatch })
                    FilterResult.Include
                else
                    FilterResult.DoesntMatch
            }
        }
    }
}

fun Sequence<Task>.filterSearch(searchContext: SearchContext, onlyHierarchy: Boolean = false) =
    if (searchContext.searchCriteria.search?.hasSearch != true) {
        map { it to FilterResult.NoSearch("e") }
    } else {
        map { it to childHierarchyMatches(it, searchContext, onlyHierarchy) }.filter { !it.second.doesntMatch }
    }


fun Sequence<Task>.filterSearchCriteria(
    searchContext: SearchContext,
    myUser: MyUser,
    showDeleted: Boolean,
    now: ExactTimeStamp.Local,
): Sequence<Pair<Task, FilterResult>> {
    if (searchContext.searchCriteria.isEmpty && showDeleted) return map { it to FilterResult.NoSearch("b") }

    val filtered1 = if (searchContext.searchCriteria.showAssignedToOthers) {
        this
    } else {
        filter { it.isAssignedToMe(myUser) }
    }

    val filtered2 = filtered1.filterSearch(searchContext)

    return if (showDeleted) {
        filtered2
    } else {
        filtered2.filter { it.first.isVisible(now) }
    }
}

fun Project<*>.filterSearchCriteria(
    searchContext: SearchContext,
    showDeleted: Boolean,
    showProjects: Boolean,
): FilterResult {
    if (!showDeleted && endExactTimeStamp != null) return FilterResult.DoesntMatch

    val search = searchContext.searchCriteria
        .search
        ?.takeIf { it.hasSearch }
        ?: return FilterResult.NoSearch("c")

    search.let { it as? SearchCriteria.Search.Query }
        ?.takeIf { showProjects }
        ?.let {
            if (name.isNotEmpty() && normalizedName.contains(it.query)) return FilterResult.Matches(true)
        }

    return if (getAllDependenciesLoadedTasks().any { !childHierarchyMatches(it, searchContext).doesntMatch })
        FilterResult.Include
    else
        FilterResult.DoesntMatch
}

fun <T : Project<*>> Sequence<T>.filterSearchCriteria(
    searchContext: SearchContext,
    showDeleted: Boolean,
    showProjects: Boolean,
) = map { it to it.filterSearchCriteria(searchContext, showDeleted, showProjects) }.filter { !it.second.doesntMatch }

// todo this could return the task.matchesSearch result to optimize building child searchCriteria in the calling function
fun Sequence<Instance>.filterSearchCriteria(
    searchContext: SearchContext,
    now: ExactTimeStamp.Local,
    myUser: MyUser,
    assumeChild: Boolean,
) = if (searchContext.searchCriteria.isEmpty) {
    this
} else {
    fun childHierarchyMatches(
        instance: Instance,
        assumeChild: Boolean,
        searchContext: SearchContext,
        firstDepth: Boolean,
    ): Boolean {
        InterruptionChecker.throwIfInterrupted()

        if (!assumeChild && !searchContext.searchCriteria.showAssignedToOthers && !instance.isAssignedToMe(myUser)) return false

        if (!searchContext.searchCriteria.showDone && instance.done != null) return false

        if (instance.instanceKey in searchContext.searchCriteria.excludedInstanceKeys) return false

        /*
        Okay so, this is a weird situation.  This function does two things:
        1. For direct invocations, it tells us if this instance should be included in the results.
        2. For nested invocations, it tells us whether a parent instance has matching children.

        These are two different things, but whatever.  There's a lot of overlapping functionality.  This check ensures
        that for case #1, the instance gets included in the results if it's a child of a match.  For case #2, we're trying
        to figure out if it really *is* a match, not just if it should be included in the results.
         */
        if (firstDepth && searchContext.searchingChildrenOfQueryMatch) return true

        return instance.task.getMatchResult(searchContext.searchCriteria.search).let {
            if (it.includeWithoutChildren) {
                true
            } else {
                val childrenSearchContext = searchContext.getChildrenSearchContext(it)

                instance.getChildInstances()
                    .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true)) }
                    .any { childHierarchyMatches(it, true, childrenSearchContext, false) }
            }
        }
    }

    filter { childHierarchyMatches(it, assumeChild, searchContext, true) }
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