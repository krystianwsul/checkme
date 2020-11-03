package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.TimezoneOffset

data class ExactTimeStamp(val long: Long) : Comparable<ExactTimeStamp> {

    companion object {

        val now get() = ExactTimeStamp(DateTimeSoy.nowUnixLong())

        fun fromOffset(long: Long, offset: Double?): ExactTimeStamp {
            return if (offset == null) {
                ExactTimeStamp(long)
            } else {
                val dateTimeSoy = DateTimeSoy(long)
                val dateTimeTz = DateTimeTz.utc(dateTimeSoy, TimezoneOffset(offset))

                val dateTimeTzAdjusted = DateTimeTz.local(dateTimeTz.local, dateTimeSoy.local.offset)

                ExactTimeStamp(dateTimeTzAdjusted.utc)
            }
        }
    }

    fun toDateTimeSoy() = DateTimeSoy.fromUnix(long)

    fun toDateTimeTz() = toDateTimeSoy().local

    val offset by lazy { toDateTimeTz().offset.totalMilliseconds }

    val date get() = Date(toDateTimeTz())

    val hourMilli get() = HourMilli(toDateTimeTz())

    constructor(dateTimeSoy: DateTimeSoy) : this(dateTimeSoy.unixMillisLong)

    constructor(date: Date, hourMilli: HourMilli) : this(DateTimeSoy(
            date.year,
            date.month,
            date.day,
            hourMilli.hour,
            hourMilli.minute,
            hourMilli.second,
            hourMilli.milli
    ).let { it - it.localOffset.time })

    override fun compareTo(other: ExactTimeStamp) = long.compareTo(other.long)

    fun plusOne() = ExactTimeStamp(long + 1)

    fun minusOne() = ExactTimeStamp(long - 1)

    fun toTimeStamp() = TimeStamp.fromMillis(long)

    override fun toString() = "$date $hourMilli"

    fun toDateTime() = DateTime(this)
}
