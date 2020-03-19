package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey

interface WeeklyScheduleBridge<T : CustomTimeId, U : ProjectKey> : RepeatingScheduleBridge<T, U> {

    val daysOfWeek: Set<Int>
}
