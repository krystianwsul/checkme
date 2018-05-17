package com.krystianwsul.checkme.utils.time

data class NormalTime(val hourMinute: HourMinute) : Time {

    override val timePair by lazy { TimePair(null, hourMinute) }

    constructor(hour: Int, minute: Int) : this(HourMinute(hour, minute))

    override fun getHourMinute(dayOfWeek: DayOfWeek) = hourMinute

    override fun toString() = hourMinute.toString()
}
