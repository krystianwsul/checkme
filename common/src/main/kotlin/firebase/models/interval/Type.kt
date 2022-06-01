package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.TaskParentEntry
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy

sealed class Type {

    open fun matches(taskHierarchy: ProjectTaskHierarchy) = false

    abstract val taskParentEntries: Collection<TaskParentEntry>

    data class Child(val parentTaskHierarchy: TaskHierarchy) : Type() {

        override val taskParentEntries get() = listOf(parentTaskHierarchy)

        override fun matches(taskHierarchy: ProjectTaskHierarchy) = parentTaskHierarchy == taskHierarchy

        fun getHierarchyInterval(interval: Interval): HierarchyInterval {
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

    data class Schedule(
            private val schedules: List<com.krystianwsul.common.firebase.models.schedule.Schedule>,
    ) : Type() {

        override val taskParentEntries get() = schedules

        fun getScheduleIntervals(interval: Interval) = schedules.map {
            ScheduleInterval(interval.startExactTimeStampOffset, interval.endExactTimeStampOffset, it)
        }

        fun getParentProjectSchedule() = taskParentEntries.sortedWith(
            compareByDescending<com.krystianwsul.common.firebase.models.schedule.Schedule> { it.startExactTimeStamp }.thenByDescending { it.id }
        ).first()
    }

    data class NoSchedule(val noScheduleOrParent: NoScheduleOrParent? = null) : Type() {

        override val taskParentEntries get() = listOfNotNull(noScheduleOrParent)

        fun getNoScheduleOrParentInterval(interval: Interval) = noScheduleOrParent?.let {
            NoScheduleOrParentInterval(
                    interval.startExactTimeStampOffset,
                    interval.endExactTimeStampOffset,
                    it,
            )
        }
    }
}