package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey

interface SingleScheduleBridge<T : CustomTimeId, U : ProjectKey> : ScheduleBridge<T, U> {

    val year: Int

    val month: Int

    val day: Int
}
