package com.krystianwsul.checkme.domain.schedules

interface WeeklyScheduleBridge : ScheduleBridge {

    val daysOfWeek: Set<Int>
}
