package com.krystianwsul.checkme.firebase.models

import com.krystianwsul.checkme.domain.schedules.WeeklyScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.firebase.records.RemoteWeeklyScheduleRecord
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey

class RemoteWeeklyScheduleBridge<T : RemoteCustomTimeId>(
        remoteProjectRecord: RemoteProjectRecord<T>,
        private val remoteWeeklyScheduleRecord: RemoteWeeklyScheduleRecord<T>
) : RemoteScheduleBridge<T>(remoteProjectRecord, remoteWeeklyScheduleRecord), WeeklyScheduleBridge {

    override val daysOfWeek get() = setOf(remoteWeeklyScheduleRecord.dayOfWeek)

    override val hour get() = remoteWeeklyScheduleRecord.hour

    override val minute get() = remoteWeeklyScheduleRecord.minute

    override val startTime by lazy { remoteWeeklyScheduleRecord.startTime }

    override var endTime
        get() = remoteWeeklyScheduleRecord.endTime
        set(value) {
            remoteWeeklyScheduleRecord.endTime = value
        }

    override val rootTaskKey get() = remoteWeeklyScheduleRecord.run { TaskKey(projectId, taskId) }

    override fun delete() = remoteWeeklyScheduleRecord.delete()

    override val remoteCustomTimeKey get() = remoteWeeklyScheduleRecord.run { customTimeId?.let { Pair(projectId, it) } }

    override val scheduleId get() = ScheduleId.Remote(remoteWeeklyScheduleRecord.projectId, remoteWeeklyScheduleRecord.taskId, remoteWeeklyScheduleRecord.id)
}
