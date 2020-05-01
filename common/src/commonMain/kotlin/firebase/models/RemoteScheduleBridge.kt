package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.ScheduleBridge
import com.krystianwsul.common.firebase.records.ScheduleRecord
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey


abstract class RemoteScheduleBridge<T : ProjectType>(
        private val scheduleRecord: ScheduleRecord<T>
) : ScheduleBridge<T> {

    final override val startTime get() = scheduleRecord.startTime

    final override var endTime: Long?
        get() = scheduleRecord.endTime
        set(value) {
            scheduleRecord.endTime = value
        }

    final override val hour get() = scheduleRecord.hour

    final override val minute get() = scheduleRecord.minute

    final override val customTimeKey get() = scheduleRecord.customTimeKey

    final override val rootTaskKey get() = TaskKey(scheduleRecord.projectId, scheduleRecord.taskId)

    final override val scheduleId get() = ScheduleId(scheduleRecord.projectId, scheduleRecord.taskId, scheduleRecord.id)

    final override fun delete() = scheduleRecord.delete()
}