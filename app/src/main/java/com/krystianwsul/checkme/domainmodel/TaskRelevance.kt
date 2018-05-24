package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import junit.framework.Assert

class TaskRelevance(private val domainFactory: DomainFactory, val task: Task) {

    var relevant = false
        private set

    fun setRelevant(taskRelevances: Map<TaskKey, TaskRelevance>, instanceRelevances: MutableMap<InstanceKey, DomainFactory.InstanceRelevance>, customTimeRelevances: Map<Int, DomainFactory.LocalCustomTimeRelevance>, now: ExactTimeStamp) {
        if (relevant) return

        relevant = true

        val taskKey = task.taskKey

        // mark parents relevant
        task.getTaskHierarchiesByChildTaskKey(taskKey)
                .map { taskRelevances[it.parentTaskKey]!! }
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now) }

        // mark children relevant
        task.getTaskHierarchiesByParentTaskKey(taskKey)
                .map { taskRelevances[it.childTaskKey]!! }
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now) }

        val oldestVisible = task.getOldestVisible()!!

        // mark instances relevant
        domainFactory.getPastInstances(task, now)
                .filter { it.scheduleDate >= oldestVisible }
                .map { instance ->
                    val instanceKey = instance.instanceKey

                    if (!instanceRelevances.containsKey(instanceKey))
                        instanceRelevances[instanceKey] = DomainFactory.InstanceRelevance(instance)

                    instanceRelevances[instanceKey]!!
                }
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now) }

        task.existingInstances
                .values
                .filter { it.scheduleDate >= oldestVisible }
                .map { instanceRelevances[it.instanceKey]!! }
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now) }

        // mark custom times relevant
        task.schedules
                .mapNotNull { it.customTimeKey?.localCustomTimeId }
                .map { customTimeRelevances[it]!! }
                .forEach { it.setRelevant() }
    }

    fun setRemoteRelevant(remoteCustomTimeRelevances: Map<kotlin.Pair<String, String>, DomainFactory.RemoteCustomTimeRelevance>, remoteProjectRelevances: Map<String, DomainFactory.RemoteProjectRelevance>) {
        Assert.assertTrue(relevant)

        task.schedules
                .mapNotNull { it.remoteCustomTimeKey }
                .map { remoteCustomTimeRelevances[it]!! }
                .forEach { it.setRelevant() }

        task.remoteNullableProject?.let { remoteProjectRelevances[it.id]!!.setRelevant() }
    }
}
