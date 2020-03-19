package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey

interface RepeatingScheduleBridge<T : CustomTimeId, U : ProjectKey> : ScheduleBridge<T, U> {

    val from: Date?
    val until: Date?
}