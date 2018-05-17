package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.krystianwsul.checkme.loaders.CreateTaskLoader
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimePairPersist
import junit.framework.Assert

class SingleScheduleEntry : ScheduleEntry {

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<SingleScheduleEntry> = object : Parcelable.Creator<SingleScheduleEntry> {

            override fun createFromParcel(parcel: Parcel) = parcel.run {
                val date = readParcelable<Date>(Date::class.java.classLoader)
                Assert.assertTrue(date != null)

                val timePair = readParcelable<TimePair>(TimePair::class.java.classLoader)
                Assert.assertTrue(timePair != null)

                val error = readString()

                SingleScheduleEntry(date!!, timePair!!, error)
            }

            override fun newArray(size: Int) = arrayOfNulls<SingleScheduleEntry>(size)
        }
    }

    val mDate: Date
    val mTimePair: TimePair

    constructor(singleScheduleData: CreateTaskLoader.ScheduleData.SingleScheduleData) {
        mDate = singleScheduleData.date
        mTimePair = singleScheduleData.timePair.copy()
    }

    constructor(scheduleHint: CreateTaskActivity.ScheduleHint?) {
        when {
            scheduleHint == null -> { // new for task
                val pair = HourMinute.nextHour

                mDate = pair.first
                mTimePair = TimePair(pair.second)
            }
            scheduleHint.mTimePair != null -> { // for instance group or instance join
                mDate = scheduleHint.mDate
                mTimePair = scheduleHint.mTimePair.copy()
            }
            else -> { // for group root
                val pair = HourMinute.getNextHour(scheduleHint.mDate)

                mDate = pair.first
                mTimePair = TimePair(pair.second)
            }
        }
    }

    private constructor(date: Date, timePair: TimePair, error: String?) : super(error) {
        mDate = date
        mTimePair = timePair
    }

    constructor(scheduleDialogData: ScheduleDialogFragment.ScheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE)

        mDate = scheduleDialogData.mDate
        mTimePair = scheduleDialogData.mTimePairPersist.timePair
    }

    override fun getText(customTimeDatas: Map<CustomTimeKey, CreateTaskLoader.CustomTimeData>, context: Context): String {
        return mDate.getDisplayText(context) + ", " + if (mTimePair.customTimeKey != null) {
            Assert.assertTrue(mTimePair.hourMinute == null)

            val customTimeData = customTimeDatas[mTimePair.customTimeKey]
            Assert.assertTrue(customTimeData != null)

            customTimeData!!.name + " (" + customTimeData.hourMinutes[mDate.dayOfWeek] + ")"
        } else {
            Assert.assertTrue(mTimePair.hourMinute != null)

            mTimePair.hourMinute!!.toString()
        }
    }

    override val scheduleData get() = CreateTaskLoader.ScheduleData.SingleScheduleData(mDate, mTimePair)

    override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.ScheduleHint?): ScheduleDialogFragment.ScheduleDialogData {
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

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.run {
            writeParcelable(mDate, 0)
            writeParcelable(mTimePair, 0)
            writeString(error)
        }
    }
}
