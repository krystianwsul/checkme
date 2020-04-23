package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.ProjectType

interface WeeklyScheduleBridge<T : ProjectType> : RepeatingScheduleBridge<T> {

    val daysOfWeek: Set<Int>
}
