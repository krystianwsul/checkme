package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.models.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType
import firebase.models.interval.Interval
import firebase.models.interval.Type

object IntervalBuilder {

    /*
     Note: this will return NoSchedule for the time spans that were covered by irrelevant schedules
     and task hierarchies.  These periods, by definition, shouldn't be needed for anything.
     */
    fun <T : ProjectType> build(task: Task<T>): List<Interval<T>> {
        val schedules = task.schedules.toMutableList()

        val groupParentTaskHierarchies = task.parentTaskHierarchies
                .filter { it.parentIsGroupTask }
                .toMutableList()

        val normalParentTaskHierarchies = (task.parentTaskHierarchies - groupParentTaskHierarchies).toMutableList()

        val noScheduleOrParents = task.noScheduleOrParents.toMutableList()

        fun getNextTypeBuilder(): TypeBuilder<T>? {
            val nextSchedule = schedules.minBy { it.startExactTimeStamp }?.let { TypeBuilder.Schedule(it) }
            val nextParentTaskHierarchy = normalParentTaskHierarchies.minBy { it.startExactTimeStamp }?.let { TypeBuilder.Parent(it) }
            val nextNoScheduleOrParent = noScheduleOrParents.minBy { it.startExactTimeStamp }?.let { TypeBuilder.NoScheduleOrParent(it) }

            return listOfNotNull(
                    nextNoScheduleOrParent,
                    nextParentTaskHierarchy,
                    nextSchedule
            ).minBy { it.startExactTimeStamp }?.also {
                when (it) {
                    is TypeBuilder.NoScheduleOrParent -> noScheduleOrParents.remove(it.noScheduleOrParent)
                    is TypeBuilder.Parent -> normalParentTaskHierarchies.remove(it.parentTaskHierarchy)
                    is TypeBuilder.Schedule -> schedules.remove(it.schedule)
                }
            }
        }

        val taskStartExactTimeStamp = task.startExactTimeStamp
        val taskEndExactTimeStamp = task.endExactTimeStamp

        fun getCurrentPlaceholder(startExactTimeStamp: ExactTimeStamp): Interval.Current<T> {
            /*
                since the only way for a task to not have a NoScheduleOrParentRecord is
                    1. it's been garbage collected, so it's really a moot point, and
                    2. the task was created directly as a child of a group task,
                I think group task hierarchies are relevant only to avoid displaying #2
                children as unscheduled root tasks, which is what I fix here.
             */

            val type = groupParentTaskHierarchies.filter { it.current(ExactTimeStamp.now) }
                    .maxBy { it.startExactTimeStamp }
                    ?.let { Type.Child(it) }
                    ?: Type.NoSchedule<T>()

            return Interval.Current(type, startExactTimeStamp)
        }

        var typeBuilder = getNextTypeBuilder()
        if (typeBuilder == null) {
            val interval = if (taskEndExactTimeStamp == null) {
                getCurrentPlaceholder(taskStartExactTimeStamp)
            } else {
                Interval.Ended<T>(Type.NoSchedule(), taskStartExactTimeStamp, taskEndExactTimeStamp)
            }

            return listOf(interval)
        } else {
            check(typeBuilder.startExactTimeStamp >= taskStartExactTimeStamp)

            val endedIntervals = mutableListOf<Interval.Ended<T>>()

            var intervalBuilder: IntervalBuilder<T>

            if (typeBuilder.startExactTimeStamp > taskStartExactTimeStamp) {
                intervalBuilder = IntervalBuilder.NoSchedule(taskStartExactTimeStamp)
            } else {
                check(typeBuilder.startExactTimeStamp == taskStartExactTimeStamp)

                intervalBuilder = typeBuilder.toIntervalBuilder()

                typeBuilder = getNextTypeBuilder()
            }

            while (typeBuilder != null) {
                fun addIntervalBuilder(endExactTimeStamp: ExactTimeStamp, newIntervalBuilder: IntervalBuilder<T>) {
                    if (intervalBuilder.badOverlap(endExactTimeStamp))
                        ErrorLogger.instance.logException(OverlapException("task: ${task.name}, taskKey: ${task.taskKey}, endExactTimeStamp: $endExactTimeStamp, old interval builder: $intervalBuilder, new interval builder: $newIntervalBuilder"))

                    check(intervalBuilder.endExactTimeStamp?.let { it < endExactTimeStamp } != true)

                    endedIntervals += intervalBuilder.toEndedInterval(endExactTimeStamp)
                    intervalBuilder = newIntervalBuilder
                }

                fun addIntervalBuilder() = typeBuilder!!.run { addIntervalBuilder(startExactTimeStamp, toIntervalBuilder()) }

                if (intervalBuilder.endExactTimeStamp?.let { it < typeBuilder!!.startExactTimeStamp } == true) {
                    endedIntervals += intervalBuilder.toEndedInterval(intervalBuilder.endExactTimeStamp!!)
                    intervalBuilder = IntervalBuilder.NoSchedule(intervalBuilder.endExactTimeStamp!!)
                }

                when (val currentIntervalBuilder = intervalBuilder) {
                    is IntervalBuilder.Child -> addIntervalBuilder()
                    is IntervalBuilder.Schedule -> {
                        when (typeBuilder) {
                            is TypeBuilder.Parent -> addIntervalBuilder()
                            is TypeBuilder.Schedule -> currentIntervalBuilder.schedules += typeBuilder.schedule
                        }
                    }
                    is IntervalBuilder.NoSchedule -> addIntervalBuilder()
                }

                typeBuilder = getNextTypeBuilder()
            }

            val intervalEndExactTimeStamp = intervalBuilder.endExactTimeStamp
                    ?: taskEndExactTimeStamp

            var currentInterval: Interval.Current<T>? = null

            if (intervalEndExactTimeStamp == null) {
                currentInterval = intervalBuilder.toCurrentInterval()
            } else {
                endedIntervals += intervalBuilder.toEndedInterval(intervalEndExactTimeStamp)

                when {
                    taskEndExactTimeStamp == null -> {
                        currentInterval = getCurrentPlaceholder(intervalEndExactTimeStamp)
                    }
                    taskEndExactTimeStamp > intervalEndExactTimeStamp -> {
                        endedIntervals += Interval.Ended(Type.NoSchedule(), intervalEndExactTimeStamp, taskEndExactTimeStamp)
                    }
                    else -> check(task.endExactTimeStamp == intervalEndExactTimeStamp)
                }
            }

            var oldTimeStamp = task.startExactTimeStamp
            endedIntervals.forEach {
                check(it.startExactTimeStamp == oldTimeStamp)

                oldTimeStamp = it.endExactTimeStamp
            }

            if (currentInterval != null) {
                check(taskEndExactTimeStamp == null)
                check(oldTimeStamp == currentInterval.startExactTimeStamp)
            } else {
                check(oldTimeStamp == task.endExactTimeStamp)
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

        abstract val startExactTimeStamp: ExactTimeStamp

        abstract fun toIntervalBuilder(): IntervalBuilder<T>

        class Parent<T : ProjectType>(val parentTaskHierarchy: TaskHierarchy<T>) : TypeBuilder<T>() {

            override val startExactTimeStamp = parentTaskHierarchy.startExactTimeStamp

            override fun toIntervalBuilder() = IntervalBuilder.Child(startExactTimeStamp, parentTaskHierarchy)
        }

        class Schedule<T : ProjectType>(val schedule: com.krystianwsul.common.firebase.models.Schedule<T>) : TypeBuilder<T>() {

            override val startExactTimeStamp = schedule.startExactTimeStamp

            override fun toIntervalBuilder() = IntervalBuilder.Schedule(startExactTimeStamp, mutableListOf(schedule))
        }

        class NoScheduleOrParent<T : ProjectType>(val noScheduleOrParent: com.krystianwsul.common.firebase.models.NoScheduleOrParent<T>) : TypeBuilder<T>() {

            override val startExactTimeStamp = noScheduleOrParent.startExactTimeStamp

            override fun toIntervalBuilder() = IntervalBuilder.NoSchedule<T>(startExactTimeStamp)
        }
    }

    private sealed class IntervalBuilder<T : ProjectType> {

        abstract val startExactTimeStamp: ExactTimeStamp
        abstract val endExactTimeStamp: ExactTimeStamp?

        fun toEndedInterval(endExactTimeStamp: ExactTimeStamp) =
                Interval.Ended(toType(), startExactTimeStamp, endExactTimeStamp)

        fun toCurrentInterval() = Interval.Current(toType(), startExactTimeStamp)

        fun toInterval(endExactTimeStamp: ExactTimeStamp?): Interval<T> {
            return if (endExactTimeStamp != null) {
                toEndedInterval(endExactTimeStamp)
            } else {
                toCurrentInterval()
            }
        }

        abstract fun toType(): Type<T>

        open fun badOverlap(nextEndExactTimeStamp: ExactTimeStamp) = endExactTimeStamp?.let { it <= nextEndExactTimeStamp } != true

        data class Child<T : ProjectType>(
                override val startExactTimeStamp: ExactTimeStamp,
                val parentTaskHierarchy: TaskHierarchy<T>
        ) : IntervalBuilder<T>() {

            override val endExactTimeStamp = parentTaskHierarchy.endExactTimeStamp

            override fun toType() = Type.Child(parentTaskHierarchy)
        }

        data class Schedule<T : ProjectType>(
                override val startExactTimeStamp: ExactTimeStamp,
                val schedules: MutableList<com.krystianwsul.common.firebase.models.Schedule<T>>
        ) : IntervalBuilder<T>() {

            override val endExactTimeStamp
                get() = schedules.map { it.endExactTimeStamp }.let {
                    if (it.any { it == null })
                        null
                    else
                        it.requireNoNulls().max()
                }

            override fun toType() = Type.Schedule(schedules)
        }

        data class NoSchedule<T : ProjectType>(
                override val startExactTimeStamp: ExactTimeStamp,
                val noScheduleOrParent: NoScheduleOrParent<T>? = null
        ) : IntervalBuilder<T>() {

            override val endExactTimeStamp = noScheduleOrParent?.endExactTimeStamp

            override fun toType() = Type.NoSchedule(noScheduleOrParent)

            // endExactTimeStamp is meaningful only when the record is present
            override fun badOverlap(nextEndExactTimeStamp: ExactTimeStamp): Boolean {
                return if (noScheduleOrParent != null)
                    super.badOverlap(nextEndExactTimeStamp)
                else
                    false
            }
        }
    }
}