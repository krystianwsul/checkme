package com.krystianwsul.common.relevance

import com.krystianwsul.common.domain.Instance
import com.krystianwsul.common.firebase.models.RemotePrivateProject
import com.krystianwsul.common.firebase.models.RemoteProject
import com.krystianwsul.common.time.ExactTimeStamp
import com.soywiz.klock.days

object Irrelevant {

    fun setIrrelevant(parent: RemoteProject.Parent, project: RemoteProject<*>, now: ExactTimeStamp): Collection<Instance> {
        val tasks = project.tasks

        // relevant hack
        val taskRelevances = tasks.map { it.taskKey to TaskRelevance(it) }.toMap()

        val taskHierarchies = project.taskHierarchies
        val taskHierarchyRelevances = taskHierarchies.associate { it.taskHierarchyKey to TaskHierarchyRelevance(it) }

        val existingInstances = project.existingInstances
        val rootInstances = project.getRootInstances(null, now.plusOne(), now)

        val instanceRelevances = (existingInstances + rootInstances)
                .asSequence()
                .distinct()
                .map { it.instanceKey to InstanceRelevance(it) }
                .toList()
                .toMap()
                .toMutableMap()

        val yesterday = ExactTimeStamp(now.toDateTimeSoy() - 1.days)

        fun getIrrelevantNow(endExactTimeStamp: ExactTimeStamp?) = endExactTimeStamp?.takeIf { it > yesterday } // delay deleting removed tasks by a day
                ?.minusOne()
                ?: now

        tasks.asSequence()
                .filter {
                    val exactTimeStamp = getIrrelevantNow(it.getEndExactTimeStamp())

                    it.current(exactTimeStamp) && it.isRootTask(exactTimeStamp) && it.isVisible(exactTimeStamp, true)
                }
                .map { taskRelevances.getValue(it.taskKey) }
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        rootInstances.map { instanceRelevances[it.instanceKey]!! }.forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        existingInstances.asSequence()
                .filter { it.isRootInstance(now) && it.isVisible(now, true) }
                .map { instanceRelevances[it.instanceKey]!! }
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        val relevantTaskRelevances = taskRelevances.values.filter { it.relevant }
        val relevantTasks = relevantTaskRelevances.map { it.task }

        val irrelevantTasks = tasks - relevantTasks

        val visibleIrrelevantTasks = irrelevantTasks.filter { it.isVisible(now, true) }
        if (visibleIrrelevantTasks.isNotEmpty())
            throw VisibleIrrelevantTasksException(visibleIrrelevantTasks.joinToString(", ") { it.taskKey.toString() })

        val relevantTaskHierarchyRelevances = taskHierarchyRelevances.values.filter { it.relevant }
        val relevantTaskHierarchies = relevantTaskHierarchyRelevances.map { it.taskHierarchy }

        val irrelevantTaskHierarchies = taskHierarchies - relevantTaskHierarchies

        val relevantInstances = instanceRelevances.values
                .filter { it.relevant }
                .map { it.instance }

        val relevantExistingInstances = relevantInstances.filter { it.exists() }
        val irrelevantExistingInstances = existingInstances - relevantExistingInstances

        val visibleIrrelevantExistingInstances = irrelevantExistingInstances.filter { it.isVisible(now, true) }
        if (visibleIrrelevantExistingInstances.isNotEmpty())
            throw VisibleIrrelevantExistingInstancesException(visibleIrrelevantExistingInstances.joinToString(", ") { it.instanceKey.toString() })

        irrelevantExistingInstances.forEach { it.delete() }
        irrelevantTasks.forEach { it.delete() }
        irrelevantTaskHierarchies.forEach { it.delete() }

        val remoteCustomTimes = project.customTimes
        val remoteCustomTimeRelevances = remoteCustomTimes.map { Pair(it.projectId, it.id) to RemoteCustomTimeRelevance(it) }.toMap()

        if (project is RemotePrivateProject) {
            project.customTimes
                    .filter { it.current(getIrrelevantNow(it.endExactTimeStamp)) }
                    .forEach { remoteCustomTimeRelevances.getValue(Pair(it.projectId, it.id)).setRelevant() }
        }

        val remoteProjects = listOf(project)
        val remoteProjectRelevances = remoteProjects.map { it.id to RemoteProjectRelevance(it) }.toMap()

        remoteProjects.filter { it.current(getIrrelevantNow(it.endExactTimeStamp)) }
                .map { remoteProjectRelevances.getValue(it.id) }
                .forEach { it.setRelevant() }

        taskRelevances.values
                .filter { it.relevant }
                .forEach { it.setRemoteRelevant(remoteCustomTimeRelevances, remoteProjectRelevances) }

        instanceRelevances.values
                .filter { it.relevant }
                .forEach { it.setRemoteRelevant(remoteCustomTimeRelevances, remoteProjectRelevances) }

        val relevantRemoteCustomTimes = remoteCustomTimeRelevances.values
                .filter { it.relevant }
                .map { it.remoteCustomTime }

        val irrelevantRemoteCustomTimes = remoteCustomTimes - relevantRemoteCustomTimes
        irrelevantRemoteCustomTimes.forEach { it.delete() }

        val relevantRemoteProjects = remoteProjectRelevances.values
                .filter { it.relevant }
                .map { it.remoteProject }

        val irrelevantRemoteProjects = remoteProjects - relevantRemoteProjects
        irrelevantRemoteProjects.forEach { it.delete(parent) }

        return relevantInstances
    }

    private class VisibleIrrelevantTasksException(message: String) : Exception(message)
    private class VisibleIrrelevantExistingInstancesException(message: String) : Exception(message)
}