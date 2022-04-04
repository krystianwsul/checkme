package com.krystianwsul.common.firebase.models.search

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.ExactTimeStamp

// todo searchContext add MyUser in here, then clean up SearchData
sealed class SearchContext {

    companion object {

        fun startSearch(searchCriteria: SearchCriteria) = new(searchCriteria, false)

        private fun new(searchCriteria: SearchCriteria, searchingChildrenOfQueryMatch: Boolean): SearchContext {
            return if (searchCriteria.isEmpty) {
                NoSearch
            } else {
                if (searchingChildrenOfQueryMatch) {
                    QueryMatchChildren(searchCriteria)
                } else {
                    Normal(searchCriteria)
                }
            }
        }
    }

    fun <T> search(action: SearchContext.() -> T) = run(action)

    abstract fun Sequence<Task>.filterSearch(onlyHierarchy: Boolean = false): Sequence<Pair<Task, FilterResult>>

    abstract fun Sequence<Task>.filterSearchCriteria(
        myUser: MyUser,
        now: ExactTimeStamp.Local,
    ): Sequence<Pair<Task, FilterResult>>

    abstract fun Sequence<Instance>.filterSearchCriteria(
        now: ExactTimeStamp.Local,
        myUser: MyUser,
        assumeChild: Boolean,
    ): Sequence<Pair<Instance, FilterResult>>

    abstract fun getChildrenSearchContext(filterResult: FilterResult): SearchContext

    object NoSearch : SearchContext() {

        override fun Sequence<Task>.filterSearch(onlyHierarchy: Boolean) = map { it to FilterResult.NoSearch("e") }

        override fun Sequence<Task>.filterSearchCriteria(
            myUser: MyUser,
            now: ExactTimeStamp.Local,
        ) = map { it to FilterResult.NoSearch("b") }

        override fun Sequence<Instance>.filterSearchCriteria(
            now: ExactTimeStamp.Local,
            myUser: MyUser,
            assumeChild: Boolean,
        ): Sequence<Pair<Instance, FilterResult>> = map { it to FilterResult.NoSearch("i") }

        override fun getChildrenSearchContext(filterResult: FilterResult): SearchContext {
            check(filterResult is FilterResult.NoSearch)

            return this
        }
    }

    sealed class Search(protected val searchCriteria: SearchCriteria, private val searchingChildrenOfQueryMatch: Boolean) :
        SearchContext() {

        init {
            check(!searchCriteria.isEmpty)
        }

        private fun childHierarchyMatches(task: Task, onlyHierarchy: Boolean = false): FilterResult {
            InterruptionChecker.throwIfInterrupted()

            return task.getMatchResult(searchCriteria.search)
                .getFilterResult()
                ?: getTaskChildrenResult(task, onlyHierarchy)
        }

        private fun getTaskChildrenResult(task: Task, onlyHierarchy: Boolean): FilterResult {
            return if (searchingChildrenOfQueryMatch) {
                FilterResult.Include(false)
            } else {
                val childTasks = if (onlyHierarchy) task.getHierarchyChildTasks() else task.getChildTasks()

                if (childTasks.any { !childHierarchyMatches(it, onlyHierarchy).doesntMatch })
                    FilterResult.Include(false)
                else
                    FilterResult.Exclude
            }
        }

        override fun Sequence<Task>.filterSearch(onlyHierarchy: Boolean): Sequence<Pair<Task, FilterResult>> =
            if (searchCriteria.search.isEmpty) {
                map { it to FilterResult.NoSearch("e") }
            } else {
                // todo taskKey this could return a subtype of FilterCriteria, i.e. the subset where doesnMatch = false
                map { it to childHierarchyMatches(it, onlyHierarchy) }.filter { !it.second.doesntMatch }
            }

        override fun Sequence<Task>.filterSearchCriteria(
            myUser: MyUser,
            now: ExactTimeStamp.Local,
        ): Sequence<Pair<Task, FilterResult>> {
            if (searchCriteria.isTaskEmpty) return map { it to FilterResult.NoSearch("b") }

            val filtered1 = if (searchCriteria.showAssignedToOthers) {
                this
            } else {
                filter { it.isAssignedToMe(myUser) }
            }

            val filtered2 = filtered1.filterSearch()

            return if (searchCriteria.taskCriteria.showDeleted) {
                filtered2
            } else {
                filtered2.filter { it.first.isVisible(now) }
            }
        }

        override fun Sequence<Instance>.filterSearchCriteria(
            now: ExactTimeStamp.Local,
            myUser: MyUser,
            assumeChild: Boolean,
        ): Sequence<Pair<Instance, FilterResult>> = if (searchCriteria.isInstanceEmpty) {
            this.map { it to FilterResult.NoSearch("i") }
        } else {
            map { it to childHierarchyMatches(now, myUser, it, assumeChild) }.filter { !it.second.doesntMatch }
        }

        private fun childHierarchyMatches(
            now: ExactTimeStamp.Local,
            myUser: MyUser,
            instance: Instance,
            assumeChild: Boolean,
        ): FilterResult {
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
                                .filter {
                                    it.isVisible(
                                        now,
                                        Instance.VisibilityOptions(assumeChildOfVisibleParent = true)
                                    )
                                }
                                .any { !childHierarchyMatches(now, myUser, it, true).doesntMatch }
                        ) {
                            FilterResult.Include(false)
                        } else {
                            FilterResult.Exclude
                        }
                    }
                }
            }
        }

        override fun getChildrenSearchContext(filterResult: FilterResult): SearchContext = when (filterResult) {
            FilterResult.Exclude -> this
            is FilterResult.NoSearch -> this
            is FilterResult.Include -> if (filterResult.matchesSearch) {
                when (searchCriteria.search) {
                    is SearchCriteria.Search.Query -> new(searchCriteria, true)
                    is SearchCriteria.Search.TaskKey -> new(searchCriteria.clearSearch(), false)
                }
            } else {
                this
            }
        }
    }

    class Normal(searchCriteria: SearchCriteria) : Search(searchCriteria, false)

    class QueryMatchChildren(searchCriteria: SearchCriteria) : Search(searchCriteria, true)
}