package com.krystianwsul.checkme.utils.time

data class DateTime(val date: Date, val time: Time) : Comparable<DateTime> {

    val timeStamp get() = TimeStamp(date, time.getHourMinute(date.dayOfWeek))

    override fun compareTo(other: DateTime) = compareValuesBy(this, other, { it.date }, { time.getHourMinute(date.dayOfWeek) })

    override fun toString() = date.toString() + " " + time.toString()

    fun getDisplayText() = date.getDisplayText() + ", " + time.toString()
}
