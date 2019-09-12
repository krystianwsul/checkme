package com.krystianwsul.common.domain.schedules

interface SingleScheduleBridge : ScheduleBridge {

    val year: Int

    val month: Int

    val day: Int
}
