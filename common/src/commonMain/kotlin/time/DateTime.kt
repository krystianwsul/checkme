package com.krystianwsul.common.time

data class DateTime(val date: Date, val time: Time) : Comparable<DateTime> {

    private val hourMinute get() = time.getHourMinute(date.dayOfWeek)

    val timeStamp get() = TimeStamp(date, hourMinute)

    override fun compareTo(other: DateTime) = compareValuesBy(
            this,
            other,
            { it.date },
            { hourMinute }
    )

    override fun toString() = "$date $time"

    fun toExactTimeStamp() = timeStamp.toExactTimeStamp()
}
