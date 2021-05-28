package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.interval.IntervalInfo
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.schedule.Schedule
import com.krystianwsul.common.firebase.models.schedule.SingleSchedule
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.UserKey

class IntervalUpdate(val task: RootTask, val intervalInfo: IntervalInfo) {

    var invalidateIntervals = false

    fun invalidateIntervals() {
        invalidateIntervals = true
    }

    fun endAllCurrentTaskHierarchies(now: ExactTimeStamp.Local) = task.parentTaskHierarchies
        .filter { it.currentOffset(now) }
        .onEach { it.setEndExactTimeStamp(now) }
        .map { it.taskHierarchyKey }

    fun endAllCurrentSchedules(now: ExactTimeStamp.Local) = task.schedules
        .filter { it.currentOffset(now) }
        .onEach { it.setEndExactTimeStamp(now.toOffset()) }
        .map { it.id }

    fun endAllCurrentNoScheduleOrParents(now: ExactTimeStamp.Local) = task.noScheduleOrParents
        .filter { it.currentOffset(now) }
        .onEach { it.setEndExactTimeStamp(now.toOffset()) }
        .map { it.id }

    private data class ScheduleDiffKey(val scheduleData: ScheduleData, val assignedTo: Set<UserKey>)

    fun updateSchedules(
        shownFactory: Instance.ShownFactory,
        scheduleDatas: List<Pair<ScheduleData, Time>>,
        now: ExactTimeStamp.Local,
        assignedTo: Set<UserKey>,
        customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        projectKey: ProjectKey<*>,
    ) {
        val removeSchedules = mutableListOf<Schedule>()
        val addScheduleDatas = scheduleDatas.map { ScheduleDiffKey(it.first, assignedTo) to it }.toMutableList()

        val oldSchedules = intervalInfo.getCurrentScheduleIntervals(now).map { it.schedule }

        val oldScheduleDatas = ScheduleGroup.getGroups(oldSchedules).map {
            ScheduleDiffKey(it.scheduleData, it.assignedTo) to it.schedules
        }

        for ((key, value) in oldScheduleDatas) {
            val existing = addScheduleDatas.singleOrNull { it.first == key }

            if (existing != null)
                addScheduleDatas.remove(existing)
            else
                removeSchedules.addAll(value)
        }

        /*
            requirements for mock:
                there was one old schedule, it was single and mocked, and it's getting replaced
                by another single schedule
         */

        val singleRemoveSchedule = removeSchedules.singleOrNull() as? SingleSchedule

        val singleAddSchedulePair = addScheduleDatas.singleOrNull()?.takeIf {
            it.first.scheduleData is ScheduleData.Single
        }

        if (singleRemoveSchedule != null && singleAddSchedulePair != null) {
            if (assignedTo.isNotEmpty()) singleRemoveSchedule.setAssignedTo(assignedTo)

            singleRemoveSchedule.getInstance(task).setInstanceDateTime(
                shownFactory,
                singleAddSchedulePair.second.run { DateTime((first as ScheduleData.Single).date, second) },
                customTimeMigrationHelper,
                now,
            )
        } else {
            removeSchedules.forEach { it.setEndExactTimeStamp(now.toOffset()) }

            task.createSchedules(
                now,
                addScheduleDatas.map { it.second },
                assignedTo,
                customTimeMigrationHelper,
                projectKey,
            )
        }
    }
}

private val intervalUpdates = mutableMapOf<Task, IntervalUpdate>()

fun RootTask.performIntervalUpdate(action: IntervalUpdate.() -> Unit) {
    checkNoIntervalUpdate()

    val intervalUpdate = IntervalUpdate(this, intervalInfo)
    intervalUpdates[this] = intervalUpdate

    try {
        intervalUpdate.action()
    } finally {
        check(intervalUpdates.containsKey(this))

        intervalUpdates.remove(this)
    }

    if (intervalUpdate.invalidateIntervals) intervalInfoProperty.invalidate()
}

fun Task.checkNoIntervalUpdate() = check(!intervalUpdates.containsKey(this))

fun Task.getIntervalUpdate() = intervalUpdates[this]