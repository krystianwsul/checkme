package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.time.Date

interface MonthlyDayScheduleBridge : ScheduleBridge {

    val dayOfMonth: Int

    val beginningOfMonth: Boolean

    var from: Date?
    var until: Date?
}
