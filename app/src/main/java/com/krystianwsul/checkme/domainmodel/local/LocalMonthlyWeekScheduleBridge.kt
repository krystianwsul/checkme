package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.MonthlyWeekScheduleBridge
import com.krystianwsul.checkme.persistencemodel.MonthlyWeekScheduleRecord
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord
import com.krystianwsul.checkme.utils.CustomTimeKey

class LocalMonthlyWeekScheduleBridge(scheduleRecord: ScheduleRecord, private val monthlyWeekScheduleRecord: MonthlyWeekScheduleRecord) : LocalScheduleBridge(scheduleRecord), MonthlyWeekScheduleBridge {

    override val dayOfMonth get() = monthlyWeekScheduleRecord.dayOfMonth

    override val dayOfWeek get() = monthlyWeekScheduleRecord.dayOfWeek

    override val beginningOfMonth get() = monthlyWeekScheduleRecord.beginningOfMonth

    override val customTimeKey get() = monthlyWeekScheduleRecord.customTimeId?.let { CustomTimeKey.LocalCustomTimeKey(it) }

    override val hour get() = monthlyWeekScheduleRecord.hour

    override val minute get() = monthlyWeekScheduleRecord.minute

    override fun delete() {
        scheduleRecord.delete()
        monthlyWeekScheduleRecord.delete()
    }
}
