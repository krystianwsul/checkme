package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.schedule.RepeatingSchedule
import com.krystianwsul.common.firebase.models.schedule.Schedule
import com.krystianwsul.common.firebase.models.schedule.SingleSchedule
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.*


class TaskRelevance(val task: Task) {

    var relevant = false
        private set

    fun setRelevant(
        taskRelevances: Map<TaskKey, TaskRelevance>,
        taskHierarchyRelevances: Map<TaskHierarchyKey, TaskHierarchyRelevance>,
        instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>,
        scheduleRelevances: MutableMap<ScheduleKey, ScheduleRelevance>,
        now: ExactTimeStamp.Local,
        @Suppress("UNUSED_PARAMETER") source: List<String>,
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
                    scheduleRelevances,
                    now,
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
            .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, scheduleRelevances, now) }

        task.intervalInfo
            .scheduleIntervals
            .filter {
                when (val schedule = it.schedule) {
                    is SingleSchedule ->
                        /**
                         * Can't assume the instance is root; it could be joined.  But (I think) the schedule is still
                         * relevant, since removing it would make the task unscheduled.
                         */
                        schedule.getInstance(schedule.topLevelTask)
                            .isVisible(now, Instance.VisibilityOptions(hack24 = true))
                    is RepeatingSchedule -> {
                        if (it.notDeletedOffset() && schedule.notDeleted) {
                            true
                        } else {
                            val oldestVisibleExactTimeStamp = schedule.oldestVisible
                                .date
                                ?.toMidnightExactTimeStamp()

                            val scheduleEndExactTimeStamp = schedule.endExactTimeStampOffset

                            if (oldestVisibleExactTimeStamp != null && scheduleEndExactTimeStamp != null)
                                oldestVisibleExactTimeStamp <= scheduleEndExactTimeStamp
                            else
                                true
                        }
                    }
                    else -> throw UnsupportedOperationException()
                }
            }
            .map { scheduleRelevances.getOrPut(it.schedule) }
            .forEach { it.setRelevant() }

        (task as? RootTask)?.let { it.getProjectIdTaskParentEntry() as? Schedule }
            ?.also { scheduleRelevances.getOrPut(it).setRelevant() }
            ?.let { it as? SingleSchedule }
            ?.let { it.getInstance(it.topLevelTask) }
            ?.takeIf { it.exists() }
            ?.let {
                instanceRelevances.getValue(it.instanceKey).setRelevant(
                    taskRelevances,
                    taskHierarchyRelevances,
                    instanceRelevances,
                    scheduleRelevances,
                    now,
                )
            }
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
