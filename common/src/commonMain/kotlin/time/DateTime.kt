package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.TimezoneOffset

data class DateTime(val date: Date, val time: Time) : Comparable<DateTime> {

    companion object {

        fun fromOffset(long: Long, offset: Double?): DateTime {
            val dateTimeTz = if (offset == null) {
                DateTimeSoy.fromUnix(long).local
            } else {
                val dateTimeSoy = DateTimeSoy.fromUnix(long)
                val dateTimeTz = DateTimeTz.utc(dateTimeSoy, TimezoneOffset(offset))

                DateTimeTz.local(dateTimeTz.local, dateTimeSoy.local.offset)
            }

            return DateTime(dateTimeTz)
        }
    }

    constructor(dateTimeTz: DateTimeTz) : this(Date(dateTimeTz), Time.Normal(HourMinute(dateTimeTz)))

    constructor(exactTimeStamp: ExactTimeStamp) : this(exactTimeStamp.toDateTimeTz())

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
