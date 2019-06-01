package com.krystianwsul.checkme.domainmodel.schedules

interface MonthlyWeekScheduleBridge : ScheduleBridge {

    val dayOfMonth: Int

    val dayOfWeek: Int

    val beginningOfMonth: Boolean
}
