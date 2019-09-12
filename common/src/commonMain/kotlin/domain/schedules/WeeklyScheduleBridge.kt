package com.krystianwsul.common.domain.schedules

interface WeeklyScheduleBridge : ScheduleBridge {

    val daysOfWeek: Set<Int>
}
