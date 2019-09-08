package com.krystianwsul.checkme.utils.time

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class TimeStamp(val long: Long) : Comparable<TimeStamp>, Parcelable {

    companion object {

        val now get() = TimeStamp(Calendar.getInstance())

        fun fromMillis(millis: Long) = TimeStamp(Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis)

        private fun calendarToMillis(calendar: Calendar): Long {
            val (year, month, day) = Date(calendar.toDateTimeTz()) // todo
            val (hour, minute) = HourMinute(calendar)

            return GregorianCalendar(year, month - 1, day, hour, minute).timeInMillis
        }
    }

    val calendar: Calendar
        get() = Calendar.getInstance().apply {
            timeInMillis = long
        }

    val date: Date get() = Date(calendar.toDateTimeTz()) // todo

    val hourMinute: HourMinute get() = HourMinute(calendar)

    constructor(calendar: Calendar) : this(calendarToMillis(calendar))

    constructor(date: Date, hourMinute: HourMinute) : this(GregorianCalendar(date.year, date.month - 1, date.day, hourMinute.hour, hourMinute.minute).timeInMillis)

    override fun compareTo(other: TimeStamp) = long.compareTo(other.long)

    override fun toString() = "$date $hourMinute"

    fun toExactTimeStamp() = ExactTimeStamp(long)

    fun toDateTimeSoy() = DateTimeSoy.fromUnix(long)

    fun toDateTimeTz() = toDateTimeSoy().local
}
