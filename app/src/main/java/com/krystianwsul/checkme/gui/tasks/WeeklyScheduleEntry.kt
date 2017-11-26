package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

import com.krystianwsul.checkme.loaders.CreateTaskLoader
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimePairPersist

import junit.framework.Assert

class WeeklyScheduleEntry : ScheduleEntry {

    companion object {

        val CREATOR: Parcelable.Creator<WeeklyScheduleEntry> = object : Parcelable.Creator<WeeklyScheduleEntry> {

            override fun createFromParcel(parcel: Parcel) = parcel.run {
                @Suppress("UNCHECKED_CAST")
                val daysOfWeek = readSerializable() as HashSet<DayOfWeek>
                val timePair = readParcelable<TimePair>(TimePair::class.java.classLoader)!!
                val error = readString()

                WeeklyScheduleEntry(daysOfWeek, timePair, error)
            }

            override fun newArray(size: Int) = arrayOfNulls<WeeklyScheduleEntry>(size)
        }
    }

    private val daysOfWeek: Set<DayOfWeek>
    private val timePair: TimePair

    constructor(weeklyScheduleData: CreateTaskLoader.ScheduleData.WeeklyScheduleData) {
        daysOfWeek = weeklyScheduleData.daysOfWeek
        timePair = weeklyScheduleData.timePair.copy()
    }

    private constructor(daysOfWeek: Set<DayOfWeek>, timePair: TimePair, error: String?) : super(error) {
        this.daysOfWeek = daysOfWeek
        this.timePair = timePair
    }

    constructor(scheduleDialogData: ScheduleDialogFragment.ScheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.WEEKLY)

        daysOfWeek = scheduleDialogData.mDaysOfWeek
        timePair = scheduleDialogData.mTimePairPersist.timePair
    }

    internal override fun getText(customTimeDatas: Map<CustomTimeKey, CreateTaskLoader.CustomTimeData>, context: Context): String {
        return daysOfWeek.toString() + ", " + if (timePair.mCustomTimeKey != null) {
            Assert.assertTrue(timePair.mHourMinute == null)

            customTimeDatas[timePair.mCustomTimeKey]!!.name
        } else {
            Assert.assertTrue(timePair.mHourMinute != null)

            timePair.mHourMinute!!.toString()
        }
    }

    internal override fun getScheduleData() = CreateTaskLoader.ScheduleData.WeeklyScheduleData(daysOfWeek, timePair)

    internal override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.ScheduleHint?): ScheduleDialogFragment.ScheduleDialogData {
        val date = scheduleHint?.mDate ?: today

        var monthDayNumber = date.day
        var beginningOfMonth = true
        if (monthDayNumber > 28) {
            monthDayNumber = Utils.getDaysInMonth(date.year, date.month) - monthDayNumber + 1
            beginningOfMonth = false
        }
        val monthWeekNumber = (monthDayNumber - 1) / 7 + 1

        return ScheduleDialogFragment.ScheduleDialogData(date, daysOfWeek.toMutableSet(), true, monthDayNumber, monthWeekNumber, date.dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.WEEKLY)
    }

    internal override fun getScheduleType() = ScheduleType.WEEKLY

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.run {
            writeSerializable(HashSet(daysOfWeek))
            writeParcelable(timePair, 0)
            writeString(mError)
        }
    }
}
