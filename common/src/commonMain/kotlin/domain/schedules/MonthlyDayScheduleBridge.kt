package com.krystianwsul.common.domain.schedules

interface MonthlyDayScheduleBridge : ScheduleBridge {

    val dayOfMonth: Int

    val beginningOfMonth: Boolean
}
