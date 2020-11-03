package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.models.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

object IntervalBuilder {

    /*
     Note: this will return NoSchedule for the time spans that were covered by irrelevant schedules
     and task hierarchies.  These periods, by definition, shouldn't be needed for anything.
     */
    fun <T : ProjectType> build(task: Task<T>): List<Interval<T>> {
        val now = ExactTimeStamp.now

        val schedules = task.schedules.toMutableList()

        val groupParentTaskHierarchies = task.parentTaskHierarchies
                .filter { it.isParentGroupTask(now) }
                .toMutableList()

        val normalParentTaskHierarchies = (task.parentTaskHierarchies - groupParentTaskHierarchies).toMutableList()

        val noScheduleOrParents = task.noScheduleOrParents.toMutableList()

        fun getNextTypeBuilder(): TypeBuilder<T>? {
            val nextSchedule = schedules.minByOrNull { it.startExactTimeStampOffset }?.let { TypeBuilder.Schedule(it) }

            val nextParentTaskHierarchy = normalParentTaskHierarchies.minByOrNull {
                it.startExactTimeStampOffset
            }?.let { TypeBuilder.Parent(it) }

            val nextNoScheduleOrParent = noScheduleOrParents.minByOrNull {
                it.startExactTimeStampOffset
            }?.let { TypeBuilder.NoScheduleOrParent(it) }

            return listOfNotNull(
                    nextNoScheduleOrParent,
                    nextParentTaskHierarchy,
                    nextSchedule
            ).minByOrNull { it.startExactTimeStampOffset }?.also {
                when (it) {
                    is TypeBuilder.NoScheduleOrParent -> noScheduleOrParents.remove(it.noScheduleOrParent)
                    is TypeBuilder.Parent -> normalParentTaskHierarchies.remove(it.parentTaskHierarchy)
                    is TypeBuilder.Schedule -> schedules.remove(it.schedule)
                }
            }
        }

        val taskStartExactTimeStampOffset = task.startExactTimeStampOffset
        val taskEndExactTimeStampOffset = task.endExactTimeStampOffset

        fun getCurrentPlaceholder(startExactTimeStampOffset: ExactTimeStamp): Interval.Current<T> {
            /*
                since the only way for a task to not have a NoScheduleOrParentRecord is
                    1. it's been garbage collected, so it's really a moot point, and
                    2. the task was created directly as a child of a group task,
                I think group task hierarchies are relevant only to avoid displaying #2
                children as unscheduled root tasks, which is what I fix here.
             */

            val type = groupParentTaskHierarchies.filter { it.current(now) }
                    .maxByOrNull { it.startExactTimeStampOffset }
                    ?.let { Type.Child(it) }
                    ?: Type.NoSchedule()

            return Interval.Current(type, startExactTimeStampOffset)
        }

        var typeBuilder = getNextTypeBuilder()
        if (typeBuilder == null) {
            val interval = if (taskEndExactTimeStampOffset == null) {
                getCurrentPlaceholder(taskStartExactTimeStampOffset)
            } else {
                Interval.Ended<T>(Type.NoSchedule(), taskStartExactTimeStampOffset, taskEndExactTimeStampOffset)
            }

            return listOf(interval)
        } else {
            check(typeBuilder.startExactTimeStampOffset >= taskStartExactTimeStampOffset)

            val endedIntervals = mutableListOf<Interval.Ended<T>>()

            var intervalBuilder: IntervalBuilder<T>

            if (typeBuilder.startExactTimeStampOffset > taskStartExactTimeStampOffset) {
                intervalBuilder = IntervalBuilder.NoSchedule(taskStartExactTimeStampOffset)
            } else {
                check(typeBuilder.startExactTimeStampOffset == taskStartExactTimeStampOffset)

                intervalBuilder = typeBuilder.toIntervalBuilder()

                typeBuilder = getNextTypeBuilder()
            }

            while (typeBuilder != null) {
                fun addIntervalBuilder(
                        endExactTimeStampOffset: ExactTimeStamp,
                        newIntervalBuilder: IntervalBuilder<T>
                ) {
                    if (intervalBuilder.badOverlap(endExactTimeStampOffset))
                        ErrorLogger.instance.logException(OverlapException(
                                "task: ${task.name}, taskKey: ${task.taskKey}, endExactTimeStampOffset: " +
                                        "$endExactTimeStampOffset, old interval builder: $intervalBuilder, new interval builder: $newIntervalBuilder"
                        ))

                    check(intervalBuilder.endExactTimeStampOffset?.let { it < endExactTimeStampOffset } != true)

                    endedIntervals += intervalBuilder.toEndedInterval(endExactTimeStampOffset)
                    intervalBuilder = newIntervalBuilder
                }

                fun addIntervalBuilder() = typeBuilder!!.run {
                    addIntervalBuilder(startExactTimeStampOffset, toIntervalBuilder())
                }

                if (
                        intervalBuilder.endExactTimeStampOffset?.let {
                            it < typeBuilder!!.startExactTimeStampOffset
                        } == true
                ) {
                    endedIntervals += intervalBuilder.toEndedInterval(intervalBuilder.endExactTimeStampOffset!!)
                    intervalBuilder = IntervalBuilder.NoSchedule(intervalBuilder.endExactTimeStampOffset!!)
                }

                when (val currentIntervalBuilder = intervalBuilder) {
                    is IntervalBuilder.Child -> addIntervalBuilder()
                    is IntervalBuilder.Schedule -> {
                        when (typeBuilder) {
                            is TypeBuilder.Parent -> addIntervalBuilder()
                            is TypeBuilder.Schedule -> currentIntervalBuilder.schedules += typeBuilder.schedule
                            is TypeBuilder.NoScheduleOrParent -> addIntervalBuilder()
                        }
                    }
                    is IntervalBuilder.NoSchedule -> addIntervalBuilder()
                }

                typeBuilder = getNextTypeBuilder()
            }

            val intervalEndExactTimeStampOffset = intervalBuilder.endExactTimeStampOffset ?: taskEndExactTimeStampOffset

            var currentInterval: Interval.Current<T>? = null

            if (intervalEndExactTimeStampOffset == null) {
                currentInterval = intervalBuilder.toCurrentInterval()
            } else {
                endedIntervals += intervalBuilder.toEndedInterval(intervalEndExactTimeStampOffset)

                when {
                    taskEndExactTimeStampOffset == null -> {
                        currentInterval = getCurrentPlaceholder(intervalEndExactTimeStampOffset)
                    }
                    taskEndExactTimeStampOffset > intervalEndExactTimeStampOffset -> {
                        endedIntervals += Interval.Ended(
                                Type.NoSchedule(),
                                intervalEndExactTimeStampOffset,
                                taskEndExactTimeStampOffset
                        )
                    }
                    else -> check(task.endExactTimeStampOffset == intervalEndExactTimeStampOffset)
                }
            }

            var oldTimeStamp = task.startExactTimeStampOffset
            endedIntervals.forEach {
                check(it.startExactTimeStampOffset == oldTimeStamp)

                oldTimeStamp = it.endExactTimeStampOffset
            }

            if (currentInterval != null) {
                check(taskEndExactTimeStampOffset == null)
                check(oldTimeStamp == currentInterval.startExactTimeStampOffset)
            } else {
                check(oldTimeStamp == task.endExactTimeStampOffset)
            }

            val intervals = (endedIntervals + currentInterval).filterNotNull()

            check(intervals.isNotEmpty())

            return intervals
        }
    }

