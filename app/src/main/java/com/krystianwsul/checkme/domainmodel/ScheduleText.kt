package com.krystianwsul.checkme.domainmodel

import androidx.annotation.StringRes
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.prettyPrint
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.domain.schedules.ScheduleGroup
import com.krystianwsul.common.firebase.models.RemoteProject
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.time.NormalTime
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ScheduleData
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

    abstract fun getScheduleText(remoteProject: RemoteProject<*>): String // todo from use activity context

    class Single(private val scheduleGroup: ScheduleGroup.Single) : ScheduleText() {

        companion object {

            fun getScheduleText(
                    scheduleData: ScheduleData.Single,
                    timePairCallback: (TimePair) -> String
            ) = scheduleData.date.getDisplayText() + ", " + timePairCallback(scheduleData.timePair)
        }

        override fun getScheduleText(remoteProject: RemoteProject<*>) = Companion.getScheduleText(scheduleGroup.scheduleData) {
            (it.customTimeKey?.let {
                remoteProject.getRemoteCustomTime(it.remoteCustomTimeId)
            } ?: NormalTime(it.hourMinute!!)).toString()
        }
    }

    class Weekly(private val scheduleGroup: ScheduleGroup.Weekly) : ScheduleText() {

        companion object {

            fun getScheduleText(
                    scheduleData: ScheduleData.Weekly,
                    timePairCallback: (TimePair) -> String
            ): String {
                val days = scheduleData.daysOfWeek.prettyPrint()
                val time = timePairCallback(scheduleData.timePair)
                return "$days$time"
            }
        }

        override fun getScheduleText(remoteProject: RemoteProject<*>): String {
            val text = Companion.getScheduleText(scheduleGroup.scheduleData) {
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

        companion object {

            fun getScheduleText(
                    scheduleData: ScheduleData.MonthlyDay,
                    timePairCallback: (TimePair) -> String
            ): String {
                return MyApplication.instance.run {
                    Utils.ordinal(scheduleData.dayOfMonth) + " " + getString(R.string.monthDay) + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (scheduleData.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)
                } + ": " + timePairCallback(scheduleData.timePair)
            }
        }

        override fun getScheduleText(remoteProject: RemoteProject<*>) = MyApplication.instance.run {
            val text = Companion.getScheduleText(scheduleGroup.scheduleData) {
                (it.customTimeKey?.let {
                    remoteProject.getRemoteCustomTime(it.remoteCustomTimeId)
                } ?: NormalTime(it.hourMinute!!)).toString()
            }

            val from = scheduleGroup.monthlyDaySchedule
                    .from
                    ?.let { " $fromStr ${it.getDisplayText(false)}" }
                    ?: ""

            val until = scheduleGroup.monthlyDaySchedule
                    .until
                    ?.let { " $untilStr ${it.getDisplayText(false)}" }
                    ?: ""

            "$text$from$until"
        }
    }

    class MonthlyWeek(private val scheduleGroup: ScheduleGroup.MonthlyWeek) : ScheduleText() {

        companion object {

            fun getScheduleText(
                    scheduleData: ScheduleData.MonthlyWeek,
                    timePairCallback: (TimePair) -> String
            ): String {
                return MyApplication.instance.run {
                    Utils.ordinal(scheduleData.dayOfMonth) + " " + scheduleData.dayOfWeek + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (scheduleData.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)
                } + ": " + timePairCallback(scheduleData.timePair)
            }
        }

        override fun getScheduleText(remoteProject: RemoteProject<*>) = MyApplication.instance.run {
            val text = Companion.getScheduleText(scheduleGroup.scheduleData) {
                (it.customTimeKey?.let {
                    remoteProject.getRemoteCustomTime(it.remoteCustomTimeId)
                } ?: NormalTime(it.hourMinute!!)).toString()
            }

            val from = scheduleGroup.monthlyWeekSchedule
                    .from
                    ?.let { " $fromStr ${it.getDisplayText(false)}" }
                    ?: ""

            val until = scheduleGroup.monthlyWeekSchedule
                    .until
                    ?.let { " $untilStr ${it.getDisplayText(false)}" }
                    ?: ""

            "$text$from$until"
        }
    }
}