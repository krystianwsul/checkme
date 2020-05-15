package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

object TreeDataBuilder {

    /*
     todo group task if there is a conflict, choose the type that has the newest (un-ended) record,
      and log a warning.  (If there's more than one parent, do the same amongst those records).  On
      the server, use the exact same algorithm to establish which records are "correct", remove the
      remaining CURRENT records, and log a warning.
     */

    fun <T : ProjectType> build(task: Task<T>): List<Interval<T>> {
        val schedules = task.schedules.toMutableList()
        val parentTaskHierarchies = task.getParentTaskHierarchies().toMutableList()

        fun getNextTypeBuilder(): TypeBuilder<T>? {
            val nextSchedule = schedules.minBy { it.startExactTimeStamp }
            val nextParentTaskHierarchy = parentTaskHierarchies.minBy { it.startExactTimeStamp }

            return if (nextSchedule == null || nextParentTaskHierarchy == null) {
                null
            } else { // parents take precedence (arbitrarily)
                if (nextSchedule.startExactTimeStamp < nextParentTaskHierarchy.startExactTimeStamp) {
                    schedules.remove(nextSchedule)

                    TypeBuilder.Schedule(nextSchedule)
                } else {
                    parentTaskHierarchies.remove(nextParentTaskHierarchy)

                    TypeBuilder.Parent(nextParentTaskHierarchy)
                }
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
                    if (intervalBuilder.endExactTimeStamp?.let { it <= endExactTimeStamp } != true) // todo group task test this
                        ErrorLogger.instance.logException(OverlapException("task: ${task.name}, taskKey: ${task.taskKey}, endExactTimeStamp: $endExactTimeStamp, old interval builder: $intervalBuilder, new interval builder: $newIntervalBuilder"))

                    intervals.add(intervalBuilder.toInterval(endExactTimeStamp))
                    intervalBuilder = newIntervalBuilder
                }

                fun addIntervalBuilder() = typeBuilder!!.run { addIntervalBuilder(startExactTimeStamp, toIntervalBuilder()) }

                val currentIntervalBuilder = intervalBuilder
                val intervalEndExactTimeStamp = currentIntervalBuilder.endExactTimeStamp

                if (intervalEndExactTimeStamp?.let { it < typeBuilder!!.startExactTimeStamp } == true) { // guaranteed not NoSchedule
                    addIntervalBuilder(
                            intervalEndExactTimeStamp,
                            IntervalBuilder.NoSchedule(intervalEndExactTimeStamp)
                    )
                } else {
                    when (currentIntervalBuilder) {
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
            }

            return intervals + intervalBuilder.toInterval(null)
        }
    }

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

        class Current<T : ProjectType>(
                override val type: Type<T>,
                override val startExactTimeStamp: ExactTimeStamp
        ) : Interval<T>()

        class Ended<T : ProjectType>(
                override val type: Type<T>,
                override val startExactTimeStamp: ExactTimeStamp,
                val endExactTimeStamp: ExactTimeStamp
        ) : Interval<T>()
    }

    sealed class Type<T : ProjectType> {

        class Child<T : ProjectType>(val parentTaskHierarchy: TaskHierarchy<T>) : Type<T>()

        class Schedule<T : ProjectType>(val schedules: List<com.krystianwsul.common.firebase.models.Schedule<T>>) : Type<T>()

        class NoSchedule<T : ProjectType> : Type<T>()
    }
}