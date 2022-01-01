package com.krystianwsul.common.time

sealed class DateOrDayOfWeek {

    abstract val date: com.krystianwsul.common.time.Date?
    abstract val dayOfWeek: com.krystianwsul.common.time.DayOfWeek

    data class Date(override val date: com.krystianwsul.common.time.Date) : DateOrDayOfWeek() {

        override val dayOfWeek get() = date.dayOfWeek
    }

    data class DayOfWeek(override val dayOfWeek: com.krystianwsul.common.time.DayOfWeek) : DateOrDayOfWeek() {

        override val date: com.krystianwsul.common.time.Date? get() = null
    }
}