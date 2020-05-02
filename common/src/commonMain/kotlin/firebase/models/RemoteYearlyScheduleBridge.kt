package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.YearlyScheduleBridge
import com.krystianwsul.common.firebase.records.YearlyScheduleRecord
import com.krystianwsul.common.utils.ProjectType

class RemoteYearlyScheduleBridge<T : ProjectType>(
        private val yearlyScheduleRecord: YearlyScheduleRecord<T>
) : RemoteRepeatingScheduleBridge<T>(yearlyScheduleRecord), YearlyScheduleBridge<T> {

    override val month get() = yearlyScheduleRecord.month
    override val day get() = yearlyScheduleRecord.day
}
