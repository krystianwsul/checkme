package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.time.Date

interface WeeklyScheduleBridge : ScheduleBridge {

    val daysOfWeek: Set<Int>

    var from: Date?
    var until: Date?
}
