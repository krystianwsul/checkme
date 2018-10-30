package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.SingleScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteSingleScheduleRecord
import com.krystianwsul.checkme.utils.TaskKey

class RemoteSingleScheduleBridge(private val kotlinDomainFactory: KotlinDomainFactory, private val remoteSingleScheduleRecord: RemoteSingleScheduleRecord) : SingleScheduleBridge {

    override val year get() = remoteSingleScheduleRecord.year

    override val month get() = remoteSingleScheduleRecord.month

    override val day get() = remoteSingleScheduleRecord.day

    override val customTimeKey = remoteSingleScheduleRecord.customTimeId?.let {
        kotlinDomainFactory.getCustomTimeKey(remoteSingleScheduleRecord.projectId, it)
    }

    override val hour get() = remoteSingleScheduleRecord.hour

    override val minute get() = remoteSingleScheduleRecord.minute

    override val startTime by lazy { remoteSingleScheduleRecord.startTime }

    override val rootTaskKey by lazy { TaskKey(remoteSingleScheduleRecord.projectId, remoteSingleScheduleRecord.taskId) }

    override val remoteCustomTimeKey = remoteSingleScheduleRecord.customTimeId?.let {
        Pair(remoteSingleScheduleRecord.projectId, it)
    }

    override fun getEndTime() = remoteSingleScheduleRecord.endTime

    override fun setEndTime(endTime: Long) = remoteSingleScheduleRecord.setEndTime(endTime)

    override fun delete() = remoteSingleScheduleRecord.delete()
}
