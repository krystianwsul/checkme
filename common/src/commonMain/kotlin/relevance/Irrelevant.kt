package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.soywiz.klock.days

object Irrelevant {

    fun setIrrelevant(
            userCustomTimeRelevances: Map<CustomTimeKey.User, CustomTimeRelevance>,
            parent: Project.Parent,
            project: Project<*>,
            now: ExactTimeStamp.Local,
            delete: Boolean = true,
    ): Result {
        val tasks = project.tasks

        tasks.forEach {
            it.correctIntervalEndExactTimeStamps()

            it.scheduleIntervals.forEach { it.updateOldestVisible(now) }
        }

        // relevant hack
        val taskRelevances = tasks.associate { it.taskKey to TaskRelevance(it) }

        val taskHierarchies = project.taskHierarchies
        val taskHierarchyRelevances = taskHierarchies.associate { it.taskHierarchyKey to TaskHierarchyRelevance(it) }

        val existingInstances = project.existingInstances
        val rootInstances = project.getRootInstances(null, now.toOffset().plusOne(), now).toList()

        val instanceRelevances = (existingInstances + rootInstances)
                .asSequence()
                .distinct()
                .associate { it.instanceKey to InstanceRelevance(it) }
                .toMutableMap()

        val yesterday = ExactTimeStamp.Local(now.toDateTimeSoy() - 1.days)

        // delay deleting removed tasks by a day
        fun getIrrelevantNow(endExactTimeStamp: ExactTimeStamp.Local?) = endExactTimeStamp?.takeIf { it > yesterday }
                ?.minusOne()
                ?: now

        tasks.asSequence()
                .filter {
                    val exactTimeStamp = getIrrelevantNow(it.endExactTimeStamp)

                    it.current(exactTimeStamp)
                            && it.isRootTask(exactTimeStamp)
                            && it.isVisible(exactTimeStamp, true)
                }
                .map { taskRelevances.getValue(it.taskKey) }
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        rootInstances.map { instanceRelevances[it.instanceKey]!! }.forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        existingInstances.asSequence()
                .filter { it.isVisible(now, Instance.VisibilityOptions(hack24 = true)) } // this probably makes the recursive set in tasks redundant
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

        val irrelevantTaskHierarchies = taskHierarchies - relevantTaskHierarchies

        val relevantInstances = instanceRelevances.values
                .filter { it.relevant }
                .map { it.instance }

        val relevantExistingInstances = relevantInstances.filter { it.exists() }
        val irrelevantExistingInstances = existingInstances - relevantExistingInstances

        val irrelevantSchedules = mutableListOf<Schedule<*>>()
        val irrelevantNoScheduleOrParents = mutableListOf<NoScheduleOrParent<*>>()

        relevantTasks.forEach {
            val scheduleIntervals = it.scheduleIntervals

            irrelevantSchedules += it.schedules - scheduleIntervals.map { it.schedule }

            irrelevantSchedules += scheduleIntervals.filter { scheduleInterval ->
                val schedule = scheduleInterval.schedule

                val result = if (schedule is SingleSchedule<*>) {
                    /**
                     * Can't assume the instance is root; it could be joined.  But (I think) the schedule is still
                     * relevant, since removing it would make the task unscheduled.
                     */
                    !schedule.getInstance(it).isVisible(
                            now,
                            Instance.VisibilityOptions(hack24 = true)
                    )
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

            val relevantNoScheduleOrParents = it.noScheduleOrParentIntervals
                    .filter { it.currentOffset(now) }
                    .map { it.noScheduleOrParent }

            irrelevantNoScheduleOrParents += it.noScheduleOrParents - relevantNoScheduleOrParents
        }

        if (delete) {
            irrelevantExistingInstances.forEach { it.delete() }
            irrelevantSchedules.forEach { it.delete() }
            irrelevantNoScheduleOrParents.forEach { it.delete() }
            irrelevantTaskHierarchies.forEach { it.delete() }
            irrelevantTasks.forEach { it.delete() }
        }

        val remoteCustomTimes = project.customTimes

        val customTimeRelevanceCollection = CustomTimeRelevanceCollection(
                userCustomTimeRelevances,
                remoteCustomTimes.associate { it.key to CustomTimeRelevance(it) },
        )

        if (project is PrivateProject) {
            project.customTimes
                    .filter { it.notDeleted(getIrrelevantNow(it.endExactTimeStamp)) }
                    .forEach { customTimeRelevanceCollection.getRelevance(it.key).setRelevant() }
        }

        val remoteProjects = listOf(project)
        val remoteProjectRelevances = remoteProjects.associate { it.projectKey to RemoteProjectRelevance(it) }

        remoteProjects.filter { it.current(getIrrelevantNow(it.endExactTimeStamp)) }
                .map { remoteProjectRelevances.getValue(it.projectKey) }
                .forEach { it.setRelevant() }

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

        if (delete) irrelevantRemoteCustomTimes.forEach { it.delete() }

        val relevantRemoteProjects = remoteProjectRelevances.values
                .filter { it.relevant }
                .map { it.project }

        val irrelevantRemoteProjects = remoteProjects - relevantRemoteProjects

        if (delete) irrelevantRemoteProjects.forEach { it.delete(parent) }

        return Result(
                irrelevantExistingInstances,
                irrelevantTaskHierarchies,
                irrelevantSchedules,
                irrelevantNoScheduleOrParents,
                irrelevantTasks,
                irrelevantRemoteCustomTimes,
                irrelevantRemoteProjects.map { it as SharedProject }
        )
    }

    private class VisibleIrrelevantTasksException(message: String) : Exception(message)

    data class Result(
            val irrelevantExistingInstances: Collection<Instance<*>>,
            val irrelevantTaskHierarchies: Collection<TaskHierarchy<*>>,
            val irrelevantSchedules: Collection<Schedule<*>>,
            val irrelevantNoScheduleOrParents: Collection<NoScheduleOrParent<*>>,
            val irrelevantTasks: Collection<Task<*>>,
            val irrelevantRemoteCustomTimes: Collection<Time.Custom.Project<*>>, // todo customtime migrate
            val removedSharedProjects: Collection<SharedProject>,
    )
}