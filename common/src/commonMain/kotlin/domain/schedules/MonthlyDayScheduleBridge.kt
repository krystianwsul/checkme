package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.ProjectType

interface MonthlyDayScheduleBridge<T : ProjectType> : RepeatingScheduleBridge<T> {

    val dayOfMonth: Int

    val beginningOfMonth: Boolean
}
