package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.ProjectType

interface MonthlyWeekScheduleBridge<T : ProjectType> : RepeatingScheduleBridge<T> {

    val dayOfMonth: Int

    val dayOfWeek: Int

    val beginningOfMonth: Boolean
}
