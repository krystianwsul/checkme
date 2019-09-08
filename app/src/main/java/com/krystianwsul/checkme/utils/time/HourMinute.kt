package com.krystianwsul.checkme.utils.time

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import java.io.Serializable
import java.util.*

@Parcelize
data class HourMinute(val hour: Int, val minute: Int) : Comparable<HourMinute>, Parcelable, Serializable {

    companion object {

        private const val PATTERN = "HH:mm"
        private val format = DateTimeFormat.forPattern(PATTERN)

        val now get() = TimeStamp.now.hourMinute

        val nextHour get() = getNextHour(Date.today(), ExactTimeStamp.now)

        fun getNextHour(date: Date) = getNextHour(date, ExactTimeStamp.now)

        fun getNextHour(date: Date, now: ExactTimeStamp): Pair<Date, HourMinute> {
            val hourMinute = now.toTimeStamp().hourMinute

            val calendar = date.calendar
            calendar.set(Calendar.HOUR_OF_DAY, hourMinute.hour)
            calendar.set(Calendar.MINUTE, hourMinute.minute)

            calendar.add(Calendar.HOUR_OF_DAY, 1)
            calendar.set(Calendar.MINUTE, 0)

            return Pair(Date(calendar.toDateTimeTz()), HourMinute(calendar)) // todo
        }

        fun fromJson(json: String) = format.parseLocalTime(json).let { HourMinute(it.hourOfDay, it.minuteOfHour) }
    }

    constructor(calendar: Calendar) : this(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

    override fun compareTo(other: HourMinute) = compareValuesBy(this, other, { it.hour }, { it.minute })

    override fun toString() = DateTimeFormat.forStyle("-S").print(LocalTime(hour, minute))!!

    fun toHourMilli() = HourMilli(hour, minute, 0, 0)

    fun toJson() = LocalTime(hour, minute).toString(PATTERN)!!
}
