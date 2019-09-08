package com.krystianwsul.checkme.utils.time

enum class DayOfWeek {
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY;

    companion object {

        fun fromDate(date: Date) = values()[date.toDateTimeTz().dayOfWeekInt]
    }

    override fun toString() = com.soywiz.klock.DayOfWeek[ordinal].localName
}
