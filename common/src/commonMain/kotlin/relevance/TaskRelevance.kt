package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.*


class TaskRelevance(val task: Task<*>) {

    var relevant = false
        private set

    fun setRelevant(
            taskRelevances: Map<TaskKey, TaskRelevance>,
            taskHierarchyRelevances: Map<TaskHierarchyKey, TaskHierarchyRelevance>,
            instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>,
            now: ExactTimeStamp
    ) {
        if (relevant)
            return

        relevant = true

        (task.parentTaskHierarchies + task.getChildTaskHierarchies())
                .filter {
                    val hierarchyExactTimeStamp = task.getHierarchyExactTimeStamp(now)
                    it.notDeleted(hierarchyExactTimeStamp)
                }
                .forEach { taskHierarchyRelevances.getValue(it.taskHierarchyKey).setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        val oldestVisible = task.getOldestVisible()

        fun Instance<*>.filterOldestVisible() = oldestVisible?.let { scheduleDate >= it } ?: true

        // mark instances relevant
        task.getPastRootInstances(now)
                .asSequence()
                .filter { it.filterOldestVisible() }
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
                .filter { it.filterOldestVisible() || it.hidden }
                .map { instanceRelevances[it.instanceKey]!! }
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }
    }

    fun setRemoteRelevant(
            remoteCustomTimeRelevances: Map<CustomTimeKey<*>, RemoteCustomTimeRelevance>,
            remoteProjectRelevances: Map<ProjectKey<*>, RemoteProjectRelevance>
    ) {
        check(relevant)

        task.scheduleIntervals
                .mapNotNull { it.schedule.customTimeKey }
                .map { remoteCustomTimeRelevances.getValue(it) }
                .forEach { it.setRelevant() }

        remoteProjectRelevances.getValue(task.project.projectKey).setRelevant()
    }
}
