package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.CustomTimeKey

interface SingleScheduleBridge : ScheduleBridge {

    val year: Int

    val month: Int

    val day: Int

    val customTimeKey: CustomTimeKey?

    val hour: Int?

    val minute: Int?
}
