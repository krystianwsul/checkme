package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey


private fun childHierarchyMatches(task: Task, search: SearchCriteria.Search?, onlyHierarchy: Boolean = false): FilterResult {
    InterruptionChecker.throwIfInterrupted()

    return task.getFilterResult(search).let {
        when (it) {
            FilterResult.NoSearch -> it
            FilterResult.Include -> {
                val childTasks = if (onlyHierarchy) task.getHierarchyChildTasks() else task.getChildTasks()

                if (childTasks.any { !childHierarchyMatches(it, search, onlyHierarchy).doesntMatch })
                    FilterResult.Include
                else
                    FilterResult.DoesntMatch
            }
            FilterResult.Matches -> it
        }
    }
}

fun Sequence<Task>.filterSearch(search: SearchCriteria.Search?, onlyHierarchy: Boolean = false) =
    if (search?.hasSearch != true) {
        map { it to FilterResult.NoSearch }
    } else {
        map { it to childHierarchyMatches(it, search, onlyHierarchy) }.filter { !it.second.doesntMatch }
    }

fun Sequence<Task>.filterSearchCriteria(
    searchCriteria: SearchCriteria,
    myUser: MyUser,
    showDeleted: Boolean,
    now: ExactTimeStamp.Local,
): Sequence<Pair<Task, FilterResult>> {
    if (searchCriteria.isEmpty && showDeleted) return map { it to FilterResult.NoSearch }

    val filtered1 = if (searchCriteria.showAssignedToOthers) {
        this
    } else {
        filter { it.isAssignedToMe(myUser) }
    }

    val filtered2 = filtered1.filterSearch(searchCriteria.search)

    return if (showDeleted) {
        filtered2
    } else {
        filtered2.filter { it.first.isVisible(now) }
    }
}

fun Project<*>.filterSearchCriteria(
    searchCriteria: SearchCriteria,
    showDeleted: Boolean,
    showProjects: Boolean,
): FilterResult {
    if (!showDeleted && endExactTimeStamp != null) return FilterResult.DoesntMatch

    if (searchCriteria.search == null) return FilterResult.NoSearch
    if (!searchCriteria.search.hasSearch) return FilterResult.NoSearch

    searchCriteria.search
        .let { it as? SearchCriteria.Search.Query }
        ?.takeIf { showProjects }
        ?.let {
            if (name.isNotEmpty() && normalizedName.contains(it.query)) return FilterResult.Matches
        }

    return if (getAllDependenciesLoadedTasks().any { !childHierarchyMatches(it, searchCriteria.search).doesntMatch })
        FilterResult.Include
    else
        FilterResult.DoesntMatch
}

fun <T : Project<*>> Sequence<T>.filterSearchCriteria(
    searchCriteria: SearchCriteria,
    showDeleted: Boolean,
    showProjects: Boolean,
) = map { it to it.filterSearchCriteria(searchCriteria, showDeleted, showProjects) }.filter { !it.second.doesntMatch }

// todo this could return the task.matchesSearch result to optimize building child searchCriteria in the calling function
fun Sequence<Instance>.filterSearchCriteria(
    searchCriteria: SearchCriteria,
    now: ExactTimeStamp.Local,
    myUser: MyUser,
    assumeChild: Boolean,
) = if (searchCriteria.isEmpty) {
    this
} else {
    fun childHierarchyMatches(instance: Instance, assumeChild: Boolean): Boolean {
        InterruptionChecker.throwIfInterrupted()

        if (!assumeChild && !searchCriteria.showAssignedToOthers && !instance.isAssignedToMe(myUser)) return false

        if (!searchCriteria.showDone && instance.done != null) return false

        if (instance.instanceKey in searchCriteria.excludedInstanceKeys) return false

        return instance.task.getFilterResult(searchCriteria.search).let {
            when (it) {
                FilterResult.NoSearch -> true
                FilterResult.Include -> instance.getChildInstances()
                    .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true)) }
                    .any { childHierarchyMatches(it, true) }
                FilterResult.Matches -> true
            }
        }
    }

    filter { childHierarchyMatches(it, assumeChild) }
}

sealed interface FilterResult {

    val doesntMatch: Boolean

    val matches get() = false

    fun getChildrenSearchCriteria(searchCriteria: SearchCriteria) = searchCriteria

    object DoesntMatch : FilterResult {

        override val doesntMatch = true
    }

    sealed class Task : FilterResult {

        override val doesntMatch = false
    }

    object NoSearch : Task()

    object Include : Task()

    object Matches : Task() {

        override val matches = true

        override fun getChildrenSearchCriteria(searchCriteria: SearchCriteria): SearchCriteria {
            return if (searchCriteria.search?.hasSearch == true) {
                searchCriteria.copy(search = null)
            } else {
                searchCriteria
            }
        }
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