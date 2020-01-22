package com.krystianwsul.common.time

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.Serializable
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.hours
import com.soywiz.klock.parse

@Parcelize
data class HourMinute(val hour: Int, val minute: Int) : Comparable<HourMinute>, Parcelable, Serializable {

    companion object {

        private const val PATTERN = "HH:mm"
        private val format = DateFormat(PATTERN)

        val now get() = TimeStamp.now.hourMinute

        val nextHour get() = getNextHour(Date.today())

        fun getNextHour(date: Date, now: ExactTimeStamp = ExactTimeStamp.now) = now.toTimeStamp()
                .hourMinute
                .let { TimeStamp(date, HourMinute(it.hour, 0)) }
                .toDateTimeTz()
                .plus(1.hours)
                .let { Pair(Date(it), HourMinute(it)) }

        fun fromJson(json: String) = format.parse(json).let { HourMinute(it.hours, it.minutes) }
    }

    constructor(dateTimeTz: DateTimeTz) : this(dateTimeTz.hours, dateTimeTz.minutes)

    override fun compareTo(other: HourMinute) = compareValuesBy(this, other, { it.hour }, { it.minute })

    override fun toString() = toDateTimeTz().formatTime()

    fun toHourMilli() = HourMilli(hour, minute, 0, 0)

    fun toJson() = toDateTimeTz().format(format)

    fun toDateTimeTz() = TimeStamp(Date.today(), this).toDateTimeTz()
}
