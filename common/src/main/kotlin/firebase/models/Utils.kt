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

fun Sequence<Task>.filterSearch(search: SearchCriteria.Search?, onlyHierarchy: Boolean = false) =
    if (search?.hasSearch != true) {
        map { it to FilterResult.MATCHES }
    } else {
        fun childHierarchyMatches(task: Task): FilterResult {
            InterruptionChecker.throwIfInterrupted()

            if (task.matchesSearch(search)) return FilterResult.MATCHES

            val childTasks = if (onlyHierarchy) task.getHierarchyChildTasks() else task.getChildTasks()

            if (childTasks.any { childHierarchyMatches(it) != FilterResult.DOESNT_MATCH })
                return FilterResult.CHILD_MATCHES

            return FilterResult.DOESNT_MATCH
        }

        map { it to childHierarchyMatches(it) }.filter { it.second != FilterResult.DOESNT_MATCH }
    }

fun Sequence<Task>.filterSearchCriteria(
    searchCriteria: SearchCriteria,
    myUser: MyUser,
    showDeleted: Boolean,
    now: ExactTimeStamp.Local,
): Sequence<Task> {
    Log.e("asdf", "magic showDeleted $showDeleted")
    if (searchCriteria.isEmpty && showDeleted) return this

    val filtered = if (searchCriteria.showAssignedToOthers) {
        this
    } else {
        filter { it.isAssignedToMe(myUser) }
    }

    return if (showDeleted) {
        filtered
    } else {
        Log.e("asdf", "magic filtering")
        filtered.filter { it.isVisible(now).also { vi -> Log.e("asdf", "magic ${it.name} isVisible? " + vi) } }
    }
}

fun Project<*>.filterSearchCriteria(showDeleted: Boolean) = showDeleted || endExactTimeStamp == null

fun Sequence<Project<*>>.filterSearchCriteria(showDeleted: Boolean) = filter { it.filterSearchCriteria(showDeleted) }

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

enum class FilterResult {

    DOESNT_MATCH, CHILD_MATCHES, MATCHES
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