package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.loaders.CreateTaskLoader
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimePairPersist

import junit.framework.Assert

class MonthlyDayScheduleEntry : ScheduleEntry {

    companion object {

        val CREATOR: Parcelable.Creator<MonthlyDayScheduleEntry> = object : Parcelable.Creator<MonthlyDayScheduleEntry> {

            override fun createFromParcel(parcel: Parcel): MonthlyDayScheduleEntry {
                val monthDayNumber = parcel.readInt()

                val beginningOfMonth = parcel.readInt() == 1

                val timePair = parcel.readParcelable<TimePair>(TimePair::class.java.classLoader)
                Assert.assertTrue(timePair != null)

                val error = parcel.readString()

                return MonthlyDayScheduleEntry(monthDayNumber, beginningOfMonth, timePair!!, error)
            }

            override fun newArray(size: Int): Array<MonthlyDayScheduleEntry?> = arrayOfNulls(size)
        }
    }

    private val monthDayNumber: Int
    private val beginningOfMonth: Boolean

    private val timePair: TimePair

    override val scheduleData: CreateTaskLoader.ScheduleData
        get() = CreateTaskLoader.ScheduleData.MonthlyDayScheduleData(monthDayNumber, beginningOfMonth, timePair)

    override val scheduleType = ScheduleType.MONTHLY_DAY

    constructor(monthlyDayScheduleData: CreateTaskLoader.ScheduleData.MonthlyDayScheduleData) {
        monthDayNumber = monthlyDayScheduleData.dayOfMonth
        beginningOfMonth = monthlyDayScheduleData.beginningOfMonth
        timePair = monthlyDayScheduleData.timePair.copy()
    }

    private constructor(monthDayNumber: Int, beginningOfMonth: Boolean, timePair: TimePair, error: String?) : super(error) {
        this.monthDayNumber = monthDayNumber
        this.beginningOfMonth = beginningOfMonth
        this.timePair = timePair
    }

    constructor(scheduleDialogData: ScheduleDialogFragment.ScheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.MONTHLY_DAY)
        Assert.assertTrue(scheduleDialogData.mMonthlyDay)

        monthDayNumber = scheduleDialogData.mMonthDayNumber
        beginningOfMonth = scheduleDialogData.mBeginningOfMonth
        timePair = scheduleDialogData.mTimePairPersist.timePair
    }

    override fun getText(customTimeDatas: Map<CustomTimeKey, CreateTaskLoader.CustomTimeData>, context: Context): String {
        val day = Utils.ordinal(monthDayNumber) + " " + context.getString(R.string.monthDay) + " " + context.getString(R.string.monthDayStart) + " " + context.resources.getStringArray(R.array.month)[if (beginningOfMonth) 0 else 1] + " " + context.getString(R.string.monthDayEnd)

        return "$day, " + if (timePair.mCustomTimeKey != null) {
            Assert.assertTrue(timePair.mHourMinute == null)

            val customTimeData = customTimeDatas[timePair.mCustomTimeKey]!!

            customTimeData.name
        } else {
            timePair.mHourMinute!!.toString()
        }
    }

    override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.ScheduleHint?): ScheduleDialogFragment.ScheduleDialogData {
        var date = scheduleHint?.mDate ?: today

        date = Utils.getDateInMonth(date.year, date.month, monthDayNumber, beginningOfMonth)

        return ScheduleDialogFragment.ScheduleDialogData(date, mutableSetOf(date.dayOfWeek), true, monthDayNumber, (monthDayNumber - 1) / 7 + 1, date.dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.MONTHLY_DAY)
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(monthDayNumber)
        parcel.writeInt(if (beginningOfMonth) 1 else 0)
        parcel.writeParcelable(timePair, 0)
        parcel.writeString(error)
    }
}
