package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.TimePairPersist
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel


class SingleScheduleEntry(single: CreateTaskViewModel.ScheduleData.Single) : ScheduleEntry() {

    val date = single.date
    val timePair = single.timePair

    override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CreateTaskViewModel.CustomTimeData>, context: Context): String {
        return date.getDisplayText() + ", " + if (timePair.customTimeKey != null) {
            check(timePair.hourMinute == null)

            val customTimeData = customTimeDatas.getValue(timePair.customTimeKey)

            customTimeData.name + " (" + customTimeData.hourMinutes[date.dayOfWeek] + ")"
        } else {
            timePair.hourMinute!!.toString()
        }
    }

    override val scheduleData get() = CreateTaskViewModel.ScheduleData.Single(date, timePair)

    override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
        var monthDayNumber = date.day
        var beginningOfMonth = true
        if (monthDayNumber > 28) {
            monthDayNumber = Utils.getDaysInMonth(date.year, date.month) - monthDayNumber + 1
            beginningOfMonth = false
        }
        val monthWeekNumber = (monthDayNumber - 1) / 7 + 1

        return ScheduleDialogFragment.ScheduleDialogData(date, mutableSetOf(date.dayOfWeek), true, monthDayNumber, monthWeekNumber, date.dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.SINGLE)
    }

    override val scheduleType = ScheduleType.SINGLE
}
