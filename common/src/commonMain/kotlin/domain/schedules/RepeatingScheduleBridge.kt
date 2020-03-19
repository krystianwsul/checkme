package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType

interface RepeatingScheduleBridge<T : ProjectType> : ScheduleBridge<T> {

    val from: Date?
    val until: Date?
}