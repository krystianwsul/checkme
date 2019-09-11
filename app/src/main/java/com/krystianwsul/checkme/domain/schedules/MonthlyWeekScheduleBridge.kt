package com.krystianwsul.checkme.domain.schedules

interface MonthlyWeekScheduleBridge : ScheduleBridge {

    val dayOfMonth: Int

    val dayOfWeek: Int

    val beginningOfMonth: Boolean
}
