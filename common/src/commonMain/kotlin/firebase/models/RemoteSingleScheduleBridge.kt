package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.SingleScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.firebase.records.RemoteSingleScheduleRecord
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey

class RemoteSingleScheduleBridge<T : RemoteCustomTimeId>(
        remoteProjectRecord: RemoteProjectRecord<T, *>,
        private val remoteSingleScheduleRecord: RemoteSingleScheduleRecord<T>
) : RemoteScheduleBridge<T>(remoteProjectRecord, remoteSingleScheduleRecord), SingleScheduleBridge {

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
