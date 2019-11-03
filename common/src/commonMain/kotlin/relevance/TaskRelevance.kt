package com.krystianwsul.common.relevance

import com.krystianwsul.common.domain.Instance
import com.krystianwsul.common.domain.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


class TaskRelevance(val task: Task) {

    var relevant = false
        private set

    fun setRelevant(
            taskRelevances: Map<TaskKey, TaskRelevance>,
            taskHierarchyRelevances: Map<TaskHierarchyKey, TaskHierarchyRelevance>,
            instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>,
            now: ExactTimeStamp) {
        if (relevant)
            return

        relevant = true

        (task.getParentTaskHierarchies() + task.getChildTaskHierarchies())
                .filter {
                    val hierarchyExactTimeStamp = task.getHierarchyExactTimeStamp(now)
                    it.notDeleted(hierarchyExactTimeStamp)
                }
                .forEach { taskHierarchyRelevances.getValue(it.taskHierarchyKey).setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        val oldestVisible = task.getOldestVisible()

        fun Instance.filterOldestVisible() = oldestVisible?.let { scheduleDate >= it } ?: true

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

    fun setRemoteRelevant(remoteCustomTimeRelevances: Map<Pair<String, RemoteCustomTimeId>, RemoteCustomTimeRelevance>, remoteProjectRelevances: Map<String, RemoteProjectRelevance>) {
        check(relevant)

        task.schedules
                .mapNotNull { it.remoteCustomTimeKey }
                .map { remoteCustomTimeRelevances.getValue(it) }
                .forEach { it.setRelevant() }

        remoteProjectRelevances.getValue(task.project.id).setRelevant()
    }
}
