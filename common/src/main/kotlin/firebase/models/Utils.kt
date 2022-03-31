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

    if (task.matchesSearch(search)) return FilterResult.MATCHES

    val childTasks = if (onlyHierarchy) task.getHierarchyChildTasks() else task.getChildTasks()

    if (childTasks.any { childHierarchyMatches(it, search, onlyHierarchy) != FilterResult.DOESNT_MATCH })
        return FilterResult.INCLUDE

    return FilterResult.DOESNT_MATCH
}

fun Sequence<Task>.filterSearch(search: SearchCriteria.Search?, onlyHierarchy: Boolean = false) =
    if (search?.hasSearch != true) {
        map { it to FilterResult.INCLUDE }
    } else {
        map { it to childHierarchyMatches(it, search, onlyHierarchy) }.filter { it.second != FilterResult.DOESNT_MATCH }
    }

fun Sequence<Task>.filterSearchCriteria(
    searchCriteria: SearchCriteria,
    myUser: MyUser,
    showDeleted: Boolean,
    now: ExactTimeStamp.Local,
): Sequence<Pair<Task, FilterResult>> {
    if (searchCriteria.isEmpty && showDeleted) return map { it to FilterResult.INCLUDE }

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
    /*
    CHILD_MATCHES really means, "should I apply the searchCriteria to child tasks?".  So yes, if projects aren't visible,
    then - given how this function is used - this is what we want to return.
     */
    if (!showProjects) return FilterResult.INCLUDE

    if (!showDeleted && endExactTimeStamp != null) return FilterResult.DOESNT_MATCH

    if (searchCriteria.search == null) return FilterResult.INCLUDE

    // ditto as CHILD_MATCHES comment above
    if (name.isEmpty()) return FilterResult.INCLUDE

    when (searchCriteria.search) {
        is SearchCriteria.Search.Query -> {
            val query = searchCriteria.search
            if (query.query.isEmpty()) return FilterResult.INCLUDE

            if (normalizedName.contains(query.query)) return FilterResult.MATCHES

            return if (getAllDependenciesLoadedTasks().any { childHierarchyMatches(it, query) != FilterResult.DOESNT_MATCH })
                FilterResult.INCLUDE
            else
                FilterResult.DOESNT_MATCH
        }
        is SearchCriteria.Search.TaskKey -> return FilterResult.INCLUDE
    }
}

fun <T : Project<*>> Sequence<T>.filterSearchCriteria(
    searchCriteria: SearchCriteria,
    showDeleted: Boolean,
    showProjects: Boolean,
) = map { it to it.filterSearchCriteria(searchCriteria, showDeleted, showProjects) }.filter {
    it.second != FilterResult.DOESNT_MATCH
}

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

        if (instance.task.matchesSearch(searchCriteria.search)) return true

        return instance.getChildInstances()
            .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true)) }
            .any { childHierarchyMatches(it, true) }
    }

    filter { childHierarchyMatches(it, assumeChild) }
}

enum class FilterResult(val matches: Boolean = false) {

    DOESNT_MATCH {

        override fun getChildrenSearchCriteria(searchCriteria: SearchCriteria) = searchCriteria
    },

    INCLUDE {

        override fun getChildrenSearchCriteria(searchCriteria: SearchCriteria) = searchCriteria
    },

    MATCHES(true) {

        override fun getChildrenSearchCriteria(searchCriteria: SearchCriteria): SearchCriteria {
            return if (searchCriteria.search?.hasSearch == true) {
                searchCriteria.copy(search = null)
            } else {
                searchCriteria
            }
        }
    };

    abstract fun getChildrenSearchCriteria(searchCriteria: SearchCriteria): SearchCriteria
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