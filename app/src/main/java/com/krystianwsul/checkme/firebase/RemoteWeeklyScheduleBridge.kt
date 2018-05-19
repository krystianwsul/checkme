package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.WeeklyScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteWeeklyScheduleRecord
import com.krystianwsul.checkme.utils.TaskKey

class RemoteWeeklyScheduleBridge(private val mDomainFactory: DomainFactory, private val mRemoteWeeklyScheduleRecord: RemoteWeeklyScheduleRecord) : WeeklyScheduleBridge {

    override val daysOfWeek get() = setOf(mRemoteWeeklyScheduleRecord.dayOfWeek)

    override val customTimeKey get() = mRemoteWeeklyScheduleRecord.run { customTimeId?.let { mDomainFactory.getCustomTimeKey(projectId, it) } }

    override val hour get() = mRemoteWeeklyScheduleRecord.hour

    override val minute get() = mRemoteWeeklyScheduleRecord.minute

    override val startTime by lazy { mRemoteWeeklyScheduleRecord.startTime }

    override fun getEndTime() = mRemoteWeeklyScheduleRecord.endTime

    override fun setEndTime(endTime: Long) {
        mRemoteWeeklyScheduleRecord.setEndTime(endTime)
    }

    override val rootTaskKey get() = mRemoteWeeklyScheduleRecord.run { TaskKey(projectId, taskId) }

    override fun delete() {
        mRemoteWeeklyScheduleRecord.delete()
    }

    override val remoteCustomTimeKey get() = mRemoteWeeklyScheduleRecord.run { customTimeId?.let { Pair(projectId, it) } }
}
