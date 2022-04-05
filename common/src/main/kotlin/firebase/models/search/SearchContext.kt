package com.krystianwsul.common.firebase.models.search

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.ExactTimeStamp

sealed class SearchContext {

    companion object {

        fun startSearch(searchCriteria: SearchCriteria, now: ExactTimeStamp.Local, myUser: MyUser) =
            new(searchCriteria, false, now, myUser)

        private fun new(
            searchCriteria: SearchCriteria,
            searchingChildrenOfQueryMatch: Boolean,
            now: ExactTimeStamp.Local,
            myUser: MyUser,
        ): SearchContext {
            return if (searchCriteria.isEmpty) {
                NoSearch
            } else {
                if (searchingChildrenOfQueryMatch) {
                    QueryMatchChildren(searchCriteria, now, myUser)
                } else {
                    Normal(searchCriteria, now, myUser)
                }
            }
        }
    }

    abstract val searchCriteria: SearchCriteria

    fun <T> search(action: SearchContext.() -> T) = run(action)

    abstract fun Sequence<Task>.filterSearch(onlyHierarchy: Boolean = false): Sequence<Pair<Task, FilterResult>>

    abstract fun Sequence<Task>.filterSearchCriteria(): Sequence<Pair<Task, FilterResult>>

    abstract fun Sequence<Instance>.filterSearchCriteria(assumeChild: Boolean): Sequence<Pair<Instance, FilterResult>>

    abstract fun getChildrenSearchContext(filterResult: FilterResult): SearchContext

    object NoSearch : SearchContext() {

        override val searchCriteria = SearchCriteria.empty

        override fun Sequence<Task>.filterSearch(onlyHierarchy: Boolean) = map { it to FilterResult.NoSearch }

        override fun Sequence<Task>.filterSearchCriteria() = map { it to FilterResult.NoSearch }

        override fun Sequence<Instance>.filterSearchCriteria(assumeChild: Boolean): Sequence<Pair<Instance, FilterResult>> =
            map { it to FilterResult.NoSearch }

        override fun getChildrenSearchContext(filterResult: FilterResult): SearchContext {
            check(filterResult is FilterResult.NoSearch)

            return this
        }
    }

    sealed class Search(
        final override val searchCriteria: SearchCriteria,
        protected val now: ExactTimeStamp.Local,
        protected val myUser: MyUser,
    ) :
        SearchContext() {

        init {
            check(!searchCriteria.isEmpty)
        }

        protected fun childHierarchyMatches(task: Task, onlyHierarchy: Boolean = false): FilterResult {
            InterruptionChecker.throwIfInterrupted()

            return task.getMatchResult(searchCriteria.search)
                .getFilterResult()
                ?: getTaskChildrenResult(task, onlyHierarchy)
        }

        protected abstract fun getTaskChildrenResult(task: Task, onlyHierarchy: Boolean): FilterResult

        override fun Sequence<Task>.filterSearch(onlyHierarchy: Boolean): Sequence<Pair<Task, FilterResult>> =
            if (searchCriteria.search.isEmpty) {
                map { it to FilterResult.NoSearch }
            } else {
                map { it to childHierarchyMatches(it, onlyHierarchy) }.filter { it.second.include }
            }

        override fun Sequence<Task>.filterSearchCriteria(): Sequence<Pair<Task, FilterResult>> {
            if (searchCriteria.isTaskEmpty) return map { it to FilterResult.NoSearch }

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

        override fun Sequence<Instance>.filterSearchCriteria(assumeChild: Boolean) = if (searchCriteria.isInstanceEmpty) {
            this.map { it to FilterResult.NoSearch }
        } else {
            map { it to childHierarchyMatches(now, myUser, it, assumeChild) }.filter { it.second.include }
        }

        protected fun childHierarchyMatches(
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

            return instance.task.getMatchResult(searchCriteria.search)
                .getFilterResult()
                ?: getInstanceChildrenResult(now, myUser, instance)
        }

        protected abstract fun getInstanceChildrenResult(
            now: ExactTimeStamp.Local,
            myUser: MyUser,
            instance: Instance,
        ): FilterResult
    }

    class Normal(searchCriteria: SearchCriteria, now: ExactTimeStamp.Local, myUser: MyUser) :
        Search(searchCriteria, now, myUser) {

        override fun getTaskChildrenResult(task: Task, onlyHierarchy: Boolean): FilterResult {
            val childTasks = if (onlyHierarchy) task.getHierarchyChildTasks() else task.getChildTasks()

            return if (childTasks.any { childHierarchyMatches(it, onlyHierarchy).include })
                FilterResult.Include(false)
            else
                FilterResult.Exclude
        }

        override fun getInstanceChildrenResult(now: ExactTimeStamp.Local, myUser: MyUser, instance: Instance): FilterResult {
            return if (
                instance.getChildInstances()
                    .filter {
                        it.isVisible(
                            now,
                            Instance.VisibilityOptions(assumeChildOfVisibleParent = true)
                        )
                    }
                    .any { childHierarchyMatches(now, myUser, it, true).include }
            ) {
                FilterResult.Include(false)
            } else {
                FilterResult.Exclude
            }
        }

        override fun getChildrenSearchContext(filterResult: FilterResult): SearchContext = when (filterResult) {
            FilterResult.Exclude -> this
            is FilterResult.NoSearch -> this
            is FilterResult.Include -> if (filterResult.matchesSearch) {
                when (searchCriteria.search) {
                    is SearchCriteria.Search.Query -> QueryMatchChildren(searchCriteria, now, myUser)
                    is SearchCriteria.Search.TaskKey ->
                        new(searchCriteria.clearSearch(), false, now, myUser)
                }
            } else {
                this
            }
        }
    }

    class QueryMatchChildren(searchCriteria: SearchCriteria, now: ExactTimeStamp.Local, myUser: MyUser) :
        Search(searchCriteria, now, myUser) {

        init {
            check(searchCriteria.search is SearchCriteria.Search.Query)
        }

        override fun getTaskChildrenResult(task: Task, onlyHierarchy: Boolean) = FilterResult.Include(false)

        override fun getInstanceChildrenResult(now: ExactTimeStamp.Local, myUser: MyUser, instance: Instance) =
            FilterResult.Include(false)

        override fun getChildrenSearchContext(filterResult: FilterResult) = this
    }
}