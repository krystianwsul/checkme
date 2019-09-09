package com.krystianwsul.common.time

data class DateTime(val date: Date, val time: Time) : Comparable<DateTime> {

    val timeStamp by lazy {
        TimeStamp(date, time.getHourMinute(date.dayOfWeek))
    }

    override fun compareTo(other: DateTime) = compareValuesBy(this, other, { it.date }, { time.getHourMinute(date.dayOfWeek) })

    override fun toString() = "$date $time"

    fun toExactTimeStamp() = timeStamp.toExactTimeStamp()
}
