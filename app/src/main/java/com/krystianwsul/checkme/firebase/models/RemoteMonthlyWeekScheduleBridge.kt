package com.krystianwsul.checkme.firebase.models

import com.krystianwsul.checkme.domainmodel.schedules.MonthlyWeekScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteMonthlyWeekScheduleRecord
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey

class RemoteMonthlyWeekScheduleBridge<T : RemoteCustomTimeId>(
        remoteProjectRecord: RemoteProjectRecord<T>,
        private val remoteMonthlyWeekScheduleRecord: RemoteMonthlyWeekScheduleRecord<T>
) : RemoteScheduleBridge<T>(remoteProjectRecord, remoteMonthlyWeekScheduleRecord), MonthlyWeekScheduleBridge {

    override val startTime by lazy { remoteMonthlyWeekScheduleRecord.startTime }

    override val rootTaskKey by lazy { TaskKey(remoteMonthlyWeekScheduleRecord.projectId, remoteMonthlyWeekScheduleRecord.taskId) }

    override val dayOfMonth get() = remoteMonthlyWeekScheduleRecord.dayOfMonth

    override val dayOfWeek get() = remoteMonthlyWeekScheduleRecord.dayOfWeek

    override val beginningOfMonth get() = remoteMonthlyWeekScheduleRecord.beginningOfMonth

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