    /*
     This isn't really an error.  But, to the best of my knowledge, no inconsistencies exist in the
     database as of now, so I'd like to know if any are found, to check if there's anything
     suspicious about them.
     */
    private class OverlapException(message: String) : Exception(message)

    private sealed class TypeBuilder<T : ProjectType> {

        abstract val startExactTimeStampOffset: ExactTimeStamp

        abstract fun toIntervalBuilder(): IntervalBuilder<T>

        class Parent<T : ProjectType>(val parentTaskHierarchy: TaskHierarchy<T>) : TypeBuilder<T>() {

            override val startExactTimeStampOffset = parentTaskHierarchy.startExactTimeStampOffset

            override fun toIntervalBuilder() = IntervalBuilder.Child(startExactTimeStampOffset, parentTaskHierarchy)
        }

        class Schedule<T : ProjectType>(
                val schedule: com.krystianwsul.common.firebase.models.Schedule<T>
        ) : TypeBuilder<T>() {

            override val startExactTimeStampOffset = schedule.startExactTimeStampOffset

            override fun toIntervalBuilder() = IntervalBuilder.Schedule(
                    startExactTimeStampOffset,
                    mutableListOf(schedule)
            )
        }

        class NoScheduleOrParent<T : ProjectType>(
                val noScheduleOrParent: com.krystianwsul.common.firebase.models.NoScheduleOrParent<T>
        ) : TypeBuilder<T>() {

            override val startExactTimeStampOffset = noScheduleOrParent.startExactTimeStampOffset

            override fun toIntervalBuilder() = IntervalBuilder.NoSchedule(startExactTimeStampOffset, noScheduleOrParent)
        }
    }

