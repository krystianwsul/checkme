package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.ScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteScheduleRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey


abstract class RemoteScheduleBridge<T : CustomTimeId, U : ProjectKey>(
        private val remoteScheduleRecord: RemoteScheduleRecord<T, U>
) : ScheduleBridge<T, U> {

    final override val startTime get() = remoteScheduleRecord.startTime

    final override var endTime: Long?
        get() = remoteScheduleRecord.endTime
        set(value) {
            remoteScheduleRecord.endTime = value
        }

    final override val hour get() = remoteScheduleRecord.hour

    final override val minute get() = remoteScheduleRecord.minute

    final override val customTimeKey get() = remoteScheduleRecord.customTimeKey

    final override val rootTaskKey get() = TaskKey(remoteScheduleRecord.projectId, remoteScheduleRecord.taskId)

    final override val scheduleId get() = ScheduleId(remoteScheduleRecord.projectId, remoteScheduleRecord.taskId, remoteScheduleRecord.id)

    final override fun delete() = remoteScheduleRecord.delete()
}