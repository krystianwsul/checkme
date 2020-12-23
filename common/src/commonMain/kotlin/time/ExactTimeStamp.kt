package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz
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

        private fun toDateTimeTzLocal() = toDateTimeSoy().local

        fun toTimeStamp() = TimeStamp.fromMillis(long)

        fun plusOne() = Local(long + 1)

        fun minusOne() = Local(long - 1)

        override fun toString() = "$date $hourMilli"

        override fun compareTo(other: ExactTimeStamp) = when (other) {
            is Local -> long.compareTo(other.long)
            is Offset -> Offset.compare(toOffset(other), other)
        }

        private val offsetExactTimeStamp by lazy { Offset.fromOffset(long, null) }

        fun toOffset(offset: Double? = null) = offset?.let { Offset.fromOffset(long, offset) } ?: offsetExactTimeStamp

        fun toOffset(referenceOffset: Offset) = Offset.fromOffset(long, referenceOffset.offset)

        override fun details() = super.toString() + ", " + toString()
    }

    data class Offset(
            override val long: Long,
            override val offset: Double,
    ) : ExactTimeStamp() {

        companion object {

            fun fromOffset(long: Long, offset: Double?): Offset {
                val dateTimeSoy = DateTimeSoy.fromUnix(long)

                val timezoneOffset = offset?.let { TimezoneOffset(it) } ?: dateTimeSoy.localOffset

                val dateTimeTz = DateTimeTz.utc(dateTimeSoy, timezoneOffset)

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

        override fun compareTo(other: ExactTimeStamp): Int {
            val otherOffset = when (other) {
                is Local -> other.toOffset(this)
                is Offset -> other
            }

            return compare(this, otherOffset)
        }

        override fun details() = super.toString() + ", " + toString()
    }

    abstract val date: Date

    abstract val hourMilli: HourMilli

    fun toDateTimeSoy() = DateTimeSoy.fromUnix(long)

    fun toDateTimeTz() = toDateTimeSoy().toOffset(TimezoneOffset(offset))

    abstract fun details(): String
}
