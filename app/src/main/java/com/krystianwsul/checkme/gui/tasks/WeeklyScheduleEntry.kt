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

internal class WeeklyScheduleEntry : ScheduleEntry {

    companion object {

        val CREATOR: Parcelable.Creator<WeeklyScheduleEntry> = object : Parcelable.Creator<WeeklyScheduleEntry> {

            override fun createFromParcel(parcel: Parcel) = parcel.run {
                val dayOfWeek = readSerializable() as DayOfWeek
                val timePair = readParcelable<TimePair>(TimePair::class.java.classLoader)!!
                val error = readString()

                WeeklyScheduleEntry(dayOfWeek, timePair, error)
            }

            override fun newArray(size: Int) = arrayOfNulls<WeeklyScheduleEntry>(size)
        }
    }

    private val mDayOfWeek: DayOfWeek

    private val mTimePair: TimePair

    constructor(weeklyScheduleData: CreateTaskLoader.WeeklyScheduleData) {
        mDayOfWeek = weeklyScheduleData.DayOfWeek
        mTimePair = weeklyScheduleData.TimePair.copy()
    }

    private constructor(dayOfWeek: DayOfWeek, timePair: TimePair, error: String?) : super(error) {
        mDayOfWeek = dayOfWeek
        mTimePair = timePair
    }

    constructor(scheduleDialogData: ScheduleDialogFragment.ScheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.WEEKLY)

        mDayOfWeek = scheduleDialogData.mDayOfWeek
        mTimePair = scheduleDialogData.mTimePairPersist.timePair
    }

    internal override fun getText(customTimeDatas: Map<CustomTimeKey, CreateTaskLoader.CustomTimeData>, context: Context): String {
        return mDayOfWeek.toString() + ", " + if (mTimePair.mCustomTimeKey != null) {
            Assert.assertTrue(mTimePair.mHourMinute == null)

            customTimeDatas[mTimePair.mCustomTimeKey]!!.let { it.Name + " (" + it.HourMinutes[mDayOfWeek].toString() + ")" }
        } else {
            Assert.assertTrue(mTimePair.mHourMinute != null)

            mTimePair.mHourMinute!!.toString()
        }
    }

    internal override fun getScheduleData() = CreateTaskLoader.WeeklyScheduleData(mDayOfWeek, mTimePair)

    internal override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.ScheduleHint?): ScheduleDialogFragment.ScheduleDialogData {
        val date = scheduleHint?.mDate ?: today

        var monthDayNumber = date.day
        var beginningOfMonth = true
        if (monthDayNumber > 28) {
            monthDayNumber = Utils.getDaysInMonth(date.year, date.month) - monthDayNumber + 1
            beginningOfMonth = false
        }
        val monthWeekNumber = (monthDayNumber - 1) / 7 + 1

        return ScheduleDialogFragment.ScheduleDialogData(date, mDayOfWeek, true, monthDayNumber, monthWeekNumber, date.dayOfWeek, beginningOfMonth, TimePairPersist(mTimePair), ScheduleType.WEEKLY)
    }

    internal override fun getScheduleType() = ScheduleType.WEEKLY

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.run {
            writeSerializable(mDayOfWeek)
            writeParcelable(mTimePair, 0)
            writeString(mError)
        }
    }
}
