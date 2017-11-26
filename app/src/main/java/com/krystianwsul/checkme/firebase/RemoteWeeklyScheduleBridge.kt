package com.krystianwsul.checkme.firebase

import android.support.v4.util.Pair
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.WeeklyScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteWeeklyScheduleRecord
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey

internal class RemoteWeeklyScheduleBridge(private val mDomainFactory: DomainFactory, private val mRemoteWeeklyScheduleRecord: RemoteWeeklyScheduleRecord) : WeeklyScheduleBridge {

    override val daysOfWeek get() = setOf(mRemoteWeeklyScheduleRecord.dayOfWeek)

    override val customTimeKey get() = mRemoteWeeklyScheduleRecord.run { customTimeId?.let { mDomainFactory.getCustomTimeKey(projectId, it) } }

    override val hour get() = mRemoteWeeklyScheduleRecord.hour

    override val minute get() = mRemoteWeeklyScheduleRecord.minute

    override fun getStartTime() = mRemoteWeeklyScheduleRecord.startTime

    override fun getEndTime() = mRemoteWeeklyScheduleRecord.endTime

    override fun setEndTime(endTime: Long) {
        mRemoteWeeklyScheduleRecord.setEndTime(endTime)
    }

    override fun getRootTaskKey() = mRemoteWeeklyScheduleRecord.run { TaskKey(projectId, taskId) }

    override fun getScheduleType() = ScheduleType.WEEKLY

    override fun delete() {
        mRemoteWeeklyScheduleRecord.delete()
    }

    override fun getRemoteCustomTimeKey() = mRemoteWeeklyScheduleRecord.run { customTimeId?.let { Pair.create(projectId, it) } }
}
