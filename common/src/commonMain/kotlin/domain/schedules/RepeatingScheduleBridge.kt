package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.time.Date

interface RepeatingScheduleBridge : ScheduleBridge {

    val from: Date?
    val until: Date?
}