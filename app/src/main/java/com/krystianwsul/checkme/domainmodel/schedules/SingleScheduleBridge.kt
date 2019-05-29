package com.krystianwsul.checkme.domainmodel.schedules

interface SingleScheduleBridge : ScheduleBridge {

    val year: Int

    val month: Int

    val day: Int
}
