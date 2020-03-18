package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.MonthlyDayScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteMonthlyDayScheduleRecord
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey

class RemoteMonthlyDayScheduleBridge<T : RemoteCustomTimeId>(
        remoteProjectRecord: RemoteProjectRecord<T, *>,
        private val remoteMonthlyDayScheduleRecord: RemoteMonthlyDayScheduleRecord<T, *>
) : RemoteScheduleBridge<T>(remoteProjectRecord, remoteMonthlyDayScheduleRecord), MonthlyDayScheduleBridge {

    override val startTime by lazy { remoteMonthlyDayScheduleRecord.startTime }

    override val rootTaskKey by lazy { TaskKey(remoteMonthlyDayScheduleRecord.projectId, remoteMonthlyDayScheduleRecord.taskId) }

    override val dayOfMonth get() = remoteMonthlyDayScheduleRecord.dayOfMonth

    override val beginningOfMonth get() = remoteMonthlyDayScheduleRecord.beginningOfMonth

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

    override val scheduleId get() = ScheduleId(remoteMonthlyDayScheduleRecord.projectId, remoteMonthlyDayScheduleRecord.taskId, remoteMonthlyDayScheduleRecord.id)

    override val from by lazy {
        remoteMonthlyDayScheduleRecord.from?.let { Date.fromJson(it) }
    }

    override val until by lazy {
        remoteMonthlyDayScheduleRecord.until?.let { Date.fromJson(it) }
    }
}
