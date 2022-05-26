package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.models.project.OwnedProject
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

// used in RelevanceChecker
fun checkInconsistentRootTaskIds(rootTasks: Collection<RootTask>, projects: Collection<OwnedProject<*>>) {
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

fun <T> Sequence<T>.requireDistinct(): Sequence<T> {
    val previous = mutableSetOf<T>()

    return onEach {
        check(it !in previous) { "sequence already contains $it" }

        previous += it
    }
}

fun Sequence<Instance>.filterAndSort(
    startExactTimeStamp: ExactTimeStamp.Offset?,
    endExactTimeStamp: ExactTimeStamp.Offset?,
) = map { it.instanceDateTime to it }.filter {
    InterruptionChecker.throwIfInterrupted()

    val exactTimeStamp = it.first.toLocalExactTimeStamp()

    if (startExactTimeStamp?.let { exactTimeStamp < it } == true) return@filter false

    if (endExactTimeStamp?.let { exactTimeStamp >= it } == true) return@filter false

    true
}
    .sortedBy { it.first } // this evaluates everything earlier
    .map { it.second }