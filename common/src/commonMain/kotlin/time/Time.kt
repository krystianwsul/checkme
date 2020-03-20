package com.krystianwsul.common.time

interface Time { // todo sealed

    val timePair: TimePair

    fun getHourMinute(dayOfWeek: DayOfWeek): HourMinute

    override fun toString(): String
}
