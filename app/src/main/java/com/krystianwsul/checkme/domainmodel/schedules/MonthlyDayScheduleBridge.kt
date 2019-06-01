package com.krystianwsul.checkme.domainmodel.schedules

interface MonthlyDayScheduleBridge : ScheduleBridge {

    val dayOfMonth: Int

    val beginningOfMonth: Boolean
}
