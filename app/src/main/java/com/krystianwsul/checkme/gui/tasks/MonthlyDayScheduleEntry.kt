package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimePairPersist
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel


class MonthlyDayScheduleEntry : ScheduleEntry {

    private val monthDayNumber: Int
    private val beginningOfMonth: Boolean

    private val timePair: TimePair

    override val scheduleData: CreateTaskViewModel.ScheduleData
        get() = CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData(monthDayNumber, beginningOfMonth, timePair)

    override val scheduleType = ScheduleType.MONTHLY_DAY

    constructor(monthlyDayScheduleData: CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData) {
        monthDayNumber = monthlyDayScheduleData.dayOfMonth
        beginningOfMonth = monthlyDayScheduleData.beginningOfMonth
        timePair = monthlyDayScheduleData.timePair.copy()
    }

    constructor(scheduleDialogData: ScheduleDialogFragment.ScheduleDialogData) {
        check(scheduleDialogData.scheduleType == ScheduleType.MONTHLY_DAY)
        check(scheduleDialogData.monthlyDay)

        monthDayNumber = scheduleDialogData.monthDayNumber
        beginningOfMonth = scheduleDialogData.beginningOfMonth
        timePair = scheduleDialogData.timePairPersist.timePair
    }

    override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CreateTaskViewModel.CustomTimeData>, context: Context): String {
        val day = Utils.ordinal(monthDayNumber) + " " + context.getString(R.string.monthDay) + " " + context.getString(R.string.monthDayStart) + " " + context.resources.getStringArray(R.array.month)[if (beginningOfMonth) 0 else 1] + " " + context.getString(R.string.monthDayEnd)

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

        date = Utils.getDateInMonth(date.year, date.month, monthDayNumber, beginningOfMonth)

        return ScheduleDialogFragment.ScheduleDialogData(date, mutableSetOf(date.dayOfWeek), true, monthDayNumber, (monthDayNumber - 1) / 7 + 1, date.dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.MONTHLY_DAY)
    }
}