    private sealed class IntervalBuilder<T : ProjectType> {

        abstract val startExactTimeStampOffset: ExactTimeStamp
        abstract val endExactTimeStampOffset: ExactTimeStamp?

        fun toEndedInterval(endExactTimeStampOffset: ExactTimeStamp) =
                Interval.Ended(toType(), startExactTimeStampOffset, endExactTimeStampOffset)

        fun toCurrentInterval() = Interval.Current(toType(), startExactTimeStampOffset)

        fun toInterval(endExactTimeStampOffset: ExactTimeStamp?): Interval<T> {
            return if (endExactTimeStampOffset != null) {
                toEndedInterval(endExactTimeStampOffset)
            } else {
                toCurrentInterval()
            }
        }

        abstract fun toType(): Type<T>

        open fun badOverlap(nextEndExactTimeStampOffset: ExactTimeStamp) = endExactTimeStampOffset?.let { it <= nextEndExactTimeStampOffset } != true

        data class Child<T : ProjectType>(
                override val startExactTimeStampOffset: ExactTimeStamp,
                val parentTaskHierarchy: TaskHierarchy<T>
        ) : IntervalBuilder<T>() {

            override val endExactTimeStampOffset = parentTaskHierarchy.endExactTimeStampOffset

            override fun toType() = Type.Child(parentTaskHierarchy)
        }

        data class Schedule<T : ProjectType>(
                override val startExactTimeStampOffset: ExactTimeStamp,
                val schedules: MutableList<com.krystianwsul.common.firebase.models.Schedule<T>>
        ) : IntervalBuilder<T>() {

            override val endExactTimeStampOffset
                get() = schedules.map { it.endExactTimeStampOffset }.let {
                    if (it.any { it == null })
                        null
                    else
                        it.requireNoNulls().maxOrNull()
                }

            override fun toType() = Type.Schedule(schedules)
        }

        data class NoSchedule<T : ProjectType>(
                override val startExactTimeStampOffset: ExactTimeStamp,
                val noScheduleOrParent: NoScheduleOrParent<T>? = null
        ) : IntervalBuilder<T>() {

            override val endExactTimeStampOffset = noScheduleOrParent?.endExactTimeStampOffset

            override fun toType() = Type.NoSchedule(noScheduleOrParent)

            // endExactTimeStamp is meaningful only when the record is present
            override fun badOverlap(nextEndExactTimeStampOffset: ExactTimeStamp): Boolean {
                return if (noScheduleOrParent != null)
                    super.badOverlap(nextEndExactTimeStampOffset)
                else
                    false
            }
        }
    }
}