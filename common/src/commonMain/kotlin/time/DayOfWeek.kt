package com.krystianwsul.common.time

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

        val set by lazy { values().toSet() }
    }

    private val localName by lazy { com.soywiz.klock.DayOfWeek[ordinal].localName.capitalize() }

    override fun toString() = localName
}
