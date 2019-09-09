package com.krystianwsul.checkme.domainmodel.schedules

import com.krystianwsul.checkme.utils.ScheduleId
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

interface ScheduleBridge {
    
    val startTime: Long

    var endTime: Long?

    val rootTaskKey: TaskKey

    val remoteCustomTimeKey: Pair<String, RemoteCustomTimeId>?

    fun delete()

    val customTimeKey: CustomTimeKey<*>?

    val hour: Int?

    val minute: Int?

    val scheduleId: ScheduleId

    val timePair
        get() = customTimeKey?.let { TimePair(it) } ?: TimePair(HourMinute(hour!!, minute!!))
}
