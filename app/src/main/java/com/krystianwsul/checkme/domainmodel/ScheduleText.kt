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
import com.krystianwsul.common.time.Date
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
        }.getScheduleText(remoteProject)

        fun fromUntil(from: Date?, until: Date?): String {
            fun getString(@StringRes id: Int) = MyApplication.instance
                    .getString(id)
                    .toLowerCase(Locale.getDefault())

            val fromStr by lazy { from!!.getDisplayText() }
            val untilStr by lazy { until!!.getDisplayText() }

            val ret = when {
                from != null && until != null -> "$fromStr - $untilStr"
                from != null -> "${getString(R.string.from)} $fromStr"
                until != null -> "${getString(R.string.until)} $untilStr"
                else -> null
            }

            return ret?.let { " ($it)" } ?: ""
        }
    }

    abstract fun getScheduleText(remoteProject: RemoteProject<*>): String

    protected fun timePairCallback(timePair: TimePair, remoteProject: RemoteProject<*>): String {
        return (timePair.customTimeKey?.let {
            remoteProject.getRemoteCustomTime(it.remoteCustomTimeId)
        } ?: NormalTime(timePair.hourMinute!!)).toString()
    }

    class Single(private val scheduleGroup: ScheduleGroup.Single) : ScheduleText() {

        companion object {

            fun getScheduleText(
                    scheduleData: ScheduleData.Single,
                    timePairCallback: (TimePair) -> String
            ) = scheduleData.date.getDisplayText() + ", " + timePairCallback(scheduleData.timePair)
        }

        override fun getScheduleText(remoteProject: RemoteProject<*>) = Companion.getScheduleText(scheduleGroup.scheduleData) {
            timePairCallback(it, remoteProject)
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
                return "$days$time" + fromUntil(scheduleData.from, scheduleData.until)
            }
        }

        override fun getScheduleText(remoteProject: RemoteProject<*>): String {
            return Companion.getScheduleText(scheduleGroup.scheduleData) {
                timePairCallback(it, remoteProject)
            }
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
                } + ": " + timePairCallback(scheduleData.timePair) + fromUntil(scheduleData.from, scheduleData.until)
            }
        }

        override fun getScheduleText(remoteProject: RemoteProject<*>) = Companion.getScheduleText(scheduleGroup.scheduleData) {
            timePairCallback(it, remoteProject)
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
                } + ": " + timePairCallback(scheduleData.timePair) + fromUntil(scheduleData.from, scheduleData.until)
            }
        }

        override fun getScheduleText(remoteProject: RemoteProject<*>) = Companion.getScheduleText(scheduleGroup.scheduleData) {
            timePairCallback(it, remoteProject)
        }
    }
}