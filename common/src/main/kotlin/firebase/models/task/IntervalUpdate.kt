package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.domain.TaskUndoData
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

    fun setEndData(
        // this is not recursive on children.  Get the whole tree beforehand.
        endData: Task.EndData,
        taskUndoData: TaskUndoData? = null,
        recursive: Boolean = false,
    ) {
        val now = endData.exactTimeStampLocal

        task.requireNotDeleted()

        /**
         * Need cached value, since Schedule.setEndExactTimeStamp will invalidate it.  It would be better to do this in
         * IntervalUpdate, but that's supposed to apply only to RootTasks.
         */
        val intervalInfo = intervalInfo

        val scheduleIds = intervalInfo.getCurrentScheduleIntervals(now)
            .map {
                it.requireCurrentOffset(now)

                it.schedule.setEndExactTimeStamp(now.toOffset())

                it.schedule.id
            }
            .toSet()

        taskUndoData?.taskKeys?.put(task.taskKey, scheduleIds)

        if (!recursive) {
            intervalInfo.getParentTaskHierarchy(now)?.let {
                it.requireCurrentOffset(now)
                it.taskHierarchy.requireNotDeleted()

                taskUndoData?.taskHierarchyKeys?.add(it.taskHierarchy.taskHierarchyKey)

                it.taskHierarchy.setEndExactTimeStamp(now)
            }
        }

        task.setMyEndExactTimeStamp(endData)
    }

    fun clearEndExactTimeStamp() {
        task.requireDeleted()

        task.setMyEndExactTimeStamp(null)
    }
}