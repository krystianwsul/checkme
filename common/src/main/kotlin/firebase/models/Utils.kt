package com.krystianwsul.common.firebase.models

import android.util.Log
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey


private fun childHierarchyMatches(task: Task, searchContext: SearchContext, onlyHierarchy: Boolean = false): FilterResult {
    InterruptionChecker.throwIfInterrupted()

    return task.getMatchResult(searchContext.searchCriteria.search).let {
        it.getFilterResult() ?: run {
            val childTasks = if (onlyHierarchy) task.getHierarchyChildTasks() else task.getChildTasks()

            if (childTasks.any { !childHierarchyMatches(it, searchContext, onlyHierarchy).doesntMatch })
                FilterResult.Include
            else
                FilterResult.DoesntMatch
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

    if (searchContext.searchCriteria.search == null) return FilterResult.NoSearch("c")
    if (!searchContext.searchCriteria.search.hasSearch) return FilterResult.NoSearch("d")

    searchContext.searchCriteria
        .search
        .let { it as? SearchCriteria.Search.Query }
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
    fun childHierarchyMatches(instance: Instance, assumeChild: Boolean): Boolean {
        InterruptionChecker.throwIfInterrupted()

        if (!assumeChild && !searchContext.searchCriteria.showAssignedToOthers && !instance.isAssignedToMe(myUser)) return false

        if (!searchContext.searchCriteria.showDone && instance.done != null) return false

        if (instance.instanceKey in searchContext.searchCriteria.excludedInstanceKeys) return false

        return instance.task.getMatchResult(searchContext.searchCriteria.search).let {
            it.includeWithoutChildren
                .takeIf { it }
                ?: instance.getChildInstances()
                    .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true)) }
                    .any { childHierarchyMatches(it, true) }
        }
    }

    filter { childHierarchyMatches(it, assumeChild) }
}

sealed interface FilterResult {

    val doesntMatch: Boolean

    val matchesSearch get() = false

    fun getChildrenSearchContext(searchContext: SearchContext) = searchContext

    object DoesntMatch : FilterResult {

        override val doesntMatch = true

        override fun toString() = "FilterResult.DoesntMatch" // todo taskKey
    }

    sealed class Task : FilterResult {

        override val doesntMatch = false

        override fun toString() = "FilterResult.Task" // todo taskKey
    }

    class NoSearch(val source: String) : Task() { // todo taskKey remove source, revert to object


        override fun toString() = "FilterResult.NoSearch source: $source" // todo taskKey
    }

    object Include : Task() {


        override fun toString() = "FilterResult.Include" // todo taskKey
    }

    // todo taskKey this naming is awful
    class Matches(override val matchesSearch: Boolean) : Task() {

        override fun getChildrenSearchContext(searchContext: SearchContext) =
            if (matchesSearch) SearchContext(searchContext.searchCriteria.clear()) else searchContext

        override fun toString() = "FilterResult.Matches matchesSearch: $matchesSearch" // todo taskKey
    }
}

// used in RelevanceChecker
fun checkInconsistentRootTaskIds(rootTasks: Collection<RootTask>, projects: Collection<Project<*>>) {
    val rootTaskProjectKeys = rootTasks.associate { it.taskKey to it.project.projectKey }

    val rootTasksInProjectKeys = projects.flatMap { project ->
        project.projectRecord
            .rootTaskParentDelegate
            .rootTaskKeys
            .map { it to project.projectKey }
    }
        .groupBy { it.first }
        .mapValues { it.value.map { it.second }.toSet() }

    rootTaskProjectKeys.entries
        .map { (taskKey, projectKey) ->
            Triple(taskKey, projectKey, rootTasksInProjectKeys[taskKey] ?: setOf())
        }
        .filter { (_, correctProjectKey, allFeaturingProjectKeys) ->
            correctProjectKey !in allFeaturingProjectKeys
        }
        .takeIf { it.isNotEmpty() }
        ?.let { throw InconsistentRootTaskIdsException(it) }
}

private class InconsistentRootTaskIdsException(pairs: List<Triple<TaskKey.Root, ProjectKey<*>, Set<ProjectKey<*>>>>) :
    Exception(
        "rootTaskIds in wrong projects:\n" + pairs.joinToString(";\n") {
            "${it.first} says it belongs in project ${it.second}, but was found in ${it.third}"
        }
    )

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

class SearchContext(val searchCriteria: SearchCriteria)