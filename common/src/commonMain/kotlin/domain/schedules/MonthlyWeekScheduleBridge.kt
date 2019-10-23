package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.time.Date

interface MonthlyWeekScheduleBridge : ScheduleBridge {

    val dayOfMonth: Int

    val dayOfWeek: Int

    val beginningOfMonth: Boolean

    var from: Date?
    var until: Date?
}
