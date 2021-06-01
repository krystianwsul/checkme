package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp

object IntervalBuilder {

    /*
     Note: this will return NoSchedule for the time spans that were covered by irrelevant schedules
     and task hierarchies.  These periods, by definition, shouldn't be needed for anything.
     */
    fun build(task: Task, allowPlaceholderCurrentNoSchedule: Boolean): IntervalInfo {
        val allTypeBuilders = listOf(
            task.schedules.map { TypeBuilder.Schedule(it) },
            task.parentTaskHierarchies.map { TypeBuilder.Parent(it) },
            task.noScheduleOrParents.map { TypeBuilder.NoScheduleOrParent(it) },
        ).flatten()
            .sortedBy { it.startExactTimeStampOffset }
            .toMutableList()

        fun getNextTypeBuilder() = allTypeBuilders.takeIf { it.isNotEmpty() }?.removeAt(0)

        val taskStartExactTimeStampOffset = task.startExactTimeStampOffset
        val taskEndExactTimeStampOffset = task.endExactTimeStampOffset

        fun getCurrentPlaceholder(startExactTimeStampOffset: ExactTimeStamp.Offset) =
            Interval.Current(Type.NoSchedule(), startExactTimeStampOffset)

        var typeBuilder = getNextTypeBuilder()
        if (typeBuilder == null) {
            val interval = if (taskEndExactTimeStampOffset == null) {
                getCurrentPlaceholder(taskStartExactTimeStampOffset)
            } else {
                Interval.Ended(Type.NoSchedule(), taskStartExactTimeStampOffset, taskEndExactTimeStampOffset)
            }

            return IntervalInfo(task, listOf(interval))
        } else {
            check(typeBuilder.startExactTimeStampOffset >= taskStartExactTimeStampOffset) {
                "IntervalBuilder $task check 1: ${typeBuilder!!.startExactTimeStampOffset.details()} >= ${taskStartExactTimeStampOffset.details()}"
            }

            val endedIntervals = mutableListOf<Interval.Ended>()

            var intervalBuilder: IntervalBuilder

            if (typeBuilder.startExactTimeStampOffset > taskStartExactTimeStampOffset) {
                intervalBuilder = IntervalBuilder.NoSchedule(taskStartExactTimeStampOffset)
            } else {
                check(typeBuilder.startExactTimeStampOffset == taskStartExactTimeStampOffset) {
                    "IntervalBuilder $task check 2: ${typeBuilder!!.startExactTimeStampOffset} >= $taskStartExactTimeStampOffset"
                }

                intervalBuilder = typeBuilder.toIntervalBuilder()

                typeBuilder = getNextTypeBuilder()
            }

            while (typeBuilder != null) {
                fun addIntervalBuilder(
                    endExactTimeStampOffset: ExactTimeStamp.Offset,
                    newIntervalBuilder: IntervalBuilder,
                ) {
                    if (intervalBuilder.badOverlap(endExactTimeStampOffset))
                        ErrorLogger.instance.logException(
                            OverlapException(
                                "task: ${task.name}, taskKey: ${task.taskKey}, endExactTimeStampOffset: " +
                                        "$endExactTimeStampOffset, old interval builder: $intervalBuilder, new interval builder: $newIntervalBuilder"
                            )
                        )

                    check(intervalBuilder.endExactTimeStampOffset?.let { it < endExactTimeStampOffset } != true) {
                        "IntervalBuilder $task check 3: ${intervalBuilder.endExactTimeStampOffset}?.let { it < $endExactTimeStampOffset } != true"
                    }

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

            var currentInterval: Interval.Current? = null

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
                    else -> check(task.endExactTimeStampOffset == intervalEndExactTimeStampOffset) {
                        "IntervalBuilder $task check 4: ${task.endExactTimeStampOffset} == $intervalEndExactTimeStampOffset"
                    }
                }
            }

            var oldTimeStamp = task.startExactTimeStampOffset
            endedIntervals.forEach {
                check(it.startExactTimeStampOffset == oldTimeStamp) {
                    "IntervalBuilder $task check 5: ${it.startExactTimeStampOffset} == $oldTimeStamp"
                }

                oldTimeStamp = it.endExactTimeStampOffset
            }

            if (currentInterval != null) {
                check(taskEndExactTimeStampOffset == null) {
                    "IntervalBuilder $task check 6: $taskEndExactTimeStampOffset == null"
                }

                check(oldTimeStamp == currentInterval.startExactTimeStampOffset) {
                    "IntervalBuilder $task check 7: $oldTimeStamp == ${currentInterval.startExactTimeStampOffset}"
                }

                if (!allowPlaceholderCurrentNoSchedule) {
                    (currentInterval.type as? Type.NoSchedule)?.let { checkNotNull(it.noScheduleOrParent) }
                }
            } else {
                check(oldTimeStamp == task.endExactTimeStampOffset) {
                    "IntervalBuilder $task check 8: $oldTimeStamp == ${task.endExactTimeStampOffset}"
                }
            }

            val intervals = (endedIntervals + currentInterval).filterNotNull()

            check(intervals.isNotEmpty()) { "IntervalBuilder $task check 9: ${intervals.isNotEmpty()}" }

            return IntervalInfo(task, intervals)
        }
    }

