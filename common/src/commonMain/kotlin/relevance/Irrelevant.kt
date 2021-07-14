package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.schedule.Schedule
import com.krystianwsul.common.firebase.models.schedule.SingleSchedule
import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.NestedTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

object Irrelevant {

    fun setIrrelevant(
        userCustomTimeRelevances: Map<CustomTimeKey.User, CustomTimeRelevance>,
        projects: Collection<Project<*>>,
        now: ExactTimeStamp.Local,
    ): Result {
        val tasks = projects.flatMap { it.getAllTasks() }

        tasks.forEach {
            it.correctIntervalEndExactTimeStamps()

            it.intervalInfo
                .scheduleIntervals
                .forEach { it.updateOldestVisible(now) }
        }

        // relevant hack
        val taskRelevances = tasks.associate { it.taskKey to TaskRelevance(it) }

        val taskHierarchies = projects.flatMap { it.taskHierarchies }
        val taskHierarchyRelevances = taskHierarchies.associate { it.taskHierarchyKey to TaskHierarchyRelevance(it) }

        val existingInstances = projects.flatMap { it.existingInstances }

        val rootInstances = projects.flatMap {
            it.getRootInstances(null, now.toOffset().plusOne(), now)
        }.toList()

        val instanceRelevances = (existingInstances + rootInstances)
            .asSequence()
            .distinct()
            .associate { it.instanceKey to InstanceRelevance(it) }
            .toMutableMap()

        tasks.asSequence()
            .filter { it.current(now) && it.isTopLevelTask(now) && it.isVisible(now, true) }
            .map { taskRelevances.getValue(it.taskKey) }
            .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        rootInstances.map { instanceRelevances.getValue(it.instanceKey) }.forEach {
            it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now)
        }

        existingInstances.asSequence()
            // this probably makes the recursive set in tasks redundant
            .filter { it.isVisible(now, Instance.VisibilityOptions(hack24 = true)) }
            .map { instanceRelevances.getValue(it.instanceKey) }
            .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        val relevantTaskRelevances = taskRelevances.values.filter { it.relevant }
        val relevantTasks = relevantTaskRelevances.map { it.task }

        val irrelevantTasks = tasks - relevantTasks

        val visibleIrrelevantTasks = irrelevantTasks.filter { it.isVisible(now, true) }
        if (visibleIrrelevantTasks.isNotEmpty()) {
            throw VisibleIrrelevantTasksException(
                visibleIrrelevantTasks.joinToString(", ") { it.taskKey.toString() }
            )
        }

        val relevantTaskHierarchyRelevances = taskHierarchyRelevances.values.filter { it.relevant }
        val relevantTaskHierarchies = relevantTaskHierarchyRelevances.map { it.taskHierarchy }

        /**
         * The first is removed normally.  The second is for nested task hierarchies, inside tasks that will also be deleted.
         * We don't want to, uh, double-delete them, but we do need to remove Project.rootTaskIds entries.
         *
         * Update: no longer doing that second part
         */
        val irrelevantTaskHierarchies = (taskHierarchies - relevantTaskHierarchies).filter {
            when (it) {
                is ProjectTaskHierarchy -> true
                is NestedTaskHierarchy -> taskRelevances.getValue(it.childTaskKey).relevant
                else -> throw UnsupportedOperationException() // compilation
            }
        }

        val relevantInstances = instanceRelevances.values
            .filter { it.relevant }
            .map { it.instance }

        val relevantExistingInstances = relevantInstances.filter { it.exists() }
        val irrelevantExistingInstances = existingInstances - relevantExistingInstances

        val irrelevantSchedules = mutableListOf<Schedule>()
        val irrelevantNoScheduleOrParents = mutableListOf<NoScheduleOrParent>()

        relevantTasks.forEach {
            val scheduleIntervals = it.intervalInfo.scheduleIntervals

            irrelevantSchedules += it.schedules - scheduleIntervals.map { it.schedule }

            irrelevantSchedules += scheduleIntervals.filter { scheduleInterval ->
                val schedule = scheduleInterval.schedule

                val result = if (schedule is SingleSchedule) {
                    /**
                     * Can't assume the instance is root; it could be joined.  But (I think) the schedule is still
                     * relevant, since removing it would make the task unscheduled.
                     */
                    !schedule.getInstance(it).isVisible(now, Instance.VisibilityOptions(hack24 = true))
                } else {
                    if (scheduleInterval.currentOffset(now) && schedule.current(now)) {
                        false
                    } else {
                        val oldestVisibleExactTimeStamp = schedule.oldestVisible
                            .date
                            ?.toMidnightExactTimeStamp()

                        val scheduleEndExactTimeStamp = schedule.endExactTimeStampOffset

                        if (oldestVisibleExactTimeStamp != null && scheduleEndExactTimeStamp != null)
                            oldestVisibleExactTimeStamp > scheduleEndExactTimeStamp
                        else
                            false
                    }
                }

                result
            }.map { it.schedule }

            val relevantNoScheduleOrParents = it.intervalInfo
                .noScheduleOrParentIntervals
                .filter { it.currentOffset(now) }
                .map { it.noScheduleOrParent }

            irrelevantNoScheduleOrParents += it.noScheduleOrParents - relevantNoScheduleOrParents
        }

