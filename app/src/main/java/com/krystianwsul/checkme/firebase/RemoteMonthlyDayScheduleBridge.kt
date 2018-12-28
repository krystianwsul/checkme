package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.MonthlyDayScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyDayScheduleRecord
import com.krystianwsul.checkme.utils.ScheduleId
import com.krystianwsul.checkme.utils.TaskKey

class RemoteMonthlyDayScheduleBridge(private val domainFactory: DomainFactory, private val remoteMonthlyDayScheduleRecord: RemoteMonthlyDayScheduleRecord) : MonthlyDayScheduleBridge {

    override val startTime by lazy { remoteMonthlyDayScheduleRecord.startTime }

    override val rootTaskKey by lazy { TaskKey(remoteMonthlyDayScheduleRecord.projectId, remoteMonthlyDayScheduleRecord.taskId) }

    override val dayOfMonth get() = remoteMonthlyDayScheduleRecord.dayOfMonth

    override val beginningOfMonth get() = remoteMonthlyDayScheduleRecord.beginningOfMonth

    override val customTimeKey = remoteMonthlyDayScheduleRecord.customTimeId?.let {
        domainFactory.getCustomTimeKey(remoteMonthlyDayScheduleRecord.projectId, it)
    }

    override val hour get() = remoteMonthlyDayScheduleRecord.hour

    override val minute get() = remoteMonthlyDayScheduleRecord.minute

    override val remoteCustomTimeKey = remoteMonthlyDayScheduleRecord.customTimeId?.let {
        Pair(remoteMonthlyDayScheduleRecord.projectId, it)
    }

    override var endTime: Long?
        get() = remoteMonthlyDayScheduleRecord.endTime
        set(value) {
            remoteMonthlyDayScheduleRecord.endTime = value
        }

    override fun delete() = remoteMonthlyDayScheduleRecord.delete()

    override val scheduleId get() = ScheduleId.Remote(remoteMonthlyDayScheduleRecord.projectId, remoteMonthlyDayScheduleRecord.taskId, remoteMonthlyDayScheduleRecord.id)
}
