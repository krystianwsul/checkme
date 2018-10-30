package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.MonthlyDayScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyDayScheduleRecord
import com.krystianwsul.checkme.utils.TaskKey

class RemoteMonthlyDayScheduleBridge(private val kotlinDomainFactory: KotlinDomainFactory, private val remoteMonthlyDayScheduleRecord: RemoteMonthlyDayScheduleRecord) : MonthlyDayScheduleBridge {

    override val startTime by lazy { remoteMonthlyDayScheduleRecord.startTime }

    override val rootTaskKey by lazy { TaskKey(remoteMonthlyDayScheduleRecord.projectId, remoteMonthlyDayScheduleRecord.taskId) }

    override val dayOfMonth get() = remoteMonthlyDayScheduleRecord.dayOfMonth

    override val beginningOfMonth get() = remoteMonthlyDayScheduleRecord.beginningOfMonth

    override val customTimeKey = remoteMonthlyDayScheduleRecord.customTimeId?.let {
        kotlinDomainFactory.getCustomTimeKey(remoteMonthlyDayScheduleRecord.projectId, it)
    }

    override val hour get() = remoteMonthlyDayScheduleRecord.hour

    override val minute get() = remoteMonthlyDayScheduleRecord.minute

    override val remoteCustomTimeKey = remoteMonthlyDayScheduleRecord.customTimeId?.let {
        Pair(remoteMonthlyDayScheduleRecord.projectId, it)
    }

    override fun getEndTime() = remoteMonthlyDayScheduleRecord.endTime

    override fun setEndTime(endTime: Long) = remoteMonthlyDayScheduleRecord.setEndTime(endTime)

    override fun delete() = remoteMonthlyDayScheduleRecord.delete()
}
