package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.TimezoneOffset

sealed class ExactTimeStamp2 {

    abstract val long: Long
    abstract val offset: Double

    data class Local(override val long: Long) : ExactTimeStamp2(), Comparable<Local> {

        companion object {

            val now get() = Local(DateTimeSoy.nowUnixLong())
        }

        override val offset by lazy { toDateTimeSoy().localOffset.totalMilliseconds }

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

        fun toTimeStamp() = TimeStamp.fromMillis(long)

        fun plusOne() = Local(long + 1)

        fun minusOne() = Local(long - 1)

        override fun toString() = "$date $hourMilli"

        override fun compareTo(other: Local) = long.compareTo(other.long)

        fun toOffset(offset: Double) = Offset.fromOffset(long, offset)

        fun toOffset(referenceOffset: Offset) = Offset.fromOffset(long, referenceOffset.offset)
    }

    data class Offset(override val long: Long, override val offset: Double) : ExactTimeStamp2(), Comparable<Offset> {

        companion object {

            fun fromOffset(long: Long, offset: Double?): Offset {
                val dateTimeSoy = DateTimeSoy.fromUnix(long)

                val timezoneOffset = offset?.let { TimezoneOffset(it) } ?: dateTimeSoy.localOffset

                val dateTimeTz = DateTimeTz.utc(dateTimeSoy, timezoneOffset)

                val dateTimeTzAdjusted = DateTimeTz.local(dateTimeTz.local, dateTimeSoy.local.offset)

                return Offset(dateTimeTzAdjusted.utc.unixMillisLong, timezoneOffset.totalMilliseconds)
            }
        }

        fun plusOne() = Local(long + 1)

        fun minusOne() = Local(long - 1)

        override fun toString() = "$date $hourMilli"

        override fun compareTo(other: Offset) = compareValuesBy(this, other, { it.date }, { it.hourMilli })
    }

    fun toDateTimeSoy() = DateTimeSoy.fromUnix(long)

    fun toDateTimeTz() = toDateTimeSoy().toOffset(TimezoneOffset(offset))

    private fun toDateTimeTzLocal() = toDateTimeSoy().local

    val date get() = Date(toDateTimeTzLocal())

    val hourMilli get() = HourMilli(toDateTimeTzLocal())

    override fun toString() = "$date $hourMilli"
}
