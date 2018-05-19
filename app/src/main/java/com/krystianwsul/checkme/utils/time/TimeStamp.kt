package com.krystianwsul.checkme.utils.time

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class TimeStamp private constructor(val long: Long) : Comparable<TimeStamp>, Parcelable {

    companion object {

        val now get() = TimeStamp(Calendar.getInstance())

        @JvmField
        val CREATOR: Parcelable.Creator<TimeStamp> = object : Parcelable.Creator<TimeStamp> {

            override fun createFromParcel(source: Parcel): TimeStamp {
                val time = source.readLong()
                return TimeStamp(time)
            }

            override fun newArray(size: Int): Array<TimeStamp?> = arrayOfNulls(size)
        }

        fun fromMillis(millis: Long): TimeStamp {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = millis
            calendar.set(Calendar.MILLISECOND, 0)
            return TimeStamp(calendar.timeInMillis)
        }

        private fun calendarToMillis(calendar: Calendar): Long {
            val (year, month, day) = Date(calendar)
            val (hour, minute) = HourMinute(calendar)

            return GregorianCalendar(year, month - 1, day, hour, minute).timeInMillis
        }
    }

    val calendar
        get() = Calendar.getInstance().apply {
            timeInMillis = long
        }!!

    val date: Date get() = Date(calendar)

    val hourMinute: HourMinute get() = HourMinute(calendar)

    constructor(calendar: Calendar) : this(calendarToMillis(calendar))

    constructor(date: Date, hourMinute: HourMinute) : this(GregorianCalendar(date.year, date.month - 1, date.day, hourMinute.hour, hourMinute.minute).timeInMillis)

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
