package com.krystianwsul.checkme.domainmodel

interface SingleScheduleBridge : ScheduleBridge {

    val year: Int

    val month: Int

    val day: Int
}
