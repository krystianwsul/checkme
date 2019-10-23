package com.krystianwsul.common.domain.schedules

interface WeeklyScheduleBridge : RepeatingScheduleBridge {

    val daysOfWeek: Set<Int>
}
