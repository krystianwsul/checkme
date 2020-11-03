package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Schedule
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

        (task.parentHierarchyIntervals + task.childHierarchyIntervals)
                .asSequence()
                .filter {
                    val hierarchyExactTimeStamp = task.getHierarchyExactTimeStamp(now)

                    it.notDeleted(hierarchyExactTimeStamp) && it.taskHierarchy.notDeleted(hierarchyExactTimeStamp)
                }
                .forEach {
                    taskHierarchyRelevances.getValue(it.taskHierarchy.taskHierarchyKey).setRelevant(
                            taskRelevances,
                            taskHierarchyRelevances,
                            instanceRelevances,
                            now
                    )
                }

        fun Instance<*>.filterOldestVisible(now: ExactTimeStamp): Boolean {
            val oldestVisibles = getOldestVisibles()

            return if (oldestVisibles.isEmpty()) {
                isVisible(now, true)
            } else {
                oldestVisibles.map {
                    when (it) {
                        Schedule.OldestVisible.Single -> isVisible(now, true)
                        Schedule.OldestVisible.RepeatingNull -> true
                        is Schedule.OldestVisible.RepeatingNonNull -> scheduleDate >= it.date
                    }
                }.any { it }
            }
        }

        // mark instances relevant
        task.getPastRootInstances(now)
                .asSequence()
                .filter { it.filterOldestVisible(now) }
                .map { instance ->
                    val instanceKey = instance.instanceKey

                    if (!instanceRelevances.containsKey(instanceKey))
                        instanceRelevances[instanceKey] = InstanceRelevance(instance)

                    instanceRelevances.getValue(instanceKey)
                }
                .toList()
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        task.existingInstances
                .values
                .filter { it.filterOldestVisible(now) || it.hidden }
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
