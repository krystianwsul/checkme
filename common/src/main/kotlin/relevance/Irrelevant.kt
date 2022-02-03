package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.schedule.Schedule
import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.NestedTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.firebase.models.users.RootUser
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleKey
import com.krystianwsul.common.utils.TaskKey

object Irrelevant {

    fun setIrrelevant(
        getRootTasks: () -> Map<TaskKey.Root, RootTask>,
        userCustomTimeRelevances: Map<CustomTimeKey.User, CustomTimeRelevance>,
        getProjects: () -> Map<ProjectKey<*>, Project<*>>,
        rootTaskProvider: Project.RootTaskProvider,
        now: ExactTimeStamp.Local,
        users: Collection<RootUser>,
    ): Result {
        val projects = getProjects()
        val tasks = projects.values.flatMap { it.getAllDependenciesLoadedTasks() }

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

            val scheduleRelevances = mutableMapOf<ScheduleKey, ScheduleRelevance>()

            tasks.asSequence()
                .filter { it.notDeleted && it.isTopLevelTask() && it.isVisible(now, true) }
                .map { taskRelevances.getValue(it.taskKey) }
                .forEach {
                    it.setRelevant(
                        taskRelevances,
                        taskHierarchyRelevances,
                        instanceRelevances,
                        scheduleRelevances,
                        now,
                        listOf("visible task"),
                    )
                }

            rootInstances.map { instanceRelevances.getValue(it.instanceKey) }.forEach {
                it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, scheduleRelevances, now)
            }

            existingInstances.asSequence()
                // this probably makes the recursive set in tasks redundant
                .filter { it.isVisible(now, Instance.VisibilityOptions(hack24 = true)) }
                .map { instanceRelevances.getValue(it.instanceKey) }
                .forEach {
                    it.setRelevant(
                        taskRelevances,
                        taskHierarchyRelevances,
                        instanceRelevances,
                        scheduleRelevances,
                        now,
                    )
                }

            val relevantTaskRelevances = taskRelevances.values.filter { it.relevant }
            val relevantTasks = relevantTaskRelevances.map { it.task }.toSet()

            irrelevantTasks = tasks - relevantTasks

            val visibleIrrelevantTasks = irrelevantTasks.filter { it.isVisible(now, true) }
            if (visibleIrrelevantTasks.isNotEmpty()) {
                throw VisibleIrrelevantTasksException(
                    visibleIrrelevantTasks.joinToString(", ") { it.taskKey.toString() }
                )
            }

            val relevantTaskHierarchyRelevances = taskHierarchyRelevances.values.filter { it.relevant }
            val relevantTaskHierarchies = relevantTaskHierarchyRelevances.map { it.taskHierarchy }.toSet()

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
                    else -> throw UnsupportedOperationException() // oh for fuck's sake, this still happens in 1.6.0
                }
            }

            val relevantInstances = instanceRelevances.values
                .filter { it.relevant }
                .map { it.instance }
                .toSet()

            val relevantExistingInstances = relevantInstances.filter { it.exists() }.toSet()
            val irrelevantExistingInstances = existingInstances - relevantExistingInstances

            val irrelevantNoScheduleOrParents = mutableListOf<NoScheduleOrParent>()
            relevantTasks.forEach {
                val relevantNoScheduleOrParents = it.intervalInfo
                    .noScheduleOrParentIntervals
                    .filter { it.notDeletedOffset() }
                    .map { it.noScheduleOrParent }
                    .toSet()

                irrelevantNoScheduleOrParents += it.noScheduleOrParents - relevantNoScheduleOrParents

                (it as? RootTask)?.let {
                    when (val taskParentEntry = it.getProjectIdTaskParentEntry()) {
                        // schedules handled elsewhere
                        is NoScheduleOrParent -> check(taskParentEntry in relevantNoScheduleOrParents)
                        is NestedTaskHierarchy ->
                            check(taskHierarchyRelevances.getValue(taskParentEntry.taskHierarchyKey).relevant)
                    }
                }
            }

            ProjectRootTaskIdTracker.checkTracking()

            val relevantSchedules = scheduleRelevances.values
                .filter { it.relevant }
                .map { it.schedule }
                .toSet()

            val irrelevantSchedules = relevantTasks.flatMap { it.schedules } - relevantSchedules

            val projectCustomTimes = projects.values.flatMap { it.customTimes }

            val customTimeRelevanceCollection = CustomTimeRelevanceCollection(
                userCustomTimeRelevances,
                projectCustomTimes.associate { it.key to CustomTimeRelevance(it) },
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

            val relevantProjectCustomTimes = customTimeRelevanceCollection.projectCustomTimeRelevances
                .values
                .filter { it.relevant }
                .map { it.customTime as Time.Custom.Project<*> }
                .toSet()

            val irrelevantProjectCustomTimes = projectCustomTimes - relevantProjectCustomTimes

            val relevantProjects = remoteProjectRelevances.values
                .filter { it.relevant }
                .map { it.project }
                .toSet()

            val irrelevantProjects = (projects.values - relevantProjects).map { it as SharedProject }

            irrelevantExistingInstances.forEach { it.delete() }
            irrelevantSchedules.forEach { it.delete() }
            irrelevantNoScheduleOrParents.forEach { it.delete() }
            irrelevantTaskHierarchies.forEach { it.delete() }
            irrelevantTasks.forEach { it.delete() }

            // we want this to run after everything from the task down is deleted, but before custom times
            OrdinalProcessor(
                users,
                relevantProjects.filterIsInstance<SharedProject>().associateBy { it.projectKey },
                relevantTasks.associateBy { it.taskKey },
                customTimeRelevanceCollection,
                now,
            ).process()

            irrelevantProjectCustomTimes.forEach { it.delete() }
            irrelevantProjects.forEach { it.delete() }

            Result(
                irrelevantExistingInstances,
                irrelevantTaskHierarchies,
                irrelevantSchedules,
                irrelevantNoScheduleOrParents,
                irrelevantTasks,
                irrelevantProjectCustomTimes,
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
}