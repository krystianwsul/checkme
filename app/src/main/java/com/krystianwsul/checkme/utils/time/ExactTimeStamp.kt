package com.krystianwsul.checkme.utils.time

import org.joda.time.DateTime
import java.util.*

data class ExactTimeStamp(val long: Long) : Comparable<ExactTimeStamp> {

    companion object {

        val now get() = ExactTimeStamp(Calendar.getInstance())
    }

    val calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = long }

    val date get() = Date(calendar)

    val hourMilli get() = HourMilli(calendar)

    constructor(date: Date, hourMilli: HourMilli) : this(Calendar.getInstance().run {
        set(Calendar.YEAR, date.year)
        set(Calendar.MONTH, date.month - 1)
        set(Calendar.DAY_OF_MONTH, date.day)
        set(Calendar.HOUR_OF_DAY, hourMilli.hour)
        set(Calendar.MINUTE, hourMilli.minute)
        set(Calendar.SECOND, hourMilli.second)
        set(Calendar.MILLISECOND, hourMilli.milli)
        timeInMillis
    })

    constructor(calendar: Calendar) : this(calendar.timeInMillis)

    constructor(dateTime: DateTime) : this(dateTime.millis)

    override fun compareTo(other: ExactTimeStamp) = long.compareTo(other.long)

    fun plusOne() = ExactTimeStamp(long + 1)

    fun minusOne() = ExactTimeStamp(long - 1)

    fun toTimeStamp() = TimeStamp.fromMillis(long)

    override fun toString() = "$date $hourMilli"
}
