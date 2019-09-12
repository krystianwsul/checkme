package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.domain.schedules.ScheduleGroup
import com.krystianwsul.common.firebase.models.RemoteProject
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.time.NormalTime

sealed class ScheduleText {

    companion object : RemoteTask.ScheduleTextFactory {

        override fun getScheduleText(scheduleGroup: ScheduleGroup, remoteProject: RemoteProject<*>) = when (scheduleGroup) {
            is ScheduleGroup.Single -> Single(scheduleGroup)
            is ScheduleGroup.Weekly -> Weekly(scheduleGroup)
            is ScheduleGroup.MonthlyDay -> MonthlyDay(scheduleGroup)
            is ScheduleGroup.MonthlyWeek -> MonthlyWeek(scheduleGroup)
        }.getScheduleText(remoteProject)
    }

    abstract fun getScheduleText(remoteProject: RemoteProject<*>): String

    class Single(private val scheduleGroup: ScheduleGroup.Single) : ScheduleText() {

        override fun getScheduleText(remoteProject: RemoteProject<*>) = scheduleGroup.singleSchedule
                .dateTime
                .getDisplayText()
    }

    class Weekly(private val scheduleGroup: ScheduleGroup.Weekly) : ScheduleText() {

        override fun getScheduleText(remoteProject: RemoteProject<*>): String {
            val days = scheduleGroup.daysOfWeek.let {
                if (it == ScheduleGroup.allDaysOfWeek)
                    MyApplication.instance.getString(R.string.daily)
                else
                    it.sorted().joinToString(", ")
            }

            val time = scheduleGroup.run {
                timePair.customTimeKey?.let {
                    remoteProject.getRemoteCustomTime(it.remoteCustomTimeId)
                } ?: NormalTime(timePair.hourMinute!!)
            }

            return "$days: $time"
        }
    }

    class MonthlyDay(private val scheduleGroup: ScheduleGroup.MonthlyDay) : ScheduleText() {

        override fun getScheduleText(remoteProject: RemoteProject<*>) = MyApplication.instance.run {
            val day = scheduleGroup.monthlyDaySchedule.dayOfMonth.toString() + " " + getString(R.string.monthDay) + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (scheduleGroup.monthlyDaySchedule.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)

            "$day: ${scheduleGroup.monthlyDaySchedule.time}"
        }
    }

    class MonthlyWeek(private val scheduleGroup: ScheduleGroup.MonthlyWeek) : ScheduleText() {

        override fun getScheduleText(remoteProject: RemoteProject<*>) = MyApplication.instance.run {
            val day = scheduleGroup.monthlyWeekSchedule.dayOfMonth.toString() + " " + scheduleGroup.monthlyWeekSchedule.dayOfWeek + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (scheduleGroup.monthlyWeekSchedule.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)

            "$day: ${scheduleGroup.monthlyWeekSchedule.time}"
        }
    }
}