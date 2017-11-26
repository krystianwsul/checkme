package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.CustomTimeKey

interface WeeklyScheduleBridge : ScheduleBridge {

    val daysOfWeek: Set<Int>

    val customTimeKey: CustomTimeKey?

    val hour: Int?

    val minute: Int?
}
