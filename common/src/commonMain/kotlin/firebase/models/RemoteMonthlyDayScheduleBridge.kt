package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.MonthlyDayScheduleBridge
import com.krystianwsul.common.firebase.records.MonthlyDayScheduleRecord
import com.krystianwsul.common.utils.ProjectType

class RemoteMonthlyDayScheduleBridge<T : ProjectType>(
        private val monthlyDayScheduleRecord: MonthlyDayScheduleRecord<T>
) : RemoteRepeatingScheduleBridge<T>(monthlyDayScheduleRecord), MonthlyDayScheduleBridge<T> {

    override val dayOfMonth get() = monthlyDayScheduleRecord.dayOfMonth

    override val beginningOfMonth get() = monthlyDayScheduleRecord.beginningOfMonth
}
