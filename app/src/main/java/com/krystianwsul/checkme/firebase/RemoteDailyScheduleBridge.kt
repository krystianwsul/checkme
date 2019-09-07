package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.schedules.WeeklyScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteDailyScheduleRecord

import com.krystianwsul.checkme.utils.ScheduleId
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteDailyScheduleBridge<T : RemoteCustomTimeId>(
        domainFactory: DomainFactory,
        private val remoteDailyScheduleRecord: RemoteDailyScheduleRecord<T>) : RemoteScheduleBridge<T>(domainFactory, remoteDailyScheduleRecord), WeeklyScheduleBridge {

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

    override val scheduleId get() = ScheduleId.Remote(remoteDailyScheduleRecord.projectId, remoteDailyScheduleRecord.taskId, remoteDailyScheduleRecord.id)
}
