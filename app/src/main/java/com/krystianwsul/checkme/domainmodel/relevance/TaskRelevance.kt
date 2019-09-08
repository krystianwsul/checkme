package com.krystianwsul.checkme.domainmodel.relevance

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Task
import com.krystianwsul.checkme.utils.InstanceKey

import com.krystianwsul.checkme.utils.TaskHierarchyKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.common.utils.RemoteCustomTimeId


class TaskRelevance(private val domainFactory: DomainFactory, val task: Task) {

    var relevant = false
        private set

    fun setRelevant(taskRelevances: Map<TaskKey, TaskRelevance>, taskHierarchyRelevances: Map<TaskHierarchyKey, TaskHierarchyRelevance>, instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>, now: ExactTimeStamp) {
        if (relevant)
            return

        relevant = true

        val taskKey = task.taskKey

        (task.getTaskHierarchiesByChildTaskKey(taskKey) + task.getTaskHierarchiesByParentTaskKey(taskKey))
                .filter {
                    val hierarchyExactTimeStamp = task.getHierarchyExactTimeStamp(now)
                    it.notDeleted(hierarchyExactTimeStamp)
                }
                .forEach { taskHierarchyRelevances.getValue(it.taskHierarchyKey).setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        val oldestVisible = task.getOldestVisible()!!

        // mark instances relevant
        domainFactory.getPastInstances(task, now)
                .asSequence()
                .filter { it.scheduleDate >= oldestVisible }
                .map { instance ->
                    val instanceKey = instance.instanceKey

                    if (!instanceRelevances.containsKey(instanceKey))
                        instanceRelevances[instanceKey] = InstanceRelevance(instance)

                    instanceRelevances[instanceKey]!!
                }
                .toList()
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        task.existingInstances
                .values
                .filter { it.scheduleDate >= oldestVisible || it.hidden }
                .map { instanceRelevances[it.instanceKey]!! }
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }
    }

    fun setRemoteRelevant(remoteCustomTimeRelevances: Map<Pair<String, RemoteCustomTimeId>, RemoteCustomTimeRelevance>, remoteProjectRelevances: Map<String, RemoteProjectRelevance>) {
        check(relevant)

        task.schedules
                .mapNotNull { it.remoteCustomTimeKey }
                .map { remoteCustomTimeRelevances.getValue(it) }
                .forEach { it.setRelevant() }

        remoteProjectRelevances.getValue(task.project.id).setRelevant()
    }
}
