package com.krystianwsul.checkme.domainmodel.schedules

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.models.RemoteProject
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel.ScheduleData
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.NormalTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeKey

sealed class ScheduleGroup {

    companion object {

        private val allDaysOfWeek by lazy { DayOfWeek.values().toSet() }

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

    abstract fun getScheduleText(remoteProject: RemoteProject<*>): String

    class Single(private val singleSchedule: SingleSchedule) : ScheduleGroup() {

        override val customTimeKey get() = singleSchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.Single(
                    singleSchedule.date,
                    singleSchedule.timePair)

        override val schedules get() = listOf(singleSchedule)

        override fun getScheduleText(remoteProject: RemoteProject<*>) = singleSchedule.dateTime.getDisplayText()
    }

    class Weekly(val timePair: TimePair, private val weeklySchedules: List<WeeklySchedule>) : ScheduleGroup() {

        override val customTimeKey get() = timePair.customTimeKey

        val daysOfWeek get() = weeklySchedules.flatMap { it.daysOfWeek }.toSet()

        override val scheduleData get() = ScheduleData.Weekly(daysOfWeek, timePair)

        override val schedules get() = weeklySchedules

        override fun getScheduleText(remoteProject: RemoteProject<*>): String {
            val days = daysOfWeek.let {
                if (it == allDaysOfWeek)
                    MyApplication.instance.getString(R.string.daily)
                else
                    daysOfWeek.sorted().joinToString(", ")
            }

            val time = timePair.customTimeKey?.let {
                remoteProject.getRemoteCustomTime(it.remoteCustomTimeId)
            } ?: NormalTime(timePair.hourMinute!!)

            return "$days: $time"
        }
    }

    class MonthlyDay(private val monthlyDaySchedule: MonthlyDaySchedule) : ScheduleGroup() {

        override val customTimeKey get() = monthlyDaySchedule.customTimeKey

        override val scheduleData
            get() = ScheduleData.MonthlyDay(
                    monthlyDaySchedule.dayOfMonth,
                    monthlyDaySchedule.beginningOfMonth,
                    monthlyDaySchedule.timePair)

        override val schedules get() = listOf(monthlyDaySchedule)

        override fun getScheduleText(remoteProject: RemoteProject<*>) = MyApplication.instance.run {
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

        override fun getScheduleText(remoteProject: RemoteProject<*>) = MyApplication.instance.run {
            val day = monthlyWeekSchedule.dayOfMonth.toString() + " " + monthlyWeekSchedule.dayOfWeek + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (monthlyWeekSchedule.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)

            "$day: ${monthlyWeekSchedule.time}"
        }
    }
}