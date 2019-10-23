package com.krystianwsul.common.domain.schedules

interface MonthlyDayScheduleBridge : RepeatingScheduleBridge {

    val dayOfMonth: Int

    val beginningOfMonth: Boolean
}
