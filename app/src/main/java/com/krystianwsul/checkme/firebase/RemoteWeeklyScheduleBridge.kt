package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.WeeklyScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteWeeklyScheduleRecord
import com.krystianwsul.checkme.utils.ScheduleId
import com.krystianwsul.checkme.utils.TaskKey

class RemoteWeeklyScheduleBridge(
        domainFactory: DomainFactory,
        private val remoteWeeklyScheduleRecord: RemoteWeeklyScheduleRecord) : RemoteScheduleBridge(domainFactory, remoteWeeklyScheduleRecord), WeeklyScheduleBridge {

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
