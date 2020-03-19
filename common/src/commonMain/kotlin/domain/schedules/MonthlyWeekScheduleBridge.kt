package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey

interface MonthlyWeekScheduleBridge<T : CustomTimeId, U : ProjectKey> : RepeatingScheduleBridge<T, U> {

    val dayOfMonth: Int

    val dayOfWeek: Int

    val beginningOfMonth: Boolean
}
