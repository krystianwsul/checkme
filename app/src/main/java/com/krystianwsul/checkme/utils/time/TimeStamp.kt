package com.krystianwsul.checkme.utils.time

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class TimeStamp : Comparable<TimeStamp>, Parcelable {

    companion object {

        val now get() = TimeStamp(Calendar.getInstance())

        val CREATOR: Parcelable.Creator<TimeStamp> = object : Parcelable.Creator<TimeStamp> {
            override fun createFromParcel(source: Parcel): TimeStamp {
                val time = source.readLong()
                return TimeStamp(time)
            }

            override fun newArray(size: Int): Array<TimeStamp?> = arrayOfNulls(size)
        }
    }

    val long: Long

    val calendar
        get() = Calendar.getInstance().apply {
            timeInMillis = long
        }!!

    val date: Date get() = Date(calendar)

    val hourMinute: HourMinute get() = HourMinute(calendar)

    constructor(date: Date, hourMinute: HourMinute) {
        long = GregorianCalendar(date.year, date.month - 1, date.day, hourMinute.hour, hourMinute.minute).timeInMillis
    }

    constructor(calendar: Calendar) {
        val (year, month, day) = Date(calendar)
        val (hour, minute) = HourMinute(calendar)

        long = GregorianCalendar(year, month - 1, day, hour, minute).timeInMillis
    }

    constructor(millis: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.MILLISECOND, 0)
        long = calendar.timeInMillis
    }

    override fun compareTo(other: TimeStamp) = long.compareTo(other.long)

    override fun toString() = date.toString() + " " + hourMinute.toString()

    override fun hashCode() = long.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        if (other !is TimeStamp)
            return false

        if (other === this)
            return true

        return long == other.long
    }

    fun toExactTimeStamp() = ExactTimeStamp(long)

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = dest.writeLong(long)
}
