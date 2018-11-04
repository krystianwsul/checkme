package com.krystianwsul.checkme.domainmodel

interface WeeklyScheduleBridge : ScheduleBridge {

    val daysOfWeek: Set<Int>
}
