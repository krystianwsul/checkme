package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.schedule.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.UserKey

sealed interface ScheduleGroup {

    companion object {

        fun getGroups(schedules: List<Schedule>): List<ScheduleGroup> {
            fun Time.getTimeFloat(daysOfWeek: Collection<DayOfWeek> = DayOfWeek.set) = daysOfWeek.sumOf { day ->
                getHourMinute(day).let { it.hour * 60 + it.minute }
            }
                .toFloat()
                .let { it / daysOfWeek.count() }

            val singleSchedules = schedules.filterIsInstance<SingleSchedule>()
                .sortedWith(
                    compareBy(
                        { it.date },
                        { it.time.getHourMinute(it.date.dayOfWeek) },
                    )
                )
                .map {
                    it.getInstance(it.topLevelTask)
                        .parentInstance
                        ?.let { instance -> Child(it, instance) }
                        ?: Single(it)
                }

            data class WeeklyKey(
                val timePair: TimePair,
                val from: Date?,
                val until: Date?,
                val interval: Weekly.Interval,
                val assignedTo: Set<UserKey>,
            )

            val weeklySchedules = schedules.asSequence()
                .filterIsInstance<WeeklySchedule>()
                .groupBy {
                    val interval = if (it.interval == 1) {
                        Weekly.Interval.One
                    } else {
                        Weekly.Interval.More(it.from!!, it.interval)
                    }

                    WeeklyKey(it.timePair, it.from, it.until, interval, it.assignedTo)
                }
                .map {
                    val first = it.value.first()

                    Pair(
                        first.time,
                        Weekly(
                            it.key.timePair,
                            it.value,
                            it.key.from,
                            it.key.until,
                            it.key.interval,
                            it.key.assignedTo,
                        )
                    )
                }
                .sortedBy { it.first.getTimeFloat(it.second.daysOfWeek) }
                .map { it.second }
                .toList()

            val monthlyDaySchedules = schedules.filterIsInstance<MonthlyDaySchedule>()
                .sortedWith(
                    compareBy(
                        { !it.beginningOfMonth },
                        { it.dayOfMonth },
                        { it.time.getTimeFloat() }),
                )
                .map { MonthlyDay(it) }

            val monthlyWeekSchedules = schedules.filterIsInstance<MonthlyWeekSchedule>()
                .sortedWith(
                    compareBy(
                        { !it.beginningOfMonth },
                        { it.weekOfMonth },
                        { it.dayOfWeek },
                        { it.time.getTimeFloat() }),
                )
                .map { MonthlyWeek(it) }

            val yearlySchedules = schedules.filterIsInstance<YearlySchedule>()
                .sortedWith(
                    compareBy(
                        { it.month },
                        { it.day },
                        { it.time.getTimeFloat() },
                    )
                )
                .map { Yearly(it) }

            return singleSchedules + weeklySchedules + monthlyDaySchedules + monthlyWeekSchedules + yearlySchedules
        }
    }

    val scheduleData: ScheduleData

    val schedules: List<Schedule>

    // used to diff new/old schedules
    val assignedTo: Set<UserKey>

    sealed interface Reusable : ScheduleGroup {

        val singleSchedule: SingleSchedule

        override val scheduleData: ScheduleData.Reusable

        override val schedules get() = listOf(singleSchedule)

        override val assignedTo get() = singleSchedule.assignedTo
    }

    class Single(override val singleSchedule: SingleSchedule) : Reusable {

        override val scheduleData get() = ScheduleData.Single(singleSchedule.date, singleSchedule.timePair)
    }

    class Weekly(
        private val timePair: TimePair,
        private val weeklySchedules: List<WeeklySchedule>,
        val from: Date?,
        val until: Date?,
        private val interval: Interval,
        override val assignedTo: Set<UserKey>,
    ) : ScheduleGroup {

        val daysOfWeek get() = weeklySchedules.map { it.dayOfWeek }.toSet()

        override val scheduleData
            get() = ScheduleData.Weekly(
                daysOfWeek,
                timePair,
                from,
                until,
                interval.interval,
            )

        override val schedules get() = weeklySchedules

        sealed class Interval {

            abstract val interval: Int

            object One : Interval() {

                override val interval = 1
            }

            data class More(val from: Date, override val interval: Int) : Interval()
        }
    }

    class MonthlyDay(private val monthlyDaySchedule: MonthlyDaySchedule) : ScheduleGroup {

        override val scheduleData
            get() = ScheduleData.MonthlyDay(
                monthlyDaySchedule.dayOfMonth,
                monthlyDaySchedule.beginningOfMonth,
                monthlyDaySchedule.timePair,
                monthlyDaySchedule.from,
                monthlyDaySchedule.until,
            )

        override val schedules get() = listOf(monthlyDaySchedule)

        override val assignedTo get() = monthlyDaySchedule.assignedTo
    }

    class MonthlyWeek(private val monthlyWeekSchedule: MonthlyWeekSchedule) : ScheduleGroup {

        override val scheduleData
            get() = ScheduleData.MonthlyWeek(
                monthlyWeekSchedule.weekOfMonth,
                monthlyWeekSchedule.dayOfWeek,
                monthlyWeekSchedule.beginningOfMonth,
                monthlyWeekSchedule.timePair,
                monthlyWeekSchedule.from,
                monthlyWeekSchedule.until,
            )

        override val schedules get() = listOf(monthlyWeekSchedule)

        override val assignedTo get() = monthlyWeekSchedule.assignedTo
    }

    class Yearly(private val yearlySchedule: YearlySchedule) : ScheduleGroup {

        override val scheduleData
            get() = ScheduleData.Yearly(
                yearlySchedule.month,
                yearlySchedule.day,
                yearlySchedule.timePair,
                yearlySchedule.from,
                yearlySchedule.until,
            )

        override val schedules get() = listOf(yearlySchedule)

        override val assignedTo get() = yearlySchedule.assignedTo
    }

    class Child(override val singleSchedule: SingleSchedule, val parentInstance: Instance) : Reusable {

        override val scheduleData get() = ScheduleData.Child(parentInstance.instanceKey)
    }
}