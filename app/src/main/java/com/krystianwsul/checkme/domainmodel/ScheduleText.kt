package com.krystianwsul.checkme.domainmodel

import androidx.annotation.StringRes
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.prettyPrint
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.domain.schedules.ScheduleGroup
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ScheduleData
import java.util.*

sealed class ScheduleText {

    companion object : Task.ScheduleTextFactory {

        override fun getScheduleText(
                scheduleGroup: ScheduleGroup<*>,
                project: Project<*>
        ) = when (scheduleGroup) {
            is ScheduleGroup.Single -> Single(scheduleGroup)
            is ScheduleGroup.Weekly -> Weekly(scheduleGroup)
            is ScheduleGroup.MonthlyDay -> MonthlyDay(scheduleGroup)
            is ScheduleGroup.MonthlyWeek -> MonthlyWeek(scheduleGroup)
        }.getScheduleText(project)

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

    abstract fun getScheduleText(project: Project<*>): String

    protected fun timePairCallback(timePair: TimePair, project: Project<*>): String {
        return (timePair.customTimeKey?.let {
            project.getCustomTime(it.customTimeId)
        } ?: Time.Normal(timePair.hourMinute!!)).toString()
    }

    class Single(private val scheduleGroup: ScheduleGroup.Single<*>) : ScheduleText() {

        companion object {

            fun getScheduleText(
                    scheduleData: ScheduleData.Single,
                    timePairCallback: (TimePair) -> String
            ) = scheduleData.date.getDisplayText() + ", " + timePairCallback(scheduleData.timePair)
        }

        override fun getScheduleText(project: Project<*>) = Companion.getScheduleText(scheduleGroup.scheduleData) {
            timePairCallback(it, project)
        }
    }

    class Weekly(private val scheduleGroup: ScheduleGroup.Weekly<*>) : ScheduleText() {

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

        override fun getScheduleText(project: Project<*>): String {
            return Companion.getScheduleText(scheduleGroup.scheduleData) {
                timePairCallback(it, project)
            }
        }
    }

    class MonthlyDay(private val scheduleGroup: ScheduleGroup.MonthlyDay<*>) : ScheduleText() {

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

        override fun getScheduleText(project: Project<*>) = Companion.getScheduleText(scheduleGroup.scheduleData) {
            timePairCallback(it, project)
        }
    }

    class MonthlyWeek(private val scheduleGroup: ScheduleGroup.MonthlyWeek<*>) : ScheduleText() {

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

        override fun getScheduleText(project: Project<*>) = Companion.getScheduleText(scheduleGroup.scheduleData) {
            timePairCallback(it, project)
        }
    }
}