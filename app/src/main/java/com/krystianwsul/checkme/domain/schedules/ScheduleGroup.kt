package com.krystianwsul.checkme.domain.schedules

import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ScheduleData

sealed class ScheduleGroup {

    companion object {

        val allDaysOfWeek by lazy { DayOfWeek.values().toSet() }

        fun getGroups(schedules: List<Schedule>): List<ScheduleGroup> {
            fun Time.getTimeFloat(daysOfWeek: Collection<DayOfWeek>) = daysOfWeek.map { day ->
                getHourMinute(day).let { it.hour * 60 + it.minute }
            }
                    .sum()
                    .toFloat() / daysOfWeek.count()

            val singleSchedules = schedules.filterIsInstance<SingleSchedule>()
                    .sortedWith(compareBy(
                            { it.date },
                            { it.time.getHourMinute(it.date.dayOfWeek) }
                    ))
                    .map { Single(it) }

            val weeklySchedules = schedules.asSequence()
                    .filterIsInstance<WeeklySchedule>()
                    .groupBy { it.timePair }
                    .map { it.value.first().time to Weekly(it.key, it.value) }
                    .sortedBy { it.first.getTimeFloat(it.second.daysOfWeek) }
                    .map { it.second }
                    .toList()

            val monthlyDaySchedules = schedules.filterIsInstance<MonthlyDaySchedule>()
                    .sortedWith(compareBy(
                            { !it.beginningOfMonth },
                            { it.dayOfMonth },
                            { it.time.getTimeFloat(allDaysOfWeek) }))
                    .map(::MonthlyDay)

            val monthlyWeekSchedules = schedules.filterIsInstance<MonthlyWeekSchedule>()
                    .sortedWith(compareBy(
                            { !it.beginningOfMonth },
                            { it.dayOfMonth },
                            { it.dayOfWeek },
                            { it.time.getTimeFloat(allDaysOfWeek) }))
                    .map(::MonthlyWeek)

            return singleSchedules + weeklySchedules + monthlyDaySchedules + monthlyWeekSchedules
        }
    }

    abstract val customTimeKey: CustomTimeKey<*>?

    abstract val scheduleData: ScheduleData

    abstract val schedules: List<Schedule>

    class Single(val singleSchedule: SingleSchedule) : ScheduleGroup() {

        override val customTimeKey get() = singleSchedule.customTimeKey

        override val scheduleData get() = ScheduleData.Single(singleSchedule.date, singleSchedule.timePair)

        override val schedules get() = listOf(singleSchedule)
    }

    class Weekly(val timePair: TimePair, private val weeklySchedules: List<WeeklySchedule>) : ScheduleGroup() {

        override val customTimeKey get() = timePair.customTimeKey

        val daysOfWeek get() = weeklySchedules.flatMap { it.daysOfWeek }.toSet()

        override val scheduleData get() = ScheduleData.Weekly(daysOfWeek, timePair)

        override val schedules get() = weeklySchedules
    }

    class MonthlyDay(val monthlyDaySchedule: MonthlyDaySchedule) : ScheduleGroup() {

        override val customTimeKey get() = monthlyDaySchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.MonthlyDay(
                    monthlyDaySchedule.dayOfMonth,
                    monthlyDaySchedule.beginningOfMonth,
                    monthlyDaySchedule.timePair
            )

        override val schedules get() = listOf(monthlyDaySchedule)
    }

    class MonthlyWeek(val monthlyWeekSchedule: MonthlyWeekSchedule) : ScheduleGroup() {

        override val customTimeKey get() = monthlyWeekSchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.MonthlyWeek(
                    monthlyWeekSchedule.dayOfMonth,
                    monthlyWeekSchedule.dayOfWeek,
                    monthlyWeekSchedule.beginningOfMonth,
                    monthlyWeekSchedule.timePair
            )

        override val schedules get() = listOf(monthlyWeekSchedule)
    }
}