package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.ExactTimeStamp

class IntervalInfo(val task: Task, val intervals: List<Interval>) {

    val scheduleIntervals by lazy {
        intervals.mapNotNull { (it.type as? Type.Schedule)?.getScheduleIntervals(it) }.flatten()
    }

    val parentHierarchyIntervals
        get() = intervals.mapNotNull { (it.type as? Type.Child)?.getHierarchyInterval(it) }

    val noScheduleOrParentIntervals
        get() = intervals.mapNotNull { (it.type as? Type.NoSchedule)?.getNoScheduleOrParentInterval(it) }

    fun getInterval(exactTimeStamp: ExactTimeStamp): Interval {
        return try {
            intervals.single { it.containsExactTimeStamp(exactTimeStamp) }
        } catch (throwable: Throwable) {
            throw IntervalException(
                "error getting interval for task ${task.name}. exactTimeStamp: $exactTimeStamp, intervals:\n"
                        + intervals.joinToString("\n") {
                    "${it.startExactTimeStampOffset} - ${it.endExactTimeStampOffset}"
                },
                throwable
            )
        }
    }

    val currentScheduleIntervals by lazy {
        intervals.last().let { interval ->
            interval.type
                .let { it as? Type.Schedule }
                ?.getScheduleIntervals(interval)
                .orEmpty()
                .filter { it.schedule.notDeleted }
        }
    }

    fun getCurrentScheduleIntervals(exactTimeStamp: ExactTimeStamp): List<ScheduleInterval> {
        task.requireCurrentOffset(exactTimeStamp)

        return getInterval(exactTimeStamp).let {
            (it.type as? Type.Schedule)?.getScheduleIntervals(it)
                ?.filter { it.schedule.currentOffset(exactTimeStamp) }
                ?: listOf()
        }
    }

    fun getParentTaskHierarchy(exactTimeStamp: ExactTimeStamp): HierarchyInterval? {
        task.requireCurrentOffset(exactTimeStamp)

        return getInterval(exactTimeStamp).let { (it.type as? Type.Child)?.getHierarchyInterval(it) }
    }

    fun isUnscheduled() = intervals.last().type is Type.NoSchedule

    private class IntervalException(message: String, cause: Throwable) : Exception(message, cause)
}