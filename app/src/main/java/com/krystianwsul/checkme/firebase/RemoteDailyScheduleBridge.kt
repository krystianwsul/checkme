package com.krystianwsul.checkme.firebase

import android.support.v4.util.Pair
import com.krystianwsul.checkme.domainmodel.DailyScheduleBridge
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.records.RemoteDailyScheduleRecord
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey

internal class RemoteDailyScheduleBridge(private val mDomainFactory: DomainFactory, private val mRemoteDailyScheduleRecord: RemoteDailyScheduleRecord) : DailyScheduleBridge {

    override fun getStartTime() = mRemoteDailyScheduleRecord.startTime

    override fun getEndTime() = mRemoteDailyScheduleRecord.endTime

    override fun setEndTime(endTime: Long) {
        mRemoteDailyScheduleRecord.setEndTime(endTime)
    }

    override fun getRootTaskKey() = TaskKey(mRemoteDailyScheduleRecord.projectId, mRemoteDailyScheduleRecord.taskId)

    override fun getScheduleType() = ScheduleType.DAILY

    override fun getCustomTimeKey() = mRemoteDailyScheduleRecord.customTimeId?.let { mDomainFactory.getCustomTimeKey(mRemoteDailyScheduleRecord.projectId, it) }

    override fun getHour() = mRemoteDailyScheduleRecord.hour

    override fun getMinute() = mRemoteDailyScheduleRecord.minute

    override fun delete() {
        mRemoteDailyScheduleRecord.delete()
    }

    override fun getRemoteCustomTimeKey() = mRemoteDailyScheduleRecord.customTimeId?.let { Pair.create(mRemoteDailyScheduleRecord.projectId, it) }
}
