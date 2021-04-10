package com.krystianwsul.common.time

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.Serializable
import com.soywiz.klock.*

@Parcelize
data class HourMinute(val hour: Int, val minute: Int) : Comparable<HourMinute>, Parcelable, Serializable {

    companion object {

        private const val PATTERN = "HH:mm"
        private val dateFormat = DateFormat(PATTERN)
        private val timeFormat = TimeFormat(PATTERN)

        private val hourMinuteRegex = Regex("^\\d\\d:\\d\\d$")

        val now get() = TimeStamp.now.hourMinute

        val nextHour get() = getNextHour(Date.today())

        fun getNextHour(date: Date, now: ExactTimeStamp.Local = ExactTimeStamp.Local.now) = now.toTimeStamp()
                .hourMinute
                .let { TimeStamp(date, HourMinute(it.hour, 0)) }
                .toDateTimeTz()
                .plus(1.hours)
                .let { Pair(Date(it), HourMinute(it)) }

        private fun fromJson(json: String) = dateFormat.parse(json).let { HourMinute(it.hours, it.minutes) }

        fun tryFromJson(json: String): HourMinute? {
            if (hourMinuteRegex.find(json) == null) return null

            return fromJson(json)
        }
    }

    constructor(dateTimeTz: DateTimeTz) : this(dateTimeTz.hours, dateTimeTz.minutes)

    override fun compareTo(other: HourMinute) = compareValuesBy(this, other, { it.hour }, { it.minute })

    override fun toString() = toTimeSoy().formatTime()

    fun toHourMilli() = HourMilli(hour, minute, 0, 0)

    fun toJson() = toTimeSoy().format(timeFormat)

    private fun toTimeSoy() = TimeSoy(hour, minute)
}
