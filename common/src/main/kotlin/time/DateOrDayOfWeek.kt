package com.krystianwsul.common.time

sealed class DateOrDayOfWeek {

    data class Date(val date: com.krystianwsul.common.time.Date) : DateOrDayOfWeek()

    data class DayOfWeek(val dayOfWeek: com.krystianwsul.common.time.DayOfWeek) : DateOrDayOfWeek()
}