package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.MonthlyWeekScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyWeekScheduleRecord
import com.krystianwsul.checkme.utils.ScheduleId
import com.krystianwsul.checkme.utils.TaskKey

class RemoteMonthlyWeekScheduleBridge(private val domainFactory: DomainFactory, private val remoteMonthlyWeekScheduleRecord: RemoteMonthlyWeekScheduleRecord) : MonthlyWeekScheduleBridge {

    override val startTime by lazy { remoteMonthlyWeekScheduleRecord.startTime }

    override val rootTaskKey by lazy { TaskKey(remoteMonthlyWeekScheduleRecord.projectId, remoteMonthlyWeekScheduleRecord.taskId) }

    override val dayOfMonth get() = remoteMonthlyWeekScheduleRecord.dayOfMonth

    override val dayOfWeek get() = remoteMonthlyWeekScheduleRecord.dayOfWeek

    override val beginningOfMonth get() = remoteMonthlyWeekScheduleRecord.beginningOfMonth

    override val customTimeKey = remoteMonthlyWeekScheduleRecord.customTimeId?.let {
        domainFactory.getCustomTimeKey(remoteMonthlyWeekScheduleRecord.projectId, it)
    }

    override val hour get() = remoteMonthlyWeekScheduleRecord.hour

    override val minute get() = remoteMonthlyWeekScheduleRecord.minute

    override val remoteCustomTimeKey = remoteMonthlyWeekScheduleRecord.customTimeId?.let {
        Pair(remoteMonthlyWeekScheduleRecord.projectId, it)
    }

    override var endTime
        get() = remoteMonthlyWeekScheduleRecord.endTime
        set(value) {
            remoteMonthlyWeekScheduleRecord.endTime = value
        }

    override fun delete() = remoteMonthlyWeekScheduleRecord.delete()

    override val scheduleId get() = ScheduleId.Remote(remoteMonthlyWeekScheduleRecord.projectId, remoteMonthlyWeekScheduleRecord.taskId, remoteMonthlyWeekScheduleRecord.id)
}
