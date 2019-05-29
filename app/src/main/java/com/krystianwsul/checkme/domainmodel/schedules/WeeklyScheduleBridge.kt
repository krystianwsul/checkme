package com.krystianwsul.checkme.domainmodel.schedules

interface WeeklyScheduleBridge : ScheduleBridge {

    val daysOfWeek: Set<Int>
}
