package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

interface SingleScheduleBridge<T : RemoteCustomTimeId, U : ProjectKey> : ScheduleBridge<T, U> {

    val year: Int

    val month: Int

    val day: Int
}
