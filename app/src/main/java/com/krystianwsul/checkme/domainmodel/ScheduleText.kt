package com.krystianwsul.checkme.domainmodel

import androidx.annotation.StringRes
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.prettyPrint
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.domain.schedules.ScheduleGroup
import com.krystianwsul.common.firebase.models.RemoteProject
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.NormalTime
import com.krystianwsul.common.time.TimePair
import java.util.*

sealed class ScheduleText {

    companion object : RemoteTask.ScheduleTextFactory {

        override fun getScheduleText(scheduleGroup: ScheduleGroup, remoteProject: RemoteProject<*>) = when (scheduleGroup) {
            is ScheduleGroup.Single -> Single(scheduleGroup)
            is ScheduleGroup.Weekly -> Weekly(scheduleGroup)
            is ScheduleGroup.MonthlyDay -> MonthlyDay(scheduleGroup)
            is ScheduleGroup.MonthlyWeek -> MonthlyWeek(scheduleGroup)
        }.getScheduleText(remoteProject) // todo from show from/until in rows in create task
    }

    private fun getString(@StringRes id: Int) = MyApplication.instance
            .getString(id)
            .toLowerCase(Locale.getDefault())

    protected val fromStr by lazy { getString(R.string.from) }
    protected val untilStr by lazy { getString(R.string.until) }

    abstract fun getScheduleText(remoteProject: RemoteProject<*>): String

    class Single(private val scheduleGroup: ScheduleGroup.Single) : ScheduleText() {

        override fun getScheduleText(remoteProject: RemoteProject<*>) = scheduleGroup.singleSchedule
                .dateTime
                .getDisplayText()
    }

    class Weekly(private val scheduleGroup: ScheduleGroup.Weekly) : ScheduleText() {

        companion object {

            fun getScheduleText(
                    daysOfWeek: Set<DayOfWeek>,
                    timePair: TimePair,
                    timePairCallback: (TimePair) -> String
            ): String {
                val days = daysOfWeek.prettyPrint()
                val time = timePairCallback(timePair)
                return "$days$time"
            }
        }

        override fun getScheduleText(remoteProject: RemoteProject<*>): String {
            val text = Companion.getScheduleText(scheduleGroup.daysOfWeek, scheduleGroup.timePair) {
                (it.customTimeKey?.let {
                    remoteProject.getRemoteCustomTime(it.remoteCustomTimeId)
                } ?: NormalTime(it.hourMinute!!)).toString()
            }

            val from = scheduleGroup.from
                    ?.let { " $fromStr ${it.getDisplayText(false)}" }
                    ?: ""

            val until = scheduleGroup.until
                    ?.let { " $untilStr ${it.getDisplayText(false)}" }
                    ?: ""

            return "$text$from$until"
        }
    }

    class MonthlyDay(private val scheduleGroup: ScheduleGroup.MonthlyDay) : ScheduleText() {

        override fun getScheduleText(remoteProject: RemoteProject<*>) = MyApplication.instance.run {
            val day = scheduleGroup.monthlyDaySchedule.dayOfMonth.toString() + " " + getString(R.string.monthDay) + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (scheduleGroup.monthlyDaySchedule.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)

            val from = scheduleGroup.monthlyDaySchedule
                    .from
                    ?.let { " $fromStr ${it.getDisplayText(false)}" }

            val until = scheduleGroup.monthlyDaySchedule
                    .until
                    ?.let { " $untilStr ${it.getDisplayText(false)}" }
                    ?: ""

            "$day: ${scheduleGroup.monthlyDaySchedule.time}$from$until"
        }
    }

    class MonthlyWeek(private val scheduleGroup: ScheduleGroup.MonthlyWeek) : ScheduleText() {

        override fun getScheduleText(remoteProject: RemoteProject<*>) = MyApplication.instance.run {
            val day = scheduleGroup.monthlyWeekSchedule.dayOfMonth.toString() + " " + scheduleGroup.monthlyWeekSchedule.dayOfWeek + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (scheduleGroup.monthlyWeekSchedule.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)

            val from = scheduleGroup.monthlyWeekSchedule
                    .from
                    ?.let { " $fromStr ${it.getDisplayText(false)}" }

            val until = scheduleGroup.monthlyWeekSchedule
                    .until
                    ?.let { " $untilStr ${it.getDisplayText(false)}" }
                    ?: ""

            "$day: ${scheduleGroup.monthlyWeekSchedule.time}$from$until"
        }
    }
}