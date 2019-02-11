package com.krystianwsul.checkme.domainmodel.relevance

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Task
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp


class TaskRelevance(private val domainFactory: DomainFactory, val task: Task) {

    var relevant = false
        private set

    fun setRelevant(taskRelevances: Map<TaskKey, TaskRelevance>, instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>, now: ExactTimeStamp) {
        if (relevant)
            return

        relevant = true

        val taskKey = task.taskKey

        // mark parents and children relevant
        (task.getTaskHierarchiesByChildTaskKey(taskKey)
                .filter { it.notDeleted(now) }
                .map { it.parentTaskKey }
                + task.getTaskHierarchiesByParentTaskKey(taskKey)
                .filter { it.notDeleted(now) }
                .map { it.childTaskKey })
                .forEach { taskRelevances.getValue(it).setRelevant(taskRelevances, instanceRelevances, now) }

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
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, now) }

        task.existingInstances
                .values
                .filter { it.scheduleDate >= oldestVisible }
                .map { instanceRelevances[it.instanceKey]!! }
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, now) }
    }

    fun setRemoteRelevant(remoteCustomTimeRelevances: Map<Pair<String, RemoteCustomTimeId>, RemoteCustomTimeRelevance>, remoteProjectRelevances: Map<String, RemoteProjectRelevance>) {
        check(relevant)

        task.schedules
                .mapNotNull { it.remoteCustomTimeKey }
                .map { remoteCustomTimeRelevances.getValue(it) }
                .forEach { it.setRelevant() }

        task.remoteNullableProject?.let { remoteProjectRelevances.getValue(it.id).setRelevant() }
    }
}
