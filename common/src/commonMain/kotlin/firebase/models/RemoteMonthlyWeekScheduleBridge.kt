package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.MonthlyWeekScheduleBridge
import com.krystianwsul.common.firebase.records.RemoteMonthlyWeekScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType

class RemoteMonthlyWeekScheduleBridge<T : ProjectType>(
        private val remoteMonthlyWeekScheduleRecord: RemoteMonthlyWeekScheduleRecord<T>
) : RemoteScheduleBridge<T>(remoteMonthlyWeekScheduleRecord), MonthlyWeekScheduleBridge<T> {

    override val dayOfMonth get() = remoteMonthlyWeekScheduleRecord.dayOfMonth

    override val dayOfWeek get() = remoteMonthlyWeekScheduleRecord.dayOfWeek

    override val beginningOfMonth get() = remoteMonthlyWeekScheduleRecord.beginningOfMonth

    override val from by lazy {
        remoteMonthlyWeekScheduleRecord.from?.let { Date.fromJson(it) }
    }

    override val until by lazy {
        remoteMonthlyWeekScheduleRecord.until?.let { Date.fromJson(it) }
    }
}
