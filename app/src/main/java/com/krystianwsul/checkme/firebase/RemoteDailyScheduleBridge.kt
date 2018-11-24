package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.WeeklyScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteDailyScheduleRecord
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.DayOfWeek

class RemoteDailyScheduleBridge(private val domainFactory: DomainFactory, private val mRemoteDailyScheduleRecord: RemoteDailyScheduleRecord) : WeeklyScheduleBridge {

    override val startTime by lazy { mRemoteDailyScheduleRecord.startTime }

    override fun getEndTime() = mRemoteDailyScheduleRecord.endTime

    override fun setEndTime(endTime: Long) = mRemoteDailyScheduleRecord.setEndTime(endTime)

    override val rootTaskKey get() = TaskKey(mRemoteDailyScheduleRecord.projectId, mRemoteDailyScheduleRecord.taskId)

    override val customTimeKey get() = mRemoteDailyScheduleRecord.customTimeId?.let { domainFactory.getCustomTimeKey(mRemoteDailyScheduleRecord.projectId, it) }

    override val hour get() = mRemoteDailyScheduleRecord.hour

    override val minute get() = mRemoteDailyScheduleRecord.minute

    override fun delete() = mRemoteDailyScheduleRecord.delete()

    override val remoteCustomTimeKey get() = mRemoteDailyScheduleRecord.customTimeId?.let { Pair(mRemoteDailyScheduleRecord.projectId, it) }

    override val daysOfWeek = DayOfWeek.values()
            .map { it.ordinal }
            .toSet()
}
