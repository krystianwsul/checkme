package com.krystianwsul.checkme.utils.time

import org.joda.time.DateTime
import java.util.*

data class ExactTimeStamp(val long: Long) : Comparable<ExactTimeStamp> {

    companion object {

        val now get() = ExactTimeStamp(DateTimeSoy.nowUnixLong())
    }

    fun toDateTimeSoy() = DateTimeSoy.fromUnix(long)

    val calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = long }

    val date get() = Date(toDateTimeSoy().local)

    val hourMilli get() = HourMilli(calendar)

    constructor(date: Date, hourMilli: HourMilli) : this(DateTimeSoy.createAdjusted(
            date.year,
            date.month,
            date.day,
            hourMilli.hour,
            hourMilli.minute,
            hourMilli.second,
            hourMilli.milli
    ).unixMillisLong)

    constructor(dateTime: DateTime) : this(dateTime.millis)

    override fun compareTo(other: ExactTimeStamp) = long.compareTo(other.long)

    fun plusOne() = ExactTimeStamp(long + 1)

    fun minusOne() = ExactTimeStamp(long - 1)

    fun toTimeStamp() = TimeStamp.fromMillis(long)

    override fun toString() = "$date $hourMilli"
}
