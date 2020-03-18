package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.WeeklyScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteDailyScheduleRecord
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey

class RemoteDailyScheduleBridge<T : RemoteCustomTimeId>(
        remoteProjectRecord: RemoteProjectRecord<T, *>,
        private val remoteDailyScheduleRecord: RemoteDailyScheduleRecord<T, *>
) : RemoteScheduleBridge<T>(remoteProjectRecord, remoteDailyScheduleRecord), WeeklyScheduleBridge {

    override val startTime by lazy { remoteDailyScheduleRecord.startTime }

    override var endTime
        get() = remoteDailyScheduleRecord.endTime
        set(value) {
            remoteDailyScheduleRecord.endTime = value
        }

    override val rootTaskKey get() = TaskKey(remoteDailyScheduleRecord.projectId, remoteDailyScheduleRecord.taskId)

    override val hour get() = remoteDailyScheduleRecord.hour

    override val minute get() = remoteDailyScheduleRecord.minute

    override fun delete() = remoteDailyScheduleRecord.delete()

    override val remoteCustomTimeKey get() = remoteDailyScheduleRecord.customTimeId?.let { Pair(remoteDailyScheduleRecord.projectId, it) }

    override val daysOfWeek = DayOfWeek.values()
            .map { it.ordinal }
            .toSet()

    override val scheduleId get() = ScheduleId(remoteDailyScheduleRecord.projectId, remoteDailyScheduleRecord.taskId, remoteDailyScheduleRecord.id)

    override var from: Date? = null
    override var until: Date? = null
}
