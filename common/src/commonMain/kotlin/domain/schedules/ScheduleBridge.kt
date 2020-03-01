package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.*

interface ScheduleBridge {

    val startTime: Long

    var endTime: Long?

    val rootTaskKey: TaskKey

    val remoteCustomTimeKey: Pair<ProjectKey, RemoteCustomTimeId>?

    fun delete()

    val customTimeKey: CustomTimeKey<*, *>?

    val hour: Int?

    val minute: Int?

    val scheduleId: ScheduleId

    val timePair
        get() = customTimeKey?.let { TimePair(it) } ?: TimePair(HourMinute(hour!!, minute!!))
}
