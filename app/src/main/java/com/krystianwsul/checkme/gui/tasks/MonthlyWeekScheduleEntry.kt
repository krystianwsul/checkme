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
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimePairPersist

import junit.framework.Assert

class MonthlyWeekScheduleEntry : ScheduleEntry {

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<MonthlyWeekScheduleEntry> = object : Parcelable.Creator<MonthlyWeekScheduleEntry> {

            override fun createFromParcel(parcel: Parcel): MonthlyWeekScheduleEntry {
                val monthWeekNumber = parcel.readInt()

                val monthWeekDay = parcel.readSerializable() as DayOfWeek

                val beginningOfMonth = parcel.readInt() == 1

                val timePair = parcel.readParcelable<TimePair>(TimePair::class.java.classLoader)!!

                val error = parcel.readString()

                return MonthlyWeekScheduleEntry(monthWeekNumber, monthWeekDay, beginningOfMonth, timePair, error)
            }

            override fun newArray(size: Int): Array<MonthlyWeekScheduleEntry?> = arrayOfNulls(size)
        }
    }

    private val monthWeekNumber: Int

    private val monthWeekDay: DayOfWeek

    private val beginningOfMonth: Boolean

    private val timePair: TimePair

    override val scheduleData: CreateTaskLoader.ScheduleData
        get() = CreateTaskLoader.ScheduleData.MonthlyWeekScheduleData(monthWeekNumber, monthWeekDay, beginningOfMonth, timePair)

    override val scheduleType = ScheduleType.MONTHLY_WEEK

    constructor(monthlyWeekScheduleData: CreateTaskLoader.ScheduleData.MonthlyWeekScheduleData) {
        monthWeekNumber = monthlyWeekScheduleData.dayOfMonth
        monthWeekDay = monthlyWeekScheduleData.dayOfWeek
        beginningOfMonth = monthlyWeekScheduleData.beginningOfMonth
        timePair = monthlyWeekScheduleData.TimePair.copy()
    }

    private constructor(monthWeekNumber: Int, monthWeekDay: DayOfWeek, beginningOfMonth: Boolean, timePair: TimePair, error: String?) : super(error) {
        this.monthWeekNumber = monthWeekNumber
        this.monthWeekDay = monthWeekDay
        this.beginningOfMonth = beginningOfMonth
        this.timePair = timePair
    }

    constructor(scheduleDialogData: ScheduleDialogFragment.ScheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.scheduleType == ScheduleType.MONTHLY_WEEK)
        Assert.assertTrue(!scheduleDialogData.monthlyDay)

        monthWeekNumber = scheduleDialogData.monthWeekNumber
        monthWeekDay = scheduleDialogData.monthWeekDay
        beginningOfMonth = scheduleDialogData.beginningOfMonth
        timePair = scheduleDialogData.timePairPersist.timePair
    }

    override fun getText(customTimeDatas: Map<CustomTimeKey, CreateTaskLoader.CustomTimeData>, context: Context): String {
        val day = Utils.ordinal(monthWeekNumber) + " " + monthWeekDay + " " + context.getString(R.string.monthDayStart) + " " + context.resources.getStringArray(R.array.month)[if (beginningOfMonth) 0 else 1] + " " + context.getString(R.string.monthDayEnd)

        return "$day, " + if (timePair.customTimeKey != null) {
            Assert.assertTrue(timePair.hourMinute == null)

            val customTimeData = customTimeDatas[timePair.customTimeKey]!!

            customTimeData.name
        } else {
            timePair.hourMinute!!.toString()
        }
    }

    override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.ScheduleHint?): ScheduleDialogFragment.ScheduleDialogData {
        var date = scheduleHint?.mDate ?: today

        date = Utils.getDateInMonth(date.year, date.month, monthWeekNumber, monthWeekDay, beginningOfMonth)

        return ScheduleDialogFragment.ScheduleDialogData(date, mutableSetOf(monthWeekDay), false, date.day, monthWeekNumber, monthWeekDay, beginningOfMonth, TimePairPersist(timePair), ScheduleType.MONTHLY_WEEK)
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(monthWeekNumber)
        parcel.writeSerializable(monthWeekDay)
        parcel.writeInt(if (beginningOfMonth) 1 else 0)
        parcel.writeParcelable(timePair, 0)
        parcel.writeString(error)
    }
}
