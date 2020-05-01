package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.ProjectType

interface YearlyScheduleBridge<T : ProjectType> : RepeatingScheduleBridge<T> {

    val month: Int
    val day: Int
}
