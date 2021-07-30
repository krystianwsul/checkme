package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.TimezoneOffset

sealed class ExactTimeStamp : Comparable<ExactTimeStamp> {

    abstract val long: Long
    abstract val offset: Double

    data class Local(override val long: Long) : ExactTimeStamp() {

        companion object {

            val now get() = Local(DateTimeSoy.nowUnixLong())
        }

        override val offset by lazy { toDateTimeSoy().localOffset.totalMilliseconds }

        override val date get() = Date(toDateTimeTzLocal())

        override val hourMilli get() = HourMilli(toDateTimeTzLocal())

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

        constructor(date: Date, hourMinute: HourMinute) : this(date, hourMinute.toHourMilli())

        private fun toDateTimeTzLocal() = toDateTimeSoy().local

        fun toTimeStamp() = TimeStamp.fromMillis(long)

        fun plusOne() = Local(long + 1)

        fun minusOne() = Local(long - 1)

        override fun toString() = "$date $hourMilli"

        private val offsetExactTimeStamp by lazy { Offset.fromOffset(long, null) }

        fun toOffset(offset: Double? = null) = offset?.let { Offset.fromOffset(long, offset) } ?: offsetExactTimeStamp

        fun toOffset(referenceOffset: Offset) = Offset.fromOffset(long, referenceOffset.offset)

        operator fun plus(timeSpan: TimeSpan) = Local(long + timeSpan.millisecondsLong)

        operator fun minus(timeSpan: TimeSpan) = Local(long - timeSpan.millisecondsLong)

        override fun details() = "Local(long = $long, offset = $offset)" + ", " + toString()
    }

    data class Offset(override val long: Long, override val offset: Double) : ExactTimeStamp() {

        companion object {

            fun fromOffset(long: Long, offset: Double?): Offset {
                val finalOffset = offset ?: Local(long).offset

                return Offset(long, finalOffset)
            }

            fun fromDateTime(dateTime: DateTime, offset: Double): Offset {
                val date = dateTime.date
                val hourMinute = dateTime.hourMinute

                /**
                 * This doesn't actually construct a valid DateTimeSoy.  It's just a hacky way to get the correct
                 * values into DateTimeTz.
                 */

                val dateTimeSoy = DateTimeSoy(date.year, date.month, date.day, hourMinute.hour, hourMinute.minute)
                val dateTimeTz = dateTimeSoy.toOffsetUnadjusted(TimezoneOffset(offset))

                return Offset(dateTimeTz)
            }

            fun compare(a: Offset, b: Offset) = compareValuesBy(a, b, { it.date }, { it.hourMilli })
        }

        override val date get() = Date(toDateTimeTz())

        override val hourMilli get() = HourMilli(toDateTimeTz())

        constructor(dateTimeTz: DateTimeTz) : this(dateTimeTz.utc.unixMillisLong, dateTimeTz.offset.totalMilliseconds)

        fun plusOne() = Offset(long + 1, offset)

        fun minusOne() = Offset(long - 1, offset)

        override fun toString() = "$date $hourMilli"

        override fun details() = "Offset(long = $long, offset = $offset), " + toString()
    }

    abstract val date: Date

    abstract val hourMilli: HourMilli

    fun toDateTimeSoy() = DateTimeSoy.fromUnix(long)

    fun toDateTimeTz() = toDateTimeSoy().toOffset(TimezoneOffset(offset))

    abstract fun details(): String

    override fun compareTo(other: ExactTimeStamp) = long.compareTo(other.long)
}
