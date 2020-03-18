package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

interface WeeklyScheduleBridge<T : RemoteCustomTimeId, U : ProjectKey> : RepeatingScheduleBridge<T, U> {

    val daysOfWeek: Set<Int>
}
