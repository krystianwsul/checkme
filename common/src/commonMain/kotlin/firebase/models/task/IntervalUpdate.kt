package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.models.interval.IntervalInfo
import com.krystianwsul.common.time.ExactTimeStamp

open class IntervalUpdate(private val task: Task, protected val intervalInfo: IntervalInfo) {

    var intervalsInvalid = false
        private set

    fun invalidateIntervals() {
        intervalsInvalid = true
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
}