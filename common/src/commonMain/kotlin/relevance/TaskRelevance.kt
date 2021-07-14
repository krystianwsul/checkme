package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.schedule.Schedule
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


class TaskRelevance(val task: Task) {

    var relevant = false
        private set

    fun setRelevant(
        taskRelevances: Map<TaskKey, TaskRelevance>,
        taskHierarchyRelevances: Map<TaskHierarchyKey, TaskHierarchyRelevance>,
        instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>,
        now: ExactTimeStamp.Local,
    ) {
        if (relevant) return

        relevant = true

        (task.intervalInfo.parentHierarchyIntervals + task.childHierarchyIntervals)
            .asSequence()
            .filter {
                val hierarchyExactTimeStamp = task.getHierarchyExactTimeStamp(now)

                it.notDeletedOffset(hierarchyExactTimeStamp)
                        && it.taskHierarchy.notDeletedOffset(hierarchyExactTimeStamp)
            }
            .forEach {
                taskHierarchyRelevances.getValue(it.taskHierarchy.taskHierarchyKey).setRelevant(
                    taskRelevances,
                    taskHierarchyRelevances,
                    instanceRelevances,
                    now
                )
            }

        fun Instance.filterOldestVisible(now: ExactTimeStamp.Local, ignoreHidden: Boolean = false): Boolean {
            val oldestVisibles = getOldestVisibles()

            return if (oldestVisibles.isEmpty()) {
                isVisible(now, Instance.VisibilityOptions(hack24 = true, assumeRoot = true))
            } else {
                oldestVisibles.map {
                    when (it) {
                        Schedule.OldestVisible.Single -> isVisible(
                            now,
                            Instance.VisibilityOptions(
                                hack24 = true,
                                ignoreHidden = ignoreHidden,
                                assumeRoot = true
                            )
                        )
                        Schedule.OldestVisible.RepeatingNull -> true
                        is Schedule.OldestVisible.RepeatingNonNull -> scheduleDate >= it.date
                    }
                }.any { it }
            }
        }

        task.existingInstances
            .values
            .filter { it.isRootInstance() && it.filterOldestVisible(now, true) }
            .map { instanceRelevances.getValue(it.instanceKey) }
            .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }
    }

    fun setRemoteRelevant(
        customTimeRelevanceCollection: CustomTimeRelevanceCollection,
        remoteProjectRelevances: Map<ProjectKey<*>, RemoteProjectRelevance>,
    ) {
        check(relevant)

        task.intervalInfo
            .scheduleIntervals
            .mapNotNull { it.schedule.customTimeKey }
            .map { customTimeRelevanceCollection.getRelevance(it) }
            .forEach { it.setRelevant() }

        remoteProjectRelevances.getValue(task.project.projectKey).setRelevant()
    }
}
