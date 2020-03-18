package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.WeeklyScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteProjectRecord
import com.krystianwsul.common.firebase.records.RemoteWeeklyScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteWeeklyScheduleBridge<T : RemoteCustomTimeId>(
        remoteProjectRecord: RemoteProjectRecord<T, *>,
        private val remoteWeeklyScheduleRecord: RemoteWeeklyScheduleRecord<T, *>
) : RemoteScheduleBridge<T>(remoteProjectRecord, remoteWeeklyScheduleRecord), WeeklyScheduleBridge {

    override val daysOfWeek get() = setOf(remoteWeeklyScheduleRecord.dayOfWeek)

    override val from by lazy {
        remoteWeeklyScheduleRecord.from?.let { Date.fromJson(it) }
    }

    override val until by lazy {
        remoteWeeklyScheduleRecord.until?.let { Date.fromJson(it) }
    }
}
