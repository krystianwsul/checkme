package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

interface RepeatingScheduleBridge<T : RemoteCustomTimeId, U : ProjectKey> : ScheduleBridge<T, U> {

    val from: Date?
    val until: Date?
}