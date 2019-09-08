package com.krystianwsul.checkme.utils.time

import com.soywiz.klock.DateTimeTz

enum class DayOfWeek {
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY;

    companion object {

        fun fromDate(date: Date): DayOfWeek {
            val day = DateTimeTz.fromUnixLocal(TimeStamp(date, HourMinute.now).long).dayOfWeekInt

            return values()[day]
        }
    }

    override fun toString() = com.soywiz.klock.DayOfWeek[ordinal].localName
}
