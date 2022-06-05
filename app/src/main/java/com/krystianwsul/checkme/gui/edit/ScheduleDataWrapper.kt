package com.krystianwsul.checkme.gui.edit

import android.content.Context
import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogData
import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.getDateInMonth
import com.soywiz.klock.Month
import kotlinx.parcelize.Parcelize

sealed class ScheduleDataWrapper : Parcelable {

    companion object {

        fun fromScheduleGroup(scheduleGroup: ScheduleGroup) = when (scheduleGroup) {
            is ScheduleGroup.Single -> Single.fromScheduleGroup(scheduleGroup)
            is ScheduleGroup.Weekly -> Weekly.fromScheduleGroup(scheduleGroup)
            is ScheduleGroup.MonthlyDay -> MonthlyDay.fromScheduleGroup(scheduleGroup)
            is ScheduleGroup.MonthlyWeek -> MonthlyWeek.fromScheduleGroup(scheduleGroup)
            is ScheduleGroup.Yearly -> Yearly.fromScheduleGroup(scheduleGroup)
            is ScheduleGroup.Child -> Child.fromScheduleGroup(scheduleGroup)
        }

        private fun timePairCallback(
            timePair: TimePair,
            customTimeDatas: Map<CustomTimeKey, EditViewModel.CustomTimeData>,
            dayOfWeek: DayOfWeek? = null,
        ): String {
            return timePair.customTimeKey
                ?.let {
                    customTimeDatas.getValue(it).let {
                        it.name + (dayOfWeek?.let { _ -> " (" + it.hourMinutes[dayOfWeek] + ")" } ?: "")
                    }
                }
                ?: timePair.hourMinute!!.toString()
        }

        fun dayFromEndOfMonth(date: Date) = Month(date.month).days(date.year) - date.day + 1

        fun dateToDayFromBeginningOrEnd(date: Date): Pair<Int, Boolean> {
            return if (date.day > ScheduleDialogData.MAX_MONTH_DAY) {
                dayFromEndOfMonth(date) to false
            } else {
                date.day to true
            }
        }

        fun dayOfMonthToWeekOfMonth(day: Int) = (day - 1) / 7 + 1
    }

    abstract val scheduleData: ScheduleData

    val timePair get() = scheduleData.timePair

    abstract fun getText(customTimeDatas: Map<CustomTimeKey, EditViewModel.CustomTimeData>, context: Context): String

    fun getScheduleDialogData(scheduleHint: EditParentHint.Schedule?) =
        getScheduleDialogDataHelper(scheduleHint?.date ?: Date.today())

    protected abstract fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData

    @Parcelize
    data class Single(override val scheduleData: ScheduleData.Single) : ScheduleDataWrapper() {

        companion object {

            fun fromScheduleGroup(scheduleGroup: ScheduleGroup.Single) = Single(scheduleGroup.scheduleData)
        }

        override fun getText(
            customTimeDatas: Map<CustomTimeKey, EditViewModel.CustomTimeData>,
            context: Context,
        ): String {
            return ScheduleText.Single.getScheduleText(scheduleData) {
                timePairCallback(it, customTimeDatas, scheduleData.date.dayOfWeek)
            }
        }

        override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
            val (monthDayNumber, beginningOfMonth) = dateToDayFromBeginningOrEnd(scheduleData.date)

