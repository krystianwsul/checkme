package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.prettyPrint
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimePairPersist
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel


class WeeklyScheduleEntry(weekly: CreateTaskViewModel.ScheduleData.Weekly) : ScheduleEntry() {

    private val daysOfWeek: Set<DayOfWeek>
    private val timePair: TimePair

    init {
        daysOfWeek = weekly.daysOfWeek
        timePair = weekly.timePair.copy()
    }

    override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CreateTaskViewModel.CustomTimeData>, context: Context): String {
        return daysOfWeek.prettyPrint() + if (timePair.customTimeKey != null) {
            check(timePair.hourMinute == null)

            customTimeDatas.getValue(timePair.customTimeKey).name
        } else {
            timePair.hourMinute!!.toString()
        }
    }

    override val scheduleData get() = CreateTaskViewModel.ScheduleData.Weekly(daysOfWeek, timePair)

    override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
        val date = scheduleHint?.date ?: today

        var monthDayNumber = date.day
        var beginningOfMonth = true
        if (monthDayNumber > 28) {
            monthDayNumber = Utils.getDaysInMonth(date.year, date.month) - monthDayNumber + 1
            beginningOfMonth = false
        }
        val monthWeekNumber = (monthDayNumber - 1) / 7 + 1

        return ScheduleDialogFragment.ScheduleDialogData(date, daysOfWeek.toMutableSet(), true, monthDayNumber, monthWeekNumber, date.dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.WEEKLY)
    }

    override val scheduleType = ScheduleType.WEEKLY
}
