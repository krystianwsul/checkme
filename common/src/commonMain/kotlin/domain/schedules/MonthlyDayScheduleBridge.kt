package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey

interface MonthlyDayScheduleBridge<T : CustomTimeId, U : ProjectKey> : RepeatingScheduleBridge<T, U> {

    val dayOfMonth: Int

    val beginningOfMonth: Boolean
}