        // todo root check wrapped
        ProjectRootTaskIdTracker.checkTracking()

        irrelevantExistingInstances.forEach { it.delete() }
        irrelevantSchedules.forEach { it.delete() }
        irrelevantNoScheduleOrParents.forEach { it.delete() }
        irrelevantTaskHierarchies.forEach { it.delete() }
        irrelevantTasks.forEach { it.delete() }

        val remoteCustomTimes = projects.flatMap { it.customTimes }

        val customTimeRelevanceCollection = CustomTimeRelevanceCollection(
            userCustomTimeRelevances,
            remoteCustomTimes.associate { it.key to CustomTimeRelevance(it) },
        )

        projects.filterIsInstance<PrivateProject>().forEach {
            it.customTimes
                .filter { it.notDeleted(now) }
                .forEach { customTimeRelevanceCollection.getRelevance(it.key).setRelevant() }
        }

        val remoteProjectRelevances = projects.associate { it.projectKey to RemoteProjectRelevance(it) }

        projects.filter { it.current(now) }.forEach { remoteProjectRelevances.getValue(it.projectKey).setRelevant() }

        taskRelevances.values
            .filter { it.relevant }
            .forEach { it.setRemoteRelevant(customTimeRelevanceCollection, remoteProjectRelevances) }

        instanceRelevances.values
            .filter { it.relevant }
            .forEach { it.setRemoteRelevant(customTimeRelevanceCollection, remoteProjectRelevances) }

        val relevantRemoteCustomTimes = customTimeRelevanceCollection.projectCustomTimeRelevances
            .values
            .filter { it.relevant }
            .map { it.customTime as Time.Custom.Project<*> }

        val irrelevantRemoteCustomTimes = remoteCustomTimes - relevantRemoteCustomTimes

        irrelevantRemoteCustomTimes.forEach { it.delete() }

        remoteProjectRelevances.values.forEach { remoteProjectRelevance ->
            val project = remoteProjectRelevance.project

            if (remoteProjectRelevance.relevant) {
                // todo check wrapped

                project.projectRecord
                    .rootTaskParentDelegate
                    .rootTaskKeys
                    .forEach { taskKey ->
                        if (irrelevantTasks.any { it.taskKey == taskKey })
                            throw TaskInIrrelevantException(taskKey, project.projectKey)

                        val taskRelevance =
                            taskRelevances[taskKey] ?: throw MissingRelevanceException(taskKey, project.projectKey)

                        if (!taskRelevance.relevant) throw TaskIrrelevantException(taskKey, project.projectKey)
                    }
            } else {
                project.delete()
            }
        }

        return Result(
            irrelevantExistingInstances,
            irrelevantTaskHierarchies,
            irrelevantSchedules,
            irrelevantNoScheduleOrParents,
            irrelevantTasks,
            irrelevantRemoteCustomTimes,
            remoteProjectRelevances.values
                .filter { !it.relevant }
                .map { it.project as SharedProject },
        )
    }

    private class VisibleIrrelevantTasksException(message: String) : Exception(message)

    data class Result(
        val irrelevantExistingInstances: Collection<Instance>,
        val irrelevantTaskHierarchies: Collection<TaskHierarchy>,
        val irrelevantSchedules: Collection<Schedule>,
        val irrelevantNoScheduleOrParents: Collection<NoScheduleOrParent>,
        val irrelevantTasks: Collection<Task>,
        val irrelevantRemoteCustomTimes: Collection<Time.Custom.Project<*>>,
        val removedSharedProjects: List<SharedProject>,
    )

    private class MissingRelevanceException(taskKey: TaskKey, projectKey: ProjectKey<*>) :
        Exception("missing relevance for $taskKey in $projectKey")

    private class TaskIrrelevantException(taskKey: TaskKey, projectKey: ProjectKey<*>) :
        Exception("task incorrectly relevant: $taskKey in $projectKey")

    private class TaskInIrrelevantException(taskKey: TaskKey, projectKey: ProjectKey<*>) :
        Exception("task incorrectly irrelevant: $taskKey in $projectKey")
}