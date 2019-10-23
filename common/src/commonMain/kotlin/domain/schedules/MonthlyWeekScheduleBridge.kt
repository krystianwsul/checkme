package com.krystianwsul.common.domain.schedules

interface MonthlyWeekScheduleBridge : RepeatingScheduleBridge {

    val dayOfMonth: Int

    val dayOfWeek: Int

    val beginningOfMonth: Boolean
}
