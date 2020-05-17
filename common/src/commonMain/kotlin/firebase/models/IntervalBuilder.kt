package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

object IntervalBuilder {

    /*
     todo group task once this is being used, also use the intervals for checking schedules in the
     same way that oldestVisible, from/until, and start/end/ExactTimeStamp is used.
     */

    /*
     Note: this will return NoSchedule for the time spans that were covered by irrelevant schedules
     and task hierarchies.  These periods, by definition, shouldn't be needed for anything.
     */
    fun <T : ProjectType> build(task: Task<T>): List<Interval<T>> {
        val schedules = task.schedules.toMutableList()
        val parentTaskHierarchies = task.getParentTaskHierarchies().toMutableList()

        fun getNextTypeBuilder(): TypeBuilder<T>? {
            val nextSchedule = schedules.minBy { it.startExactTimeStamp }
            val nextParentTaskHierarchy = parentTaskHierarchies.minBy { it.startExactTimeStamp }

            fun returnSchedule(nextSchedule: Schedule<T>): TypeBuilder.Schedule<T> {
                schedules.remove(nextSchedule)

                return TypeBuilder.Schedule(nextSchedule)
            }

            fun returnParent(nextParentTaskHierarchy: TaskHierarchy<T>): TypeBuilder.Parent<T> {
                parentTaskHierarchies.remove(nextParentTaskHierarchy)

                return TypeBuilder.Parent(nextParentTaskHierarchy)
            }

            return if (nextSchedule != null && nextParentTaskHierarchy != null) {
                if (nextSchedule.startExactTimeStamp < nextParentTaskHierarchy.startExactTimeStamp) // parents take precedence (arbitrarily)
                    returnSchedule(nextSchedule)
                else
                    returnParent(nextParentTaskHierarchy)
            } else if (nextParentTaskHierarchy != null) {
                returnParent(nextParentTaskHierarchy)
            } else if (nextSchedule != null) {
                returnSchedule(nextSchedule)
            } else {
                null
            }
        }

        val taskStartExactTimeStamp = task.startExactTimeStamp
        val taskEndExactTimeStamp = task.endExactTimeStamp

        var typeBuilder = getNextTypeBuilder()
        if (typeBuilder == null) {
            val interval = if (taskEndExactTimeStamp == null) {
                Interval.Current<T>(Type.NoSchedule(), taskStartExactTimeStamp)
            } else {
                Interval.Ended<T>(Type.NoSchedule(), taskStartExactTimeStamp, taskEndExactTimeStamp)
            }

            return listOf(interval)
        } else {
            check(typeBuilder.startExactTimeStamp >= taskStartExactTimeStamp)

            val intervals = mutableListOf<Interval<T>>()

            var intervalBuilder: IntervalBuilder<T>

            if (typeBuilder.startExactTimeStamp > taskStartExactTimeStamp) {
                intervalBuilder = IntervalBuilder.NoSchedule(typeBuilder.startExactTimeStamp)
            } else {
                check(typeBuilder.startExactTimeStamp == taskStartExactTimeStamp)

                intervalBuilder = typeBuilder.toIntervalBuilder()

                typeBuilder = getNextTypeBuilder()
            }

            while (typeBuilder != null) {
                fun addIntervalBuilder(endExactTimeStamp: ExactTimeStamp, newIntervalBuilder: IntervalBuilder<T>) {
                    /*
                     todo group tasks: endExactTimeStamp isn't meaningful for NoSchedule constructed
                      from an absence of records.  It will be, however, for NoSchedule constructed
                      from the records I'll be adding to represent a root task without a schedule.
                     */
                    if (intervalBuilder !is IntervalBuilder.NoSchedule &&
                            intervalBuilder.endExactTimeStamp?.let { it <= endExactTimeStamp } != true
                    ) {
                        ErrorLogger.instance.logException(OverlapException("task: ${task.name}, taskKey: ${task.taskKey}, endExactTimeStamp: $endExactTimeStamp, old interval builder: $intervalBuilder, new interval builder: $newIntervalBuilder"))
                    }

                    intervals.add(intervalBuilder.toInterval(endExactTimeStamp))
                    intervalBuilder = newIntervalBuilder
                }

                fun addIntervalBuilder() = typeBuilder!!.run { addIntervalBuilder(startExactTimeStamp, toIntervalBuilder()) }

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

            return intervals + intervalBuilder.toInterval(null)
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
    }

    private sealed class IntervalBuilder<T : ProjectType> {

        abstract val startExactTimeStamp: ExactTimeStamp
        abstract val endExactTimeStamp: ExactTimeStamp?

        fun toInterval(endExactTimeStamp: ExactTimeStamp?): Interval<T> {
            return if (endExactTimeStamp != null) {
                Interval.Ended(toType(), startExactTimeStamp, endExactTimeStamp)
            } else {
                Interval.Current(toType(), startExactTimeStamp)
            }
        }

        abstract fun toType(): Type<T>

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
                override val startExactTimeStamp: ExactTimeStamp
        ) : IntervalBuilder<T>() {

            override val endExactTimeStamp: ExactTimeStamp? = null

            override fun toType() = Type.NoSchedule<T>()
        }
    }

    sealed class Interval<T : ProjectType> {

        abstract val type: Type<T>

        abstract val startExactTimeStamp: ExactTimeStamp

        open fun containsExactTimeStamp(exactTimeStamp: ExactTimeStamp) = startExactTimeStamp <= exactTimeStamp

        data class Current<T : ProjectType>(
                override val type: Type<T>,
                override val startExactTimeStamp: ExactTimeStamp
        ) : Interval<T>()

        data class Ended<T : ProjectType>(
                override val type: Type<T>,
                override val startExactTimeStamp: ExactTimeStamp,
                val endExactTimeStamp: ExactTimeStamp
        ) : Interval<T>() {

            override fun containsExactTimeStamp(exactTimeStamp: ExactTimeStamp): Boolean {
                if (!super.containsExactTimeStamp(exactTimeStamp))
                    return false

                return endExactTimeStamp > exactTimeStamp
            }
        }
    }

    sealed class Type<T : ProjectType> {

        class Child<T : ProjectType>(val parentTaskHierarchy: TaskHierarchy<T>) : Type<T>()

        class Schedule<T : ProjectType>(val schedules: List<com.krystianwsul.common.firebase.models.Schedule<T>>) : Type<T>()

        class NoSchedule<T : ProjectType> : Type<T>()
    }
}