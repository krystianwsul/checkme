package com.krystianwsul.checkme.domainmodel

import androidx.annotation.StringRes
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.prettyPrint
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ScheduleData
import org.joda.time.LocalDate
import java.util.*

sealed class ScheduleText {

    companion object : Task.ScheduleTextFactory {

        override fun getScheduleText(
                scheduleGroup: ScheduleGroup,
                customTimeProvider: JsonTime.CustomTimeProvider,
        ) = when (scheduleGroup) {
            is ScheduleGroup.Single -> Single(scheduleGroup)
            is ScheduleGroup.Weekly -> Weekly(scheduleGroup)
            is ScheduleGroup.MonthlyDay -> MonthlyDay(scheduleGroup)
            is ScheduleGroup.MonthlyWeek -> MonthlyWeek(scheduleGroup)
            is ScheduleGroup.Yearly -> Yearly(scheduleGroup)
            is ScheduleGroup.Child -> Child(scheduleGroup)
        }.getScheduleText(customTimeProvider)

        fun fromUntil(from: Date?, until: Date?, intervalText: String? = null): String {
            fun getString(@StringRes id: Int) = MyApplication.instance
                .getString(id)
                .lowercase(Locale.getDefault())

            val fromStr by lazy { from!!.getDisplayText() }
            val untilStr by lazy { until!!.getDisplayText() }

            val ret = when {
                from != null && until != null -> "$fromStr - $untilStr"
                from != null -> "${getString(R.string.from)} $fromStr"
                until != null -> "${getString(R.string.until)} $untilStr"
                else -> null
            }?.let {
                it + (intervalText?.let { ", $it" } ?: "")
            }

            return ret?.let { " ($it)" } ?: ""
        }
    }

    abstract fun getScheduleText(customTimeProvider: JsonTime.CustomTimeProvider): String

    protected fun timePairCallback(timePair: TimePair, customTimeProvider: JsonTime.CustomTimeProvider): String {
        val time = timePair.customTimeKey
                ?.let { customTimeProvider.getCustomTime(it) }
                ?: Time.Normal(timePair.hourMinute!!)

        return time.toString()
    }

    class Single(private val scheduleGroup: ScheduleGroup.Single) : ScheduleText() {

        companion object {

            fun getScheduleText(
                dateTimePair: DateTimePair,
                timePairCallback: (TimePair) -> String,
            ) = dateTimePair.run { date.getDisplayText() + ", " + timePairCallback(timePair) }

            fun getScheduleText(
                scheduleData: ScheduleData.Single,
                timePairCallback: (TimePair) -> String,
            ) = getScheduleText(scheduleData.run { DateTimePair(date, timePair) }, timePairCallback)
        }

        override fun getScheduleText(customTimeProvider: JsonTime.CustomTimeProvider) =
                Companion.getScheduleText(scheduleGroup.scheduleData) { timePairCallback(it, customTimeProvider) }
    }

    class Weekly(private val scheduleGroup: ScheduleGroup.Weekly) : ScheduleText() {

        companion object {

            fun getScheduleText(
                    scheduleData: ScheduleData.Weekly,
                    timePairCallback: (TimePair) -> String,
            ): String {
                val intervalText = scheduleData.interval
                        .takeIf { it > 1 }
                        ?.let { MyApplication.instance.getString(R.string.everyXWeeks, scheduleData.interval) }

                val days = scheduleData.daysOfWeek.prettyPrint()
                val time = timePairCallback(scheduleData.timePair)
                return "$days$time" + fromUntil(scheduleData.from, scheduleData.until, intervalText)
            }
        }

        override fun getScheduleText(customTimeProvider: JsonTime.CustomTimeProvider): String {
            return Companion.getScheduleText(scheduleGroup.scheduleData) {
                timePairCallback(it, customTimeProvider)
            }
        }
    }

    class MonthlyDay(private val scheduleGroup: ScheduleGroup.MonthlyDay) : ScheduleText() {

        companion object {

            fun getScheduleText(
                    scheduleData: ScheduleData.MonthlyDay,
                    timePairCallback: (TimePair) -> String,
            ): String {
                return MyApplication.instance.run {
                    Utils.ordinal(scheduleData.dayOfMonth) + " " + getString(R.string.monthDay) + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (scheduleData.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)
                } + ": " + timePairCallback(scheduleData.timePair) + fromUntil(scheduleData.from, scheduleData.until)
            }
        }

        override fun getScheduleText(customTimeProvider: JsonTime.CustomTimeProvider) =
                Companion.getScheduleText(scheduleGroup.scheduleData) { timePairCallback(it, customTimeProvider) }
    }

    class MonthlyWeek(private val scheduleGroup: ScheduleGroup.MonthlyWeek) : ScheduleText() {

        companion object {

            fun getScheduleText(
                    scheduleData: ScheduleData.MonthlyWeek,
                    timePairCallback: (TimePair) -> String,
            ): String {
                return MyApplication.instance.run {
                    Utils.ordinal(scheduleData.weekOfMonth) + " " + scheduleData.dayOfWeek + " " + getString(R.string.monthDayStart) + " " + resources.getStringArray(R.array.month)[if (scheduleData.beginningOfMonth) 0 else 1] + " " + getString(R.string.monthDayEnd)
                } + ": " + timePairCallback(scheduleData.timePair) + fromUntil(scheduleData.from, scheduleData.until)
            }
        }

        override fun getScheduleText(customTimeProvider: JsonTime.CustomTimeProvider) =
                Companion.getScheduleText(scheduleGroup.scheduleData) { timePairCallback(it, customTimeProvider) }
    }

    class Yearly(private val scheduleGroup: ScheduleGroup.Yearly) : ScheduleText() {

        companion object {

            fun getDateText(month: Int, day: Int) = LocalDate(0, month, day).toString("MMMM d")!!

            fun getScheduleText(
                scheduleData: ScheduleData.Yearly,
                timePairCallback: (TimePair) -> String,
            ) = getDateText(scheduleData.month, scheduleData.day) +
                    ": " +
                    timePairCallback(scheduleData.timePair) +
                    fromUntil(scheduleData.from, scheduleData.until)
        }

        override fun getScheduleText(customTimeProvider: JsonTime.CustomTimeProvider) =
            Companion.getScheduleText(scheduleGroup.scheduleData) { timePairCallback(it, customTimeProvider) }
    }

    class Child(private val scheduleGroup: ScheduleGroup.Child) : ScheduleText() {

        companion object {

            fun getScheduleText(
                parentInstanceName: String,
                parentInstanceDateTimePair: DateTimePair,
                timePairCallback: (TimePair) -> String,
            ): String {
                val dateTimeText = Single.getScheduleText(parentInstanceDateTimePair, timePairCallback)

                return "Shown in: $parentInstanceName ($dateTimeText)" // todo join resources
            }
        }

        override fun getScheduleText(customTimeProvider: JsonTime.CustomTimeProvider) = scheduleGroup.parentInstance.run {
            Companion.getScheduleText(name, instanceDateTime.toDateTimePair()) { timePairCallback(it, customTimeProvider) }
        }
    }
}