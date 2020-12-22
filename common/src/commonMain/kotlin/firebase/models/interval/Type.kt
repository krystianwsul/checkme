package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.firebase.models.TaskParentEntry
import com.krystianwsul.common.utils.ProjectType

sealed class Type<T : ProjectType> {

    open fun matches(taskHierarchy: TaskHierarchy<T>) = false

    abstract val taskParentEntries: Collection<TaskParentEntry>

    data class Child<T : ProjectType>(val parentTaskHierarchy: TaskHierarchy<T>) : Type<T>() {

        override val taskParentEntries get() = listOf(parentTaskHierarchy)

        override fun matches(taskHierarchy: TaskHierarchy<T>) = parentTaskHierarchy == taskHierarchy

        fun getHierarchyInterval(interval: Interval<T>): HierarchyInterval<T> {
            check(parentTaskHierarchy.startExactTimeStampOffset == interval.startExactTimeStampOffset)

            parentTaskHierarchy.endExactTimeStampOffset?.let {
                val intervalEndExactTimeStamp = interval.endExactTimeStampOffset
                checkNotNull(intervalEndExactTimeStamp)
                check(it >= intervalEndExactTimeStamp)
            }

            return HierarchyInterval(
                    interval.startExactTimeStampOffset,
                    interval.endExactTimeStampOffset,
                    parentTaskHierarchy
            )
        }
    }

    data class Schedule<T : ProjectType>(
            private val schedules: List<com.krystianwsul.common.firebase.models.Schedule<T>>
    ) : Type<T>() {

        override val taskParentEntries get() = schedules

        fun getScheduleIntervals(interval: Interval<T>): List<ScheduleInterval<T>> {
            val minStartExactTimeStamp = schedules.map { it.startExactTimeStampOffset }.minOrNull()!!
            check(minStartExactTimeStamp == interval.startExactTimeStampOffset)

            val endExactTimeStamps = schedules.map { it.endExactTimeStampOffset }
            if (endExactTimeStamps.all { it != null }) {
                val intervalEndExactTimeStamp = interval.endExactTimeStampOffset
                checkNotNull(intervalEndExactTimeStamp)

                val maxEndExactTimeStamp = endExactTimeStamps.requireNoNulls().maxOrNull()!!
                check(maxEndExactTimeStamp >= intervalEndExactTimeStamp)
            }

            return schedules.map {
                ScheduleInterval(
                        interval.startExactTimeStampOffset,
                        interval.endExactTimeStampOffset,
                        it
                )
            }
        }
    }

    data class NoSchedule<T : ProjectType>(val noScheduleOrParent: NoScheduleOrParent<T>? = null) : Type<T>() {

        override val taskParentEntries get() = listOfNotNull(noScheduleOrParent)

        fun getNoScheduleOrParentInterval(interval: Interval<T>) = noScheduleOrParent?.let {
            NoScheduleOrParentInterval(
                    interval.startExactTimeStampOffset,
                    interval.endExactTimeStampOffset,
                    it
            )
        }
    }
}