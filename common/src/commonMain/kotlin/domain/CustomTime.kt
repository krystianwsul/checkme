package com.krystianwsul.common.domain

import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey

interface CustomTime : Time {

    var name: String

    val hourMinutes: Map<DayOfWeek, HourMinute>

    val customTimeKey: CustomTimeKey<*>

    fun setHourMinute(dayOfWeek: DayOfWeek, hourMinute: HourMinute)
}
