package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz

data class DateTime(val date: Date, val time: Time) : Comparable<DateTime> { // todo customtime ref

    constructor(dateTimeTz: DateTimeTz) : this(Date(dateTimeTz), Time.Normal(HourMinute(dateTimeTz)))

    constructor(exactTimeStamp: ExactTimeStamp) : this(exactTimeStamp.toDateTimeTz())

    constructor(date: Date, hourMinute: HourMinute) : this(date, Time.Normal(hourMinute))

    val hourMinute get() = time.getHourMinute(date.dayOfWeek)

    val timeStamp get() = TimeStamp(date, hourMinute)

    override fun compareTo(other: DateTime) = compareValuesBy(
            this,
            other,
            { it.date },
            { it.hourMinute }
    )

    override fun toString() = "$date $time"

    fun toLocalExactTimeStamp() = timeStamp.toLocalExactTimeStamp()

    fun toDateTimeSoy() = timeStamp.toDateTimeSoy()

    fun toDateTimePair() = DateTimePair(date, time.timePair)
}
