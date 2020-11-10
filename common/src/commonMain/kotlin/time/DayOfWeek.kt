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

        private val dateCache = mutableMapOf<Date, DayOfWeek>() // because I can't hold this on a parcelable date

        fun fromDate(date: Date): DayOfWeek {
            if (!dateCache.containsKey(date)) dateCache[date] = values()[date.toDateSoy().dayOfWeekInt]

            return dateCache.getValue(date)
        }

        val set by lazy { values().toSet() }
    }

    private val localName by lazy { com.soywiz.klock.DayOfWeek[ordinal].localName.capitalize() }

    override fun toString() = localName
}
