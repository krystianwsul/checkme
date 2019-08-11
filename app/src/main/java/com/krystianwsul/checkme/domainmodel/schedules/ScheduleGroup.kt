package com.krystianwsul.checkme.domainmodel.schedules

import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel.ScheduleData

sealed class ScheduleGroup {

    companion object {

        fun getGroups(schedules: List<Schedule>): List<ScheduleGroup> {
            val scheduleGroups = mutableListOf<ScheduleGroup>()

            val weeklySchedules = mutableMapOf<TimePair, MutableList<WeeklySchedule>>()

            schedules.forEach {
                when (it) {
                    is SingleSchedule -> scheduleGroups.add(Single(it))
                    is WeeklySchedule -> {
                        if (!weeklySchedules.containsKey(it.timePair))
                            weeklySchedules[it.timePair] = mutableListOf()
                        weeklySchedules.getValue(it.timePair).add(it)
                    }
                    is MonthlyDaySchedule -> scheduleGroups.add(MonthlyDay(it))
                    is MonthlyWeekSchedule -> scheduleGroups.add(MonthlyWeek(it))
                }
            }

            scheduleGroups.addAll(weeklySchedules.map { Weekly(it.key, it.value) })
            return scheduleGroups
        }
    }

    abstract val customTimeKey: CustomTimeKey<*>?

    abstract val scheduleData: ScheduleData

    abstract val schedules: List<Schedule>

    class Single(private val singleSchedule: SingleSchedule) : ScheduleGroup() {

        override val customTimeKey get() = singleSchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.Single(
                    singleSchedule.date,
                    singleSchedule.timePair)

        override val schedules get() = listOf(singleSchedule)
    }

    class Weekly(val timePair: TimePair, private val weeklySchedules: List<WeeklySchedule>) : ScheduleGroup() {

        override val customTimeKey get() = timePair.customTimeKey

        override val scheduleData
            get() = ScheduleData.Weekly(
                    weeklySchedules.flatMap { it.daysOfWeek }.toSet(),
                    timePair)

        override val schedules get() = weeklySchedules
    }

    class MonthlyDay(private val monthlyDaySchedule: MonthlyDaySchedule) : ScheduleGroup() {

        override val customTimeKey get() = monthlyDaySchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.MonthlyDay(
                    monthlyDaySchedule.dayOfMonth,
                    monthlyDaySchedule.beginningOfMonth,
                    monthlyDaySchedule.timePair)

        override val schedules get() = listOf(monthlyDaySchedule)
    }

    class MonthlyWeek(private val monthlyWeekSchedule: MonthlyWeekSchedule) : ScheduleGroup() {

        override val customTimeKey get() = monthlyWeekSchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.MonthlyWeek(
                    monthlyWeekSchedule.dayOfMonth,
                    monthlyWeekSchedule.dayOfWeek,
                    monthlyWeekSchedule.beginningOfMonth,
                    monthlyWeekSchedule.timePair)

        override val schedules get() = listOf(monthlyWeekSchedule)
    }
}