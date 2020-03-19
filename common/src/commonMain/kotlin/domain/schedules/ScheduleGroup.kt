package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData

sealed class ScheduleGroup<T : CustomTimeId, U : ProjectKey> {

    companion object {

        private val allDaysOfWeek by lazy { DayOfWeek.values().toSet() }

        fun <T : CustomTimeId, U : ProjectKey> getGroups(schedules: List<Schedule<out T, out U>>): List<ScheduleGroup<T, U>> {
            fun Time.getTimeFloat(daysOfWeek: Collection<DayOfWeek>) = daysOfWeek.map { day ->
                getHourMinute(day).let { it.hour * 60 + it.minute }
            }
                    .sum()
                    .toFloat() / daysOfWeek.count()

            val singleSchedules = schedules.filterIsInstance<SingleSchedule<T, U>>()
                    .sortedWith(compareBy(
                            { it.date },
                            { it.time.getHourMinute(it.date.dayOfWeek) }
                    ))
                    .map { Single(it) }

            val weeklySchedules = schedules.asSequence()
                    .filterIsInstance<WeeklySchedule<T, U>>()
                    .groupBy { Triple(it.timePair, it.from, it.until) }
                    .map { it.value.first().time to Weekly(it.value.first().customTimeKey, it.key.first, it.value, it.key.second, it.key.third) }
                    .sortedBy { it.first.getTimeFloat(it.second.daysOfWeek) }
                    .map { it.second }
                    .toList()

            val monthlyDaySchedules = schedules.filterIsInstance<MonthlyDaySchedule<T, U>>()
                    .sortedWith(compareBy(
                            { !it.beginningOfMonth },
                            { it.dayOfMonth },
                            { it.time.getTimeFloat(allDaysOfWeek) }))
                    .map { MonthlyDay(it) }

            val monthlyWeekSchedules = schedules.filterIsInstance<MonthlyWeekSchedule<T, U>>()
                    .sortedWith(compareBy(
                            { !it.beginningOfMonth },
                            { it.dayOfMonth },
                            { it.dayOfWeek },
                            { it.time.getTimeFloat(allDaysOfWeek) }))
                    .map { MonthlyWeek(it) }

            return singleSchedules + weeklySchedules + monthlyDaySchedules + monthlyWeekSchedules
        }
    }

    abstract val customTimeKey: CustomTimeKey<T, U>?

    abstract val scheduleData: ScheduleData

    abstract val schedules: List<Schedule<T, U>>

    class Single<T : CustomTimeId, U : ProjectKey>(
            private val singleSchedule: SingleSchedule<T, U>
    ) : ScheduleGroup<T, U>() {

        override val customTimeKey get() = singleSchedule.customTimeKey

        override val scheduleData get() = ScheduleData.Single(singleSchedule.date, singleSchedule.timePair)

        override val schedules get() = listOf(singleSchedule)
    }

    class Weekly<T : CustomTimeId, U : ProjectKey>(
            override val customTimeKey: CustomTimeKey<T, U>?,
            private val timePair: TimePair,
            private val weeklySchedules: List<WeeklySchedule<T, U>>,
            val from: Date?,
            val until: Date?
    ) : ScheduleGroup<T, U>() {

        val daysOfWeek get() = weeklySchedules.flatMap { it.daysOfWeek }.toSet()

        override val scheduleData get() = ScheduleData.Weekly(daysOfWeek, timePair, from, until)

        override val schedules get() = weeklySchedules
    }

    class MonthlyDay<T : CustomTimeId, U : ProjectKey>(
            private val monthlyDaySchedule: MonthlyDaySchedule<T, U>
    ) : ScheduleGroup<T, U>() {

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

    class MonthlyWeek<T : CustomTimeId, U : ProjectKey>(
            private val monthlyWeekSchedule: MonthlyWeekSchedule<T, U>
    ) : ScheduleGroup<T, U>() {

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
}