package com.krystianwsul.common.firebase.models.search

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.ExactTimeStamp

// todo searchContext add MyUser in here, then clean up SearchData
class SearchContext private constructor(
    private val searchCriteria: SearchCriteria,
    private val searchingChildrenOfQueryMatch: Boolean,
) {

    companion object {

        fun startSearch(searchCriteria: SearchCriteria) = SearchContext(searchCriteria, false)
    }

    // todo searchContext check usages, remove receiver
    fun getChildrenSearchContext(filterResult: FilterResult) = when (filterResult) {
        FilterResult.Exclude -> this
        is FilterResult.NoSearch -> this
        is FilterResult.Include -> if (filterResult.matchesSearch) {
            when (searchCriteria.search!!) {
                is SearchCriteria.Search.Query -> SearchContext(searchCriteria, true)
                is SearchCriteria.Search.TaskKey -> SearchContext(searchCriteria.clear(), false)
            }
        } else {
            this
        }
    }

    // todo taskKey
    override fun toString() =
        "SearchContext searchingChildrenOfQueryMatch: $searchingChildrenOfQueryMatch, criteria: $searchCriteria"

    fun <T> search(action: SearchContext.() -> T) = run(action)

    // todo searchContext private
    private fun childHierarchyMatches(task: Task, onlyHierarchy: Boolean = false): FilterResult {
        InterruptionChecker.throwIfInterrupted()

        return task.getMatchResult(searchCriteria.search).let {
            it.getFilterResult() ?: run {
                if (searchingChildrenOfQueryMatch) {
                    FilterResult.Include(false)
                } else {
                    val childTasks = if (onlyHierarchy) task.getHierarchyChildTasks() else task.getChildTasks()

                    if (childTasks.any { !childHierarchyMatches(it, onlyHierarchy).doesntMatch })
                        FilterResult.Include(false)
                    else
                        FilterResult.Exclude
                }
            }
        }
    }

    fun Project<*>.filterSearchCriteria(showDeleted: Boolean, showProjects: Boolean): FilterResult {
        if (!showDeleted && endExactTimeStamp != null) return FilterResult.Exclude

        val search = searchCriteria.search
            ?.takeIf { it.hasSearch }
            ?: return FilterResult.NoSearch("c")

        search.let { it as? SearchCriteria.Search.Query }
            ?.takeIf { showProjects }
            ?.let {
                if (name.isNotEmpty() && normalizedName.contains(it.query)) return FilterResult.Include(true)
            }

        return if (getAllDependenciesLoadedTasks().any { !childHierarchyMatches(it).doesntMatch })
            FilterResult.Include(false)
        else
            FilterResult.Exclude
    }

    fun <T : Project<*>> Sequence<T>.filterSearchCriteria(showDeleted: Boolean, showProjects: Boolean) =
        map { it to it.filterSearchCriteria(showDeleted, showProjects) }.filter { !it.second.doesntMatch }

    fun Sequence<Task>.filterSearch(onlyHierarchy: Boolean = false) =
        if (searchCriteria.search?.hasSearch != true) {
            map { it to FilterResult.NoSearch("e") }
        } else {
            // todo taskKey this could return a subtype of FilterCriteria, i.e. the subset where doesnMatch = false
            map { it to childHierarchyMatches(it, onlyHierarchy) }.filter { !it.second.doesntMatch }
        }

    fun Sequence<Task>.filterSearchCriteria(
        myUser: MyUser,
        showDeleted: Boolean,
        now: ExactTimeStamp.Local,
    ): Sequence<Pair<Task, FilterResult>> {
        if (searchCriteria.isEmpty && showDeleted) return map { it to FilterResult.NoSearch("b") }

        val filtered1 = if (searchCriteria.showAssignedToOthers) {
            this
        } else {
            filter { it.isAssignedToMe(myUser) }
        }

        val filtered2 = filtered1.filterSearch()

        return if (showDeleted) {
            filtered2
        } else {
            filtered2.filter { it.first.isVisible(now) }
        }
    }

    // todo this could return the task.matchesSearch result to optimize building child searchCriteria in the calling function
    fun Sequence<Instance>.filterSearchCriteria(
        now: ExactTimeStamp.Local,
        myUser: MyUser,
        assumeChild: Boolean,
    ): Sequence<Pair<Instance, FilterResult>> = if (searchCriteria.isEmpty) {
        this.map { it to FilterResult.NoSearch("i") }
    } else {
        fun childHierarchyMatches(instance: Instance, assumeChild: Boolean): FilterResult {
            InterruptionChecker.throwIfInterrupted()

            if (!assumeChild && !searchCriteria.showAssignedToOthers && !instance.isAssignedToMe(myUser))
                return FilterResult.Exclude

            if (!searchCriteria.showDone && instance.done != null)
                return FilterResult.Exclude

            if (instance.instanceKey in searchCriteria.excludedInstanceKeys)
                return FilterResult.Exclude

            return instance.task.getMatchResult(searchCriteria.search).let {
                it.getFilterResult() ?: run {
                    if (searchingChildrenOfQueryMatch) {
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
}