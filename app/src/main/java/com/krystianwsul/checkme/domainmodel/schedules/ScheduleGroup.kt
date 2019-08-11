package com.krystianwsul.checkme.domainmodel.schedules

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
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

    abstract fun getScheduleText(domainFactory: DomainFactory): String

    class Single(private val singleSchedule: SingleSchedule) : ScheduleGroup() {

        override val customTimeKey get() = singleSchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.Single(
                    singleSchedule.date,
                    singleSchedule.timePair)

        override val schedules get() = listOf(singleSchedule)

        override fun getScheduleText(domainFactory: DomainFactory) = singleSchedule.date.getDisplayText()
    }

    class Weekly(val timePair: TimePair, private val weeklySchedules: List<WeeklySchedule>) : ScheduleGroup() {

        override val customTimeKey get() = timePair.customTimeKey

        private val daysOfWeek get() = weeklySchedules.flatMap { it.daysOfWeek }.toSet()

        override val scheduleData get() = ScheduleData.Weekly(daysOfWeek, timePair)

        override val schedules get() = weeklySchedules

        override fun getScheduleText(domainFactory: DomainFactory) = daysOfWeek.joinToString(", ") + ": " + domainFactory.getTime(timePair)
    }

    class MonthlyDay(private val monthlyDaySchedule: MonthlyDaySchedule) : ScheduleGroup() {

        override val customTimeKey get() = monthlyDaySchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.MonthlyDay(
                    monthlyDaySchedule.dayOfMonth,
                    monthlyDaySchedule.beginningOfMonth,
                    monthlyDaySchedule.timePair)

        override val schedules get() = listOf(monthlyDaySchedule)

        override fun getScheduleText(domainFactory: DomainFactory) = MyApplication.instance.run {
            val day = monthlyDaySchedule.dayOfMonth.toString() + " " + getString(R.string.monthDay) + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (monthlyDaySchedule.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)

            "$day: ${monthlyDaySchedule.time}"
        }
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

        override fun getScheduleText(domainFactory: DomainFactory) = MyApplication.instance.run {
            val day = monthlyWeekSchedule.dayOfMonth.toString() + " " + monthlyWeekSchedule.dayOfWeek + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (monthlyWeekSchedule.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)

            "$day: ${monthlyWeekSchedule.time}"
        }
    }
}