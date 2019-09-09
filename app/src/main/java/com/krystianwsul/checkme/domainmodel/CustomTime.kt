package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import java.util.*

interface CustomTime : Time {

    var name: String

    val hourMinutes: TreeMap<DayOfWeek, HourMinute>

    val customTimeKey: CustomTimeKey<*>

    fun setHourMinute(dayOfWeek: DayOfWeek, hourMinute: HourMinute)
}
