package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

interface MonthlyWeekScheduleBridge<T : RemoteCustomTimeId, U : ProjectKey> : RepeatingScheduleBridge<T, U> {

    val dayOfMonth: Int

    val dayOfWeek: Int

    val beginningOfMonth: Boolean
}
