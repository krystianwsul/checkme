package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleData

sealed class ScheduleGroup<T : ProjectType> {

    companion object {

        fun <T : ProjectType> getGroups(schedules: List<Schedule<out T>>): List<ScheduleGroup<T>> {
            fun Time.getTimeFloat(daysOfWeek: Collection<DayOfWeek> = DayOfWeek.set) = daysOfWeek.map { day ->
                getHourMinute(day).let { it.hour * 60 + it.minute }
            }
                    .sum()
                    .toFloat() / daysOfWeek.count()

            val singleSchedules = schedules.filterIsInstance<SingleSchedule<T>>()
                    .sortedWith(compareBy(
                            { it.date },
                            { it.time.getHourMinute(it.date.dayOfWeek) }
                    ))
                    .map { Single(it) }

            val weeklySchedules = schedules.asSequence()
                    .filterIsInstance<WeeklySchedule<T>>()
                    .groupBy { Triple(it.timePair, it.from, it.until) }
                    .map { it.value.first().time to Weekly(it.value.first().customTimeKey, it.key.first, it.value, it.key.second, it.key.third) }
                    .sortedBy { it.first.getTimeFloat(it.second.daysOfWeek) }
                    .map { it.second }
                    .toList()

            val monthlyDaySchedules = schedules.filterIsInstance<MonthlyDaySchedule<T>>()
                    .sortedWith(compareBy(
                            { !it.beginningOfMonth },
                            { it.dayOfMonth },
                            { it.time.getTimeFloat() }))
                    .map { MonthlyDay(it) }

            val monthlyWeekSchedules = schedules.filterIsInstance<MonthlyWeekSchedule<T>>()
                    .sortedWith(compareBy(
                            { !it.beginningOfMonth },
                            { it.dayOfMonth },
                            { it.dayOfWeek },
                            { it.time.getTimeFloat() }))
                    .map { MonthlyWeek(it) }

            val yearlySchedules = schedules.filterIsInstance<YearlySchedule<T>>()
                    .sortedWith(compareBy(
                            { it.month },
                            { it.day },
                            { it.time.getTimeFloat() }
                    ))
                    .map { Yearly(it) }

            return singleSchedules + weeklySchedules + monthlyDaySchedules + monthlyWeekSchedules + yearlySchedules
        }
    }

    abstract val customTimeKey: CustomTimeKey<T>?

    abstract val scheduleData: ScheduleData

    abstract val schedules: List<Schedule<T>>

    class Single<T : ProjectType>(
            private val singleSchedule: SingleSchedule<T>
    ) : ScheduleGroup<T>() {

        override val customTimeKey get() = singleSchedule.customTimeKey

        override val scheduleData get() = ScheduleData.Single(singleSchedule.date, singleSchedule.timePair)

        override val schedules get() = listOf(singleSchedule)
    }

    class Weekly<T : ProjectType>(
            override val customTimeKey: CustomTimeKey<T>?,
            private val timePair: TimePair,
            private val weeklySchedules: List<WeeklySchedule<T>>,
            val from: Date?,
            val until: Date?
    ) : ScheduleGroup<T>() {

        val daysOfWeek get() = weeklySchedules.map { it.dayOfWeek }.toSet()

        override val scheduleData get() = ScheduleData.Weekly(daysOfWeek, timePair, from, until)

        override val schedules get() = weeklySchedules
    }

    class MonthlyDay<T : ProjectType>(
            private val monthlyDaySchedule: MonthlyDaySchedule<T>
    ) : ScheduleGroup<T>() {

        override val customTimeKey get() = monthlyDaySchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.MonthlyDay(
                    monthlyDaySchedule.dayOfMonth,
                    monthlyDaySchedule.beginningOfMonth,
                    monthlyDaySchedule.timePair,
                    monthlyDaySchedule.from,
                    monthlyDaySchedule.until
            )

        override val schedules get() = listOf(monthlyDaySchedule)
    }

    class MonthlyWeek<T : ProjectType>(
            private val monthlyWeekSchedule: MonthlyWeekSchedule<T>
    ) : ScheduleGroup<T>() {

        override val customTimeKey get() = monthlyWeekSchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.MonthlyWeek(
                    monthlyWeekSchedule.dayOfMonth,
                    monthlyWeekSchedule.dayOfWeek,
                    monthlyWeekSchedule.beginningOfMonth,
                    monthlyWeekSchedule.timePair,
                    monthlyWeekSchedule.from,
                    monthlyWeekSchedule.until
            )

        override val schedules get() = listOf(monthlyWeekSchedule)
    }

    class Yearly<T : ProjectType>(
            private val yearlySchedule: YearlySchedule<T>
    ) : ScheduleGroup<T>() {

        override val customTimeKey get() = yearlySchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.Yearly(
                    yearlySchedule.month,
                    yearlySchedule.day,
                    yearlySchedule.timePair,
                    yearlySchedule.from,
                    yearlySchedule.until
            )

        override val schedules get() = listOf(yearlySchedule)
    }
}