package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.MonthlyDayScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteMonthlyDayScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteMonthlyDayScheduleBridge<T : RemoteCustomTimeId, U : ProjectKey>(
        private val remoteMonthlyDayScheduleRecord: RemoteMonthlyDayScheduleRecord<T, U>
) : RemoteScheduleBridge<T, U>(remoteMonthlyDayScheduleRecord), MonthlyDayScheduleBridge {

    override val dayOfMonth get() = remoteMonthlyDayScheduleRecord.dayOfMonth

    override val beginningOfMonth get() = remoteMonthlyDayScheduleRecord.beginningOfMonth

    override val from by lazy {
        remoteMonthlyDayScheduleRecord.from?.let { Date.fromJson(it) }
    }

    override val until by lazy {
        remoteMonthlyDayScheduleRecord.until?.let { Date.fromJson(it) }
    }
}
