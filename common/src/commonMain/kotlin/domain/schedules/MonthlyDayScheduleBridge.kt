package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

interface MonthlyDayScheduleBridge<T : RemoteCustomTimeId, U : ProjectKey> : RepeatingScheduleBridge<T, U> {

    val dayOfMonth: Int

    val beginningOfMonth: Boolean
}
