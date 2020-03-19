package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.MonthlyDayScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteMonthlyDayScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey

class RemoteMonthlyDayScheduleBridge<T : CustomTimeId, U : ProjectKey>(
        private val remoteMonthlyDayScheduleRecord: RemoteMonthlyDayScheduleRecord<T, U>
) : RemoteScheduleBridge<T, U>(remoteMonthlyDayScheduleRecord), MonthlyDayScheduleBridge<T, U> {

    override val dayOfMonth get() = remoteMonthlyDayScheduleRecord.dayOfMonth

    override val beginningOfMonth get() = remoteMonthlyDayScheduleRecord.beginningOfMonth

    override val from by lazy {
        remoteMonthlyDayScheduleRecord.from?.let { Date.fromJson(it) }
    }

    override val until by lazy {
        remoteMonthlyDayScheduleRecord.until?.let { Date.fromJson(it) }
    }
}
