package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.TaskParentEntry
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.utils.ProjectType

sealed class Type<T : ProjectType> {

    open fun matches(taskHierarchy: ProjectTaskHierarchy) = false

    abstract val taskParentEntries: Collection<TaskParentEntry>

    data class Child<T : ProjectType>(val parentTaskHierarchy: TaskHierarchy) : Type<T>() {

        override val taskParentEntries get() = listOf(parentTaskHierarchy)

        override fun matches(taskHierarchy: ProjectTaskHierarchy) = parentTaskHierarchy == taskHierarchy

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
                    parentTaskHierarchy,
            )
        }
    }

    data class Schedule<T : ProjectType>(
            private val schedules: List<com.krystianwsul.common.firebase.models.schedule.Schedule>,
    ) : Type<T>() {

        override val taskParentEntries get() = schedules

        fun getScheduleIntervals(interval: Interval<T>) = schedules.map {
            ScheduleInterval<T>(interval.startExactTimeStampOffset, interval.endExactTimeStampOffset, it)
        }
    }

    data class NoSchedule<T : ProjectType>(val noScheduleOrParent: NoScheduleOrParent? = null) : Type<T>() {

        override val taskParentEntries get() = listOfNotNull(noScheduleOrParent)

        fun getNoScheduleOrParentInterval(interval: Interval<T>) = noScheduleOrParent?.let {
            NoScheduleOrParentInterval<T>(
                    interval.startExactTimeStampOffset,
                    interval.endExactTimeStampOffset,
                    it,
            )
        }
    }
}