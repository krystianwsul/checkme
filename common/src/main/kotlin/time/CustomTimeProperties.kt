package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeKey

interface CustomTimeProperties {

    val key: CustomTimeKey

    val name: String

    val hourMinutes: Map<DayOfWeek, HourMinute>

    fun getHourMinute(dayOfWeek: DayOfWeek): HourMinute
}