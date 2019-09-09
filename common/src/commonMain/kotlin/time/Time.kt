package com.krystianwsul.common.time

interface Time {

    val timePair: TimePair

    fun getHourMinute(dayOfWeek: DayOfWeek): HourMinute

    override fun toString(): String
}
