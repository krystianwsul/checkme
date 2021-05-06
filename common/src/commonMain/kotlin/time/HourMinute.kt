package com.krystianwsul.common.time

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.Serializable
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.TimeFormat
import com.soywiz.klock.hours

@Parcelize
data class HourMinute(val hour: Int, val minute: Int) : Comparable<HourMinute>, Parcelable, Serializable {

    companion object {

        private const val PATTERN = "HH:mm"
        private val timeFormat = TimeFormat(PATTERN)

        private val hourMinuteRegex = Regex("^(\\d\\d):(\\d\\d)$")

        val now get() = TimeStamp.now.hourMinute

        val nextHour get() = getNextHour(Date.today())

        fun getNextHour(date: Date, now: ExactTimeStamp.Local = ExactTimeStamp.Local.now) = now.toTimeStamp()
            .hourMinute
            .let { TimeStamp(date, HourMinute(it.hour, 0)) }
            .toDateTimeTz()
            .plus(1.hours)
            .let { Pair(Date(it), HourMinute(it)) }

        fun tryFromJson(json: String) = hourMinuteRegex.find(json)?.let { matchResult ->
            val (hour, minute) = (1..2).map { matchResult.groupValues[it].toInt() }
            HourMinute(hour, minute)
        }
    }

    constructor(dateTimeTz: DateTimeTz) : this(dateTimeTz.hours, dateTimeTz.minutes)

    override fun compareTo(other: HourMinute) = compareValuesBy(this, other, { it.hour }, { it.minute })

    override fun toString() = toTimeSoy().formatTime()

    fun toHourMilli() = HourMilli(hour, minute, 0, 0)

    fun toJson() = toTimeSoy().format(timeFormat)

    private fun toTimeSoy() = TimeSoy(hour, minute)
}
