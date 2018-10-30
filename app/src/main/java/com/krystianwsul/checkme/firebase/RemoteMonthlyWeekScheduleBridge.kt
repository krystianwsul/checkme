package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.MonthlyWeekScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyWeekScheduleRecord
import com.krystianwsul.checkme.utils.TaskKey

class RemoteMonthlyWeekScheduleBridge(private val kotlinDomainFactory: KotlinDomainFactory, private val remoteMonthlyWeekScheduleRecord: RemoteMonthlyWeekScheduleRecord) : MonthlyWeekScheduleBridge {

    override val startTime by lazy { remoteMonthlyWeekScheduleRecord.startTime }

    override val rootTaskKey by lazy { TaskKey(remoteMonthlyWeekScheduleRecord.projectId, remoteMonthlyWeekScheduleRecord.taskId) }

    override val dayOfMonth get() = remoteMonthlyWeekScheduleRecord.dayOfMonth

    override val dayOfWeek get() = remoteMonthlyWeekScheduleRecord.dayOfWeek

    override val beginningOfMonth get() = remoteMonthlyWeekScheduleRecord.beginningOfMonth

    override val customTimeKey = remoteMonthlyWeekScheduleRecord.customTimeId?.let {
        kotlinDomainFactory.getCustomTimeKey(remoteMonthlyWeekScheduleRecord.projectId, it)
    }

    override val hour get() = remoteMonthlyWeekScheduleRecord.hour

    override val minute get() = remoteMonthlyWeekScheduleRecord.minute

    override val remoteCustomTimeKey = remoteMonthlyWeekScheduleRecord.customTimeId?.let {
        Pair(remoteMonthlyWeekScheduleRecord.projectId, it)
    }

    override fun getEndTime() = remoteMonthlyWeekScheduleRecord.endTime

    override fun setEndTime(endTime: Long) = remoteMonthlyWeekScheduleRecord.setEndTime(endTime)

    override fun delete() = remoteMonthlyWeekScheduleRecord.delete()
}
