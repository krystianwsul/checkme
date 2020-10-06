package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.firebase.models.TaskParentEntry
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

sealed class Type<T : ProjectType> {

    open fun matches(taskHierarchy: TaskHierarchy<T>) = false

    abstract val taskParentEntries: Collection<TaskParentEntry>

    data class Child<T : ProjectType>(val parentTaskHierarchy: TaskHierarchy<T>) : Type<T>() {

        override val taskParentEntries get() = listOf(parentTaskHierarchy)

        override fun matches(taskHierarchy: TaskHierarchy<T>) = parentTaskHierarchy == taskHierarchy

        fun getHierarchyInterval(interval: Interval<T>): HierarchyInterval<T> {
            if (parentTaskHierarchy.isParentGroupTask(ExactTimeStamp.now)) {
                /*
                I'm allowing this inconsistency because the only place this should be relevant is
                for tasks that are children of a group task.
                 */
                check(interval is Interval.Current)
                check(parentTaskHierarchy.startExactTimeStamp >= interval.startExactTimeStamp)
            } else {
                check(parentTaskHierarchy.startExactTimeStamp == interval.startExactTimeStamp)
            }

            parentTaskHierarchy.endExactTimeStamp?.let {
                val intervalEndExactTimeStamp = interval.endExactTimeStamp
                checkNotNull(intervalEndExactTimeStamp)
                check(it >= intervalEndExactTimeStamp)
            }

            return HierarchyInterval(
                    interval.startExactTimeStamp,
                    interval.endExactTimeStamp,
                    parentTaskHierarchy
            )
        }
    }

    data class Schedule<T : ProjectType>(
            private val schedules: List<com.krystianwsul.common.firebase.models.Schedule<T>>
    ) : Type<T>() {

        override val taskParentEntries get() = schedules

        fun getScheduleIntervals(interval: Interval<T>): List<ScheduleInterval<T>> {
            val minStartExactTimeStamp = schedules.map { it.startExactTimeStamp }.minOrNull()!!
            check(minStartExactTimeStamp == interval.startExactTimeStamp)

            val endExactTimeStamps = schedules.map { it.endExactTimeStamp }
            if (endExactTimeStamps.all { it != null }) {
                val intervalEndExactTimeStamp = interval.endExactTimeStamp
                checkNotNull(intervalEndExactTimeStamp)

                val maxEndExactTimeStamp = endExactTimeStamps.requireNoNulls().maxOrNull()!!
                check(maxEndExactTimeStamp >= intervalEndExactTimeStamp)
            }

            return schedules.map {
                ScheduleInterval(
                        interval.startExactTimeStamp,
                        interval.endExactTimeStamp,
                        it
                )
            }
        }
    }

    data class NoSchedule<T : ProjectType>(val noScheduleOrParent: NoScheduleOrParent<T>? = null) : Type<T>() {

        override val taskParentEntries get() = listOfNotNull(noScheduleOrParent)

        fun getNoScheduleOrParentInterval(interval: Interval<T>) = noScheduleOrParent?.let {
            NoScheduleOrParentInterval(
                    interval.startExactTimeStamp,
                    interval.endExactTimeStamp,
                    it
            )
        }
    }
}