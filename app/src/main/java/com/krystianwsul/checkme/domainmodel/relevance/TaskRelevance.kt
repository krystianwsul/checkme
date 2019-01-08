package com.krystianwsul.checkme.domainmodel.relevance

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Task
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp


class TaskRelevance(private val domainFactory: DomainFactory, val task: Task) {

    var relevant = false
        private set

    var notOnlyHierarchy = false // todo begining to log on 08-01-2019. If nothing by 08-02-2019, start deleting these tasks
        private set

    fun setRelevant(taskRelevances: Map<TaskKey, TaskRelevance>, instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>, customTimeRelevances: Map<Int, LocalCustomTimeRelevance>, now: ExactTimeStamp, notOnlyHierarchy: Boolean = true) {
        this.notOnlyHierarchy = this.notOnlyHierarchy || notOnlyHierarchy
        if (relevant) return

        relevant = true

        val taskKey = task.taskKey

        // mark parents and children relevant
        (task.getTaskHierarchiesByChildTaskKey(taskKey).map { Pair(it, it.parentTaskKey) } + task.getTaskHierarchiesByParentTaskKey(taskKey).map { Pair(it, it.childTaskKey) })
                .forEach { (taskHierarchy, taskKey) ->
                    taskRelevances[taskKey]!!.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now, notOnlyHierarchy && taskHierarchy.notDeleted(now))
                }

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
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now) }

        task.existingInstances
                .values
                .filter { it.scheduleDate >= oldestVisible }
                .map { instanceRelevances[it.instanceKey]!! }
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now) }

        // mark custom times relevant
        task.schedules
                .asSequence()
                .map { it.customTimeKey }
                .filterIsInstance<CustomTimeKey.LocalCustomTimeKey>()
                .map { customTimeRelevances[it.localCustomTimeId]!! }
                .forEach { it.setRelevant() }
    }

    fun setRemoteRelevant(remoteCustomTimeRelevances: Map<kotlin.Pair<String, String>, RemoteCustomTimeRelevance>, remoteProjectRelevances: Map<String, RemoteProjectRelevance>) {
        check(relevant)

        task.schedules
                .mapNotNull { it.remoteCustomTimeKey }
                .map { remoteCustomTimeRelevances[it]!! }
                .forEach { it.setRelevant() }

        task.remoteNullableProject?.let { remoteProjectRelevances[it.id]!!.setRelevant() }
    }
}
