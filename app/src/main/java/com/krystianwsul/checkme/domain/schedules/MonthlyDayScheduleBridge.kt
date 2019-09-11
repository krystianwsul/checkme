package com.krystianwsul.checkme.domain.schedules

interface MonthlyDayScheduleBridge : ScheduleBridge {

    val dayOfMonth: Int

    val beginningOfMonth: Boolean
}
