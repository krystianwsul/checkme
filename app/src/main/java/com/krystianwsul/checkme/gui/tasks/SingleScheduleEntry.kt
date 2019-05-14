package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimePairPersist
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel


class SingleScheduleEntry(single: CreateTaskViewModel.ScheduleData.Single) : ScheduleEntry() {

    val mDate: Date
    val mTimePair: TimePair

    override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CreateTaskViewModel.CustomTimeData>, context: Context): String {
        return mDate.getDisplayText() + ", " + if (mTimePair.customTimeKey != null) {
            check(mTimePair.hourMinute == null)

            val customTimeData = customTimeDatas.getValue(mTimePair.customTimeKey)

            customTimeData.name + " (" + customTimeData.hourMinutes[mDate.dayOfWeek] + ")"
        } else {
            mTimePair.hourMinute!!.toString()
        }
    }

    override val scheduleData get() = CreateTaskViewModel.ScheduleData.Single(mDate, mTimePair)

    override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
        var monthDayNumber = mDate.day
        var beginningOfMonth = true
        if (monthDayNumber > 28) {
            monthDayNumber = Utils.getDaysInMonth(mDate.year, mDate.month) - monthDayNumber + 1
            beginningOfMonth = false
        }
        val monthWeekNumber = (monthDayNumber - 1) / 7 + 1

        return ScheduleDialogFragment.ScheduleDialogData(mDate, mutableSetOf(mDate.dayOfWeek), true, monthDayNumber, monthWeekNumber, mDate.dayOfWeek, beginningOfMonth, TimePairPersist(mTimePair), ScheduleType.SINGLE)
    }

    override val scheduleType = ScheduleType.SINGLE

    init {
        mDate = single.date
        mTimePair = single.timePair.copy()
    }
}
