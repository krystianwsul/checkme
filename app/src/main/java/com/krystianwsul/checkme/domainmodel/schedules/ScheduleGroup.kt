package com.krystianwsul.checkme.domainmodel.schedules

import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.time.TimePair

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

    class Single(val singleSchedule: SingleSchedule) : ScheduleGroup() {

        override val customTimeKey get() = singleSchedule.customTimeKey
    }

    class Weekly(val timePair: TimePair, val weeklySchedules: List<WeeklySchedule>) : ScheduleGroup() {

        override val customTimeKey get() = timePair.customTimeKey
    }

    class MonthlyDay(val monthlyDaySchedule: MonthlyDaySchedule) : ScheduleGroup() {

        override val customTimeKey get() = monthlyDaySchedule.customTimeKey
    }

    class MonthlyWeek(val monthlyWeekSchedule: MonthlyWeekSchedule) : ScheduleGroup() {

        override val customTimeKey get() = monthlyWeekSchedule.customTimeKey
    }
}