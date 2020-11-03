package com.krystianwsul.common.time

data class ExactTimeStamp(val long: Long) : Comparable<ExactTimeStamp> {

    companion object {

        val now get() = ExactTimeStamp(DateTimeSoy.nowUnixLong())
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

    fun plusOne() = ExactTimeStamp(long + 1) // todo dst

    fun minusOne() = ExactTimeStamp(long - 1) // todo dst

    fun toTimeStamp() = TimeStamp.fromMillis(long)

    override fun toString() = "$date $hourMilli"

    fun toDateTime() = DateTime(this)
}