            @Suppress("BooleanLiteralArgument")
            return ScheduleDialogData(
                scheduleData.date,
                setOf(scheduleData.date.dayOfWeek),
                true,
                monthDayNumber,
                dayOfMonthToWeekOfMonth(monthDayNumber),
                scheduleData.date.dayOfWeek,
                beginningOfMonth,
                TimePairPersist(timePair),
                ScheduleDialogData.Type.SINGLE,
                null,
                null,
                1,
                null,
            )
        }
    }

    @Parcelize
    data class Weekly(override val scheduleData: ScheduleData.Weekly) : ScheduleDataWrapper() {

        companion object {

            fun fromScheduleGroup(scheduleGroup: ScheduleGroup.Weekly) = Weekly(scheduleGroup.scheduleData)
        }

        override fun getText(
            customTimeDatas: Map<CustomTimeKey, EditViewModel.CustomTimeData>,
            context: Context,
        ): String {
            return ScheduleText.Weekly.getScheduleText(scheduleData) {
                timePairCallback(it, customTimeDatas)
            }
        }

        override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
            val (monthDayNumber, beginningOfMonth) = dateToDayFromBeginningOrEnd(suggestedDate)

            val type = if (scheduleData.daysOfWeek == DayOfWeek.set && scheduleData.interval == 1)
                ScheduleDialogData.Type.DAILY
            else
                ScheduleDialogData.Type.WEEKLY

            return ScheduleDialogData(
                suggestedDate,
                scheduleData.daysOfWeek,
                true,
                monthDayNumber,
                dayOfMonthToWeekOfMonth(monthDayNumber),
                suggestedDate.dayOfWeek,
                beginningOfMonth,
                TimePairPersist(timePair),
                type,
                scheduleData.from,
                scheduleData.until,
                scheduleData.interval,
                null,
            )
        }
    }

    @Parcelize
    data class MonthlyDay(override val scheduleData: ScheduleData.MonthlyDay) : ScheduleDataWrapper() {

        companion object {

            fun fromScheduleGroup(scheduleGroup: ScheduleGroup.MonthlyDay) = MonthlyDay(scheduleGroup.scheduleData)
        }

        override fun getText(
            customTimeDatas: Map<CustomTimeKey, EditViewModel.CustomTimeData>,
            context: Context,
        ): String {
            return ScheduleText.MonthlyDay.getScheduleText(scheduleData) {
                timePairCallback(it, customTimeDatas)
            }
        }

        override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
            val date = getDateInMonth(
                suggestedDate.year,
                suggestedDate.month,
                scheduleData.dayOfMonth,
                scheduleData.beginningOfMonth
            )

            @Suppress("BooleanLiteralArgument")
            return ScheduleDialogData(
                date,
                setOf(date.dayOfWeek),
                true,
                scheduleData.dayOfMonth,
                dayOfMonthToWeekOfMonth(scheduleData.dayOfMonth),
                date.dayOfWeek,
                scheduleData.beginningOfMonth,
                TimePairPersist(timePair),
                ScheduleDialogData.Type.MONTHLY,
                scheduleData.from,
                scheduleData.until,
                1,
                null,
            )
        }
    }

    @Parcelize
    data class MonthlyWeek(override val scheduleData: ScheduleData.MonthlyWeek) : ScheduleDataWrapper() {

        companion object {

            fun fromScheduleGroup(scheduleGroup: ScheduleGroup.MonthlyWeek) = MonthlyWeek(scheduleGroup.scheduleData)
        }

        override fun getText(
            customTimeDatas: Map<CustomTimeKey, EditViewModel.CustomTimeData>,
            context: Context,
        ): String {
            return ScheduleText.MonthlyWeek.getScheduleText(scheduleData) {
                timePairCallback(it, customTimeDatas)
            }
        }

        override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
            val date = getDateInMonth(
                suggestedDate.year,
                suggestedDate.month,
                scheduleData.weekOfMonth,
                scheduleData.dayOfWeek,
                scheduleData.beginningOfMonth
            )

            val dayNumber = if (scheduleData.beginningOfMonth)
                date.day
            else
                dayFromEndOfMonth(date)

            @Suppress("BooleanLiteralArgument")
            return ScheduleDialogData(
                date,
                setOf(scheduleData.dayOfWeek),
                false,
                listOf(dayNumber, ScheduleDialogData.MAX_MONTH_DAY).minOrNull()!!,
                scheduleData.weekOfMonth,
                scheduleData.dayOfWeek,
                scheduleData.beginningOfMonth,
                TimePairPersist(timePair),
                ScheduleDialogData.Type.MONTHLY,
                scheduleData.from,
                scheduleData.until,
                1,
                null,
            )
        }
    }

    @Parcelize
    data class Yearly(override val scheduleData: ScheduleData.Yearly) : ScheduleDataWrapper() {

        companion object {

            fun fromScheduleGroup(scheduleGroup: ScheduleGroup.Yearly) = Yearly(scheduleGroup.scheduleData)
        }

        override fun getText(
            customTimeDatas: Map<CustomTimeKey, EditViewModel.CustomTimeData>,
            context: Context,
        ): String {
            return ScheduleText.Yearly.getScheduleText(scheduleData) {
                timePairCallback(it, customTimeDatas)
            }
        }

        override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
            val date = getDateInMonth(
                suggestedDate.year,
                scheduleData.month,
                scheduleData.day,
                true,
            )

            val (monthDayNumber, beginningOfMonth) = dateToDayFromBeginningOrEnd(date)

            @Suppress("BooleanLiteralArgument")
            return ScheduleDialogData(
                date,
                setOf(date.dayOfWeek),
                true,
                monthDayNumber,
                dayOfMonthToWeekOfMonth(monthDayNumber),
                date.dayOfWeek,
                beginningOfMonth,
                TimePairPersist(timePair),
                ScheduleDialogData.Type.YEARLY,
                scheduleData.from,
                scheduleData.until,
                1,
                null,
            )
        }
    }

    @Parcelize
    data class Child(
        override val scheduleData: ScheduleData,
        val parentInstanceName: String,
        val parentInstanceDateTimePair: DateTimePair,
    ) : ScheduleDataWrapper() {

        companion object {

            fun fromScheduleGroup(scheduleGroup: ScheduleGroup.Child) = scheduleGroup.run {
                Child(scheduleData, parentInstance.name, parentInstance.instanceDateTime.toDateTimePair())
            }
        }

        override fun getText(
            customTimeDatas: Map<CustomTimeKey, EditViewModel.CustomTimeData>,
            context: Context,
        ) = ScheduleText.Child.getScheduleText(parentInstanceName, parentInstanceDateTimePair) {
            timePairCallback(it, customTimeDatas)
        }

        override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
            val date = parentInstanceDateTimePair.date
            val dayOfWeek = date.dayOfWeek

            val (monthDayNumber, beginningOfMonth) = dateToDayFromBeginningOrEnd(date)

            return ScheduleDialogData(
                date,
                setOf(dayOfWeek),
                true,
                monthDayNumber,
                dayOfMonthToWeekOfMonth(monthDayNumber),
                dayOfWeek,
                beginningOfMonth,
                TimePairPersist(parentInstanceDateTimePair.timePair),
                ScheduleDialogData.Type.CHILD,
                null,
                null,
                1,
                ScheduleDialogData.ParentInstanceData(parentInstanceName),
            )
        }
    }
}