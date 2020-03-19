package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.ProjectType

interface SingleScheduleBridge<T : ProjectType> : ScheduleBridge<T> {

    val year: Int

    val month: Int

    val day: Int
}
