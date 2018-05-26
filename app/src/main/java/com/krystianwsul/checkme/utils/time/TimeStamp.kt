package com.krystianwsul.checkme.utils.time

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class TimeStamp internal constructor(val long: Long) : Comparable<TimeStamp>, Parcelable {

    companion object {

        val now get() = TimeStamp(Calendar.getInstance())

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
}
