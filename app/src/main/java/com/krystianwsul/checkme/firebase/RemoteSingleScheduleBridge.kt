package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.schedules.SingleScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteSingleScheduleRecord
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.ScheduleId
import com.krystianwsul.checkme.utils.TaskKey

class RemoteSingleScheduleBridge<T : RemoteCustomTimeId>(
        domainFactory: DomainFactory,
        private val remoteSingleScheduleRecord: RemoteSingleScheduleRecord<T>) : RemoteScheduleBridge<T>(domainFactory, remoteSingleScheduleRecord), SingleScheduleBridge {

    override val year get() = remoteSingleScheduleRecord.year

    override val month get() = remoteSingleScheduleRecord.month

    override val day get() = remoteSingleScheduleRecord.day

    override val hour get() = remoteSingleScheduleRecord.hour

    override val minute get() = remoteSingleScheduleRecord.minute

    override val startTime by lazy { remoteSingleScheduleRecord.startTime }

    override val rootTaskKey by lazy { TaskKey(remoteSingleScheduleRecord.projectId, remoteSingleScheduleRecord.taskId) }

    override val remoteCustomTimeKey = remoteSingleScheduleRecord.customTimeId?.let {
        Pair(remoteSingleScheduleRecord.projectId, it)
    }

    override var endTime
        get() = remoteSingleScheduleRecord.endTime
        set(value) {
            remoteSingleScheduleRecord.endTime = value
        }

    override fun delete() = remoteSingleScheduleRecord.delete()

    override val scheduleId get() = ScheduleId.Remote(remoteSingleScheduleRecord.projectId, remoteSingleScheduleRecord.taskId, remoteSingleScheduleRecord.id)
}
