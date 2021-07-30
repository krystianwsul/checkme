package com.krystianwsul.common.time

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds

@Parcelize
data class TimeStamp(val long: Long) : Comparable<TimeStamp>, Parcelable {

    companion object {

        val now get() = TimeStamp(DateTimeSoy.now())

        fun fromMillis(millis: Long) = TimeStamp(DateTimeSoy.fromUnix(millis))
    }

    val date: Date get() = Date(toDateTimeTz())

    val hourMinute: HourMinute get() = HourMinute(toDateTimeTz())

    constructor(dateTimeSoy: DateTimeSoy) : this(
            dateTimeSoy.run {
                minus(seconds.seconds).minus(milliseconds.milliseconds)
            }.unixMillisLong
    )

    constructor(date: Date, hourMinute: HourMinute) : this(
            DateTimeSoy(
                    date.year,
                    date.month,
                    date.day,
                    hourMinute.hour,
                    hourMinute.minute
            ).let { it - it.localOffset.time }
    )

    override fun compareTo(other: TimeStamp) = long.compareTo(other.long)

    override fun toString() = "$date $hourMinute"

    fun toLocalExactTimeStamp() = ExactTimeStamp.Local(long)

    fun toDateTimeSoy() = DateTimeSoy.fromUnix(long)

    fun toDateTimeTz() = toDateTimeSoy().local
}
