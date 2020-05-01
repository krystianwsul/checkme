package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.MonthlyDayScheduleBridge
import com.krystianwsul.common.firebase.records.MonthlyDayScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType

class RemoteMonthlyDayScheduleBridge<T : ProjectType>(
        private val monthlyDayScheduleRecord: MonthlyDayScheduleRecord<T>
) : RemoteScheduleBridge<T>(monthlyDayScheduleRecord), MonthlyDayScheduleBridge<T> {

    override val dayOfMonth get() = monthlyDayScheduleRecord.dayOfMonth

    override val beginningOfMonth get() = monthlyDayScheduleRecord.beginningOfMonth

    override val from by lazy {
        monthlyDayScheduleRecord.from?.let { Date.fromJson(it) }
    }

    override val until by lazy {
        monthlyDayScheduleRecord.until?.let { Date.fromJson(it) }
    }
}
