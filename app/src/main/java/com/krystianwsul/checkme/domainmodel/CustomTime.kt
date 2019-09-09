package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import java.util.*

interface CustomTime : Time {

    var name: String

    val hourMinutes: TreeMap<DayOfWeek, HourMinute>

    val customTimeKey: CustomTimeKey<*>

    fun setHourMinute(dayOfWeek: DayOfWeek, hourMinute: HourMinute)
}
