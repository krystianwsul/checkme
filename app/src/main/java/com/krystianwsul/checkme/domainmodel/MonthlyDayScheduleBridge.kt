package com.krystianwsul.checkme.domainmodel

interface MonthlyDayScheduleBridge : ScheduleBridge {

    val dayOfMonth: Int

    val beginningOfMonth: Boolean
}
