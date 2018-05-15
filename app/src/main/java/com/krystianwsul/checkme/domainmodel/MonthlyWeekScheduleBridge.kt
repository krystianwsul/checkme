package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.CustomTimeKey

interface MonthlyWeekScheduleBridge : ScheduleBridge {

    val dayOfMonth: Int

    val dayOfWeek: Int

    val beginningOfMonth: Boolean

    val customTimeKey: CustomTimeKey?

    val hour: Int?

    val minute: Int?
}
