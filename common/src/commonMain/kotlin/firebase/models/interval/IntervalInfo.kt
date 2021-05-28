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
        try {
            return intervals.single {
                it.containsExactTimeStamp(exactTimeStamp)
            }
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

    private class IntervalException(message: String, cause: Throwable) : Exception(message, cause)
}