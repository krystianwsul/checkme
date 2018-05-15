package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.Time
import java.util.*

interface CustomTime : Time {

    val name: String

    val hourMinutes: TreeMap<DayOfWeek, HourMinute>

    val customTimeKey: CustomTimeKey
}
