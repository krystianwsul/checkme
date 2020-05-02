package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.MonthlyWeekScheduleBridge
import com.krystianwsul.common.firebase.records.MonthlyWeekScheduleRecord
import com.krystianwsul.common.utils.ProjectType

class RemoteMonthlyWeekScheduleBridge<T : ProjectType>(
        private val monthlyWeekScheduleRecord: MonthlyWeekScheduleRecord<T>
) : RemoteRepeatingScheduleBridge<T>(monthlyWeekScheduleRecord), MonthlyWeekScheduleBridge<T> {

    override val dayOfMonth get() = monthlyWeekScheduleRecord.dayOfMonth

    override val dayOfWeek get() = monthlyWeekScheduleRecord.dayOfWeek

    override val beginningOfMonth get() = monthlyWeekScheduleRecord.beginningOfMonth
}
