package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.WeeklyScheduleBridge
import com.krystianwsul.common.firebase.records.WeeklyScheduleRecord
import com.krystianwsul.common.utils.ProjectType

class RemoteWeeklyScheduleBridge<T : ProjectType>(
        private val weeklyScheduleRecord: WeeklyScheduleRecord<T>
) : RemoteRepeatingScheduleBridge<T>(weeklyScheduleRecord), WeeklyScheduleBridge<T> {

    override val daysOfWeek get() = setOf(weeklyScheduleRecord.dayOfWeek)
}
