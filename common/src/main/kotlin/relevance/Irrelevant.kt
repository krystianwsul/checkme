package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.schedule.Schedule
import com.krystianwsul.common.firebase.models.schedule.SingleSchedule
import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.models.task.RootTask
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
        getRootTasks: () -> Map<TaskKey.Root, RootTask>,
        userCustomTimeRelevances: Map<CustomTimeKey.User, CustomTimeRelevance>,
        getProjects: () -> Map<ProjectKey<*>, Project<*>>,
        rootTaskProvider: Project.RootTaskProvider,
        now: ExactTimeStamp.Local,
    ): Result {
        val projects = getProjects()
        val tasks = projects.values.flatMap { it.getAllTasks() }

        lateinit var taskRelevances: Map<TaskKey, TaskRelevance>
        lateinit var remoteProjectRelevances: Map<ProjectKey<*>, RemoteProjectRelevance>
        lateinit var irrelevantTasks: List<Task>

        val result = ProjectRootTaskIdTracker.trackRootTaskIds(
            getRootTasks,
            getProjects,
            rootTaskProvider,
        ) {
            tasks.forEach {
                it.correctIntervalEndExactTimeStamps()

                it.intervalInfo
                    .scheduleIntervals
                    .forEach { it.updateOldestVisible(now) }
            }

            // relevant hack
            taskRelevances = tasks.associate { it.taskKey to TaskRelevance(it) }

            val taskHierarchies = projects.values.flatMap { it.taskHierarchies }
            val taskHierarchyRelevances = taskHierarchies.associate { it.taskHierarchyKey to TaskHierarchyRelevance(it) }

            val existingInstances = projects.values.flatMap { it.existingInstances }

            val rootInstances = projects.values
                .flatMap { it.getRootInstances(null, now.toOffset().plusOne(), now) }
                .toList()

            val instanceRelevances = (existingInstances + rootInstances)
                .asSequence()
                .distinct()
                .associate { it.instanceKey to InstanceRelevance(it) }
                .toMutableMap()

            tasks.asSequence()
                .filter { it.notDeleted && it.isTopLevelTask(now) && it.isVisible(now, true) }
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

            irrelevantTasks = tasks - relevantTasks

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
                        if (scheduleInterval.notDeletedOffset() && schedule.notDeleted) {
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
                    .filter { it.notDeletedOffset() }
                    .map { it.noScheduleOrParent }

                irrelevantNoScheduleOrParents += it.noScheduleOrParents - relevantNoScheduleOrParents

                (it as? RootTask)?.let { rootTask ->
                    when (val taskParentEntry = it.getProjectIdTaskParentEntry()) {
                        is Schedule -> {
                            irrelevantSchedules -= taskParentEntry

                            /**
                             * My concern here is that,
                             * 1. We need to keep the schedule, because it hold the project info.
                             * 2. We can't remove the instance, since it'll just get regenerated
                             * 3. Therefore, it should be relevant?
                             */

                            (taskParentEntry as? SingleSchedule)?.getInstance(rootTask)
                                ?.takeIf { it.exists() }
                                ?.takeIf { !instanceRelevances.getValue(it.instanceKey).relevant }
                                ?.let {
                                    throw InstanceIrrelevantForProjectScheduleException(rootTask.taskKey)
                                }
                        }
                        is NoScheduleOrParent -> check(taskParentEntry in relevantNoScheduleOrParents)
                        is NestedTaskHierarchy ->
                            check(taskHierarchyRelevances.getValue(taskParentEntry.taskHierarchyKey).relevant)
                        else -> throw IllegalStateException()
                    }
                }
            }

            ProjectRootTaskIdTracker.checkTracking()

            irrelevantExistingInstances.forEach { it.delete() }
            irrelevantSchedules.forEach { it.delete() }
            irrelevantNoScheduleOrParents.forEach { it.delete() }
            irrelevantTaskHierarchies.forEach { it.delete() }
            irrelevantTasks.forEach { it.delete() }

            val remoteCustomTimes = projects.values.flatMap { it.customTimes }

            val customTimeRelevanceCollection = CustomTimeRelevanceCollection(
                userCustomTimeRelevances,
                remoteCustomTimes.associate { it.key to CustomTimeRelevance(it) },
            )

            projects.values
                .filterIsInstance<PrivateProject>()
                .forEach {
                    it.customTimes
                        .filter { it.notDeleted }
                        .forEach { customTimeRelevanceCollection.getRelevance(it.key).setRelevant() }
                }

            remoteProjectRelevances = projects.mapValues { RemoteProjectRelevance(it.value) }

            projects.values
                .filter { it.notDeleted }
                .forEach { remoteProjectRelevances.getValue(it.projectKey).setRelevant() }

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

                if (!remoteProjectRelevance.relevant) project.delete()
            }

            Result(
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

        remoteProjectRelevances.values.forEach { remoteProjectRelevance ->
            val project = remoteProjectRelevance.project

            if (remoteProjectRelevance.relevant) {
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
            }
        }

        return result
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

    private class InstanceIrrelevantForProjectScheduleException(taskKey: TaskKey) :
        Exception("single schedule instance incorrectly irrelevant for taskKey: $taskKey")
}