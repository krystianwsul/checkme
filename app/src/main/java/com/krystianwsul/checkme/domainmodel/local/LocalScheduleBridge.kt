package com.krystianwsul.checkme.domainmodel.local

import android.support.v4.util.Pair

import com.krystianwsul.checkme.domainmodel.ScheduleBridge
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord
import com.krystianwsul.checkme.utils.TaskKey

abstract class LocalScheduleBridge(val scheduleRecord: ScheduleRecord) : ScheduleBridge {

    override val startTime by lazy { scheduleRecord.startTime }

    override val rootTaskKey get() = TaskKey(scheduleRecord.rootTaskId)

    override val remoteCustomTimeKey: Pair<String, String>? = null

    override fun getEndTime() = scheduleRecord.endTime

    override fun setEndTime(endTime: Long) {
        scheduleRecord.endTime = endTime
    }
}
