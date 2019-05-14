package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimePairPersist
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel


class MonthlyWeekScheduleEntry(monthlyWeek: CreateTaskViewModel.ScheduleData.MonthlyWeek) : ScheduleEntry() {

    private val monthWeekNumber: Int

    private val monthWeekDay: DayOfWeek

    private val beginningOfMonth: Boolean

    private val timePair: TimePair

    override val scheduleData: CreateTaskViewModel.ScheduleData
        get() = CreateTaskViewModel.ScheduleData.MonthlyWeek(monthWeekNumber, monthWeekDay, beginningOfMonth, timePair)

    override val scheduleType = ScheduleType.MONTHLY_WEEK

    init {
        monthWeekNumber = monthlyWeek.dayOfMonth
        monthWeekDay = monthlyWeek.dayOfWeek
        beginningOfMonth = monthlyWeek.beginningOfMonth
        timePair = monthlyWeek.timePair.copy()
    }

    override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CreateTaskViewModel.CustomTimeData>, context: Context): String {
        val day = Utils.ordinal(monthWeekNumber) + " " + monthWeekDay + " " + context.getString(R.string.monthDayStart) + " " + context.resources.getStringArray(R.array.month)[if (beginningOfMonth) 0 else 1] + " " + context.getString(R.string.monthDayEnd)

        return "$day, " + if (timePair.customTimeKey != null) {
            check(timePair.hourMinute == null)

            val customTimeData = customTimeDatas.getValue(timePair.customTimeKey)

            customTimeData.name
        } else {
            timePair.hourMinute!!.toString()
        }
    }

    override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
        var date = scheduleHint?.date ?: today

        date = Utils.getDateInMonth(date.year, date.month, monthWeekNumber, monthWeekDay, beginningOfMonth)

        return ScheduleDialogFragment.ScheduleDialogData(date, mutableSetOf(monthWeekDay), false, date.day, monthWeekNumber, monthWeekDay, beginningOfMonth, TimePairPersist(timePair), ScheduleType.MONTHLY_WEEK)
    }
}
