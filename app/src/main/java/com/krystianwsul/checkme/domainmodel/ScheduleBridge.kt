package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.ScheduleId
import com.krystianwsul.checkme.utils.TaskKey

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
}
