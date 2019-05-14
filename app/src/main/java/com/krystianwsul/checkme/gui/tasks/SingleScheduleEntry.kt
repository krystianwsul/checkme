package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimePairPersist
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel


class SingleScheduleEntry : ScheduleEntry {

    val mDate: Date
    val mTimePair: TimePair

    constructor(single: CreateTaskViewModel.ScheduleData.Single) {
        mDate = single.date
        mTimePair = single.timePair.copy()
    }

    constructor(scheduleHint: CreateTaskActivity.Hint.Schedule?) {
        when {
            scheduleHint == null -> { // new for task
                val pair = HourMinute.nextHour

                mDate = pair.first
                mTimePair = TimePair(pair.second)
            }
            scheduleHint.timePair != null -> { // for instance group or instance join
                mDate = scheduleHint.date
                mTimePair = scheduleHint.timePair.copy()
            }
            else -> { // for group root
                val pair = HourMinute.getNextHour(scheduleHint.date)

                mDate = pair.first
                mTimePair = TimePair(pair.second)
            }
        }
    }

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
}