    /*
     This isn't really an error.  But, to the best of my knowledge, no inconsistencies exist in the
     database as of now, so I'd like to know if any are found, to check if there's anything
     suspicious about them.
     */
    private class OverlapException(message: String) : Exception(message)

    private sealed class TypeBuilder {

        abstract val startExactTimeStampOffset: ExactTimeStamp.Offset

        abstract fun toIntervalBuilder(): IntervalBuilder

        class Parent(val parentTaskHierarchy: TaskHierarchy) : TypeBuilder() {

            override val startExactTimeStampOffset = parentTaskHierarchy.startExactTimeStampOffset

            override fun toIntervalBuilder() = IntervalBuilder.Child(startExactTimeStampOffset, parentTaskHierarchy)
        }

        class Schedule(
            val schedule: com.krystianwsul.common.firebase.models.schedule.Schedule,
        ) : TypeBuilder() {

            override val startExactTimeStampOffset = schedule.startExactTimeStampOffset

            override fun toIntervalBuilder() = IntervalBuilder.Schedule(
                startExactTimeStampOffset,
                mutableListOf(schedule),
            )
        }

        class NoScheduleOrParent(
            val noScheduleOrParent: com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent,
        ) : TypeBuilder() {

            override val startExactTimeStampOffset = noScheduleOrParent.startExactTimeStampOffset

            override fun toIntervalBuilder() =
                IntervalBuilder.NoSchedule(startExactTimeStampOffset, noScheduleOrParent)
        }
    }

    private sealed class IntervalBuilder {

        abstract val startExactTimeStampOffset: ExactTimeStamp.Offset
        abstract val endExactTimeStampOffset: ExactTimeStamp.Offset?

        fun toEndedInterval(endExactTimeStampOffset: ExactTimeStamp.Offset) =
            Interval.Ended(toType(), startExactTimeStampOffset, endExactTimeStampOffset)

        fun toCurrentInterval() = Interval.Current(toType(), startExactTimeStampOffset)

        abstract fun toType(): Type

        open fun badOverlap(nextEndExactTimeStampOffset: ExactTimeStamp.Offset) =
            endExactTimeStampOffset?.let { it <= nextEndExactTimeStampOffset } != true

        data class Child(
            override val startExactTimeStampOffset: ExactTimeStamp.Offset,
            val parentTaskHierarchy: TaskHierarchy,
        ) : IntervalBuilder() {

            override val endExactTimeStampOffset = parentTaskHierarchy.endExactTimeStampOffset

            override fun toType() = Type.Child(parentTaskHierarchy)
        }

        data class Schedule(
            override val startExactTimeStampOffset: ExactTimeStamp.Offset,
            val schedules: MutableList<com.krystianwsul.common.firebase.models.schedule.Schedule>,
        ) : IntervalBuilder() {

            override val endExactTimeStampOffset
                get() = schedules.map { it.endExactTimeStampOffset }.let {
                    if (it.any { it == null })
                        null
                    else
                        it.requireNoNulls().maxOrNull()
                }

            override fun toType() = Type.Schedule(schedules)
        }

        data class NoSchedule(
            override val startExactTimeStampOffset: ExactTimeStamp.Offset,
            val noScheduleOrParent: NoScheduleOrParent? = null,
        ) : IntervalBuilder() {

            override val endExactTimeStampOffset = noScheduleOrParent?.endExactTimeStampOffset

            override fun toType() = Type.NoSchedule(noScheduleOrParent)

            // endExactTimeStamp is meaningful only when the record is present
            override fun badOverlap(nextEndExactTimeStampOffset: ExactTimeStamp.Offset): Boolean {
                return if (noScheduleOrParent != null)
                    super.badOverlap(nextEndExactTimeStampOffset)
                else
                    false
            }
        }
    }
}