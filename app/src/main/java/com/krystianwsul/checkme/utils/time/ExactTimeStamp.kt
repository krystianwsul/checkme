package com.krystianwsul.checkme.utils.time

import java.util.*

class ExactTimeStamp(val long: Long) : Comparable<ExactTimeStamp> {

    companion object {

        val now get() = ExactTimeStamp(Calendar.getInstance())
    }

    val calendar
        get() = Calendar.getInstance().apply {
            timeInMillis = long
        }!!

    val date get() = Date(calendar)

    val hourMilli get() = HourMilli(calendar)

    constructor(date: Date, hourMilli: HourMilli) : this(Calendar.getInstance().run {
        set(Calendar.YEAR, date.year)
        set(Calendar.MONTH, date.month - 1)
        set(Calendar.DAY_OF_MONTH, date.day)
        set(Calendar.HOUR_OF_DAY, hourMilli.hour)
        set(Calendar.MINUTE, hourMilli.minute)
        set(Calendar.SECOND, hourMilli.second)
        set(Calendar.MILLISECOND, hourMilli.milli)
        timeInMillis
    })

    constructor(calendar: Calendar) : this(calendar.timeInMillis)

    override fun compareTo(other: ExactTimeStamp) = long.compareTo(other.long)

    override fun hashCode() = long.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        if (other !is ExactTimeStamp)
            return false

        if (other === this)
            return true

        return long == other.long
    }

    fun plusOne() = ExactTimeStamp(long + 1)

    fun minusOne() = ExactTimeStamp(long - 1)

    fun toTimeStamp() = TimeStamp.fromMillis(long)

    override fun toString() = date.toString() + " " + hourMilli.toString()
}
