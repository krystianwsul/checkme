package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.ScheduleBridge
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.ScheduleId
import com.krystianwsul.checkme.utils.TaskKey

abstract class LocalScheduleBridge(val scheduleRecord: ScheduleRecord) : ScheduleBridge {

    override val startTime by lazy { scheduleRecord.startTime }

    override val rootTaskKey get() = TaskKey(scheduleRecord.rootTaskId)

    override val remoteCustomTimeKey: Pair<String, RemoteCustomTimeId>? = null

    override var endTime
        get() = scheduleRecord.endTime
        set(value) {
            scheduleRecord.endTime = value
        }

    override val scheduleId by lazy { ScheduleId.Local(scheduleRecord.id) }
}
