package com.krystianwsul.checkme.utils.time

interface Time {

    val timePair: TimePair

    fun getHourMinute(dayOfWeek: DayOfWeek): HourMinute

    override fun toString(): String
}
