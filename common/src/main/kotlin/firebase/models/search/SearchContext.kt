package com.krystianwsul.common.firebase.models.search

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.time.ExactTimeStamp

class SearchContext private constructor(val searchCriteria: SearchCriteria, val searchingChildrenOfQueryMatch: Boolean) {

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

        return if (
            getAllDependenciesLoadedTasks().any { !childHierarchyMatches(it, this@SearchContext).doesntMatch }
        )
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
            map { it to childHierarchyMatches(it, this@SearchContext, onlyHierarchy) }.filter { !it.second.doesntMatch }
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
}