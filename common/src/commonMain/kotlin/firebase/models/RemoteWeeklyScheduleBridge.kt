package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.WeeklyScheduleBridge
import com.krystianwsul.common.firebase.records.WeeklyScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType

class RemoteWeeklyScheduleBridge<T : ProjectType>(
        private val weeklyScheduleRecord: WeeklyScheduleRecord<T>
) : RemoteScheduleBridge<T>(weeklyScheduleRecord), WeeklyScheduleBridge<T> {

    override val daysOfWeek get() = setOf(weeklyScheduleRecord.dayOfWeek)

    override val from by lazy {
        weeklyScheduleRecord.from?.let { Date.fromJson(it) }
    }

    override val until by lazy {
        weeklyScheduleRecord.until?.let { Date.fromJson(it) }
    }
}
