package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.MonthlyDayScheduleBridge
import com.krystianwsul.checkme.persistencemodel.MonthlyDayScheduleRecord
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord
import com.krystianwsul.checkme.utils.CustomTimeKey

class LocalMonthlyDayScheduleBridge(scheduleRecord: ScheduleRecord, private val monthlyDayScheduleRecord: MonthlyDayScheduleRecord) : LocalScheduleBridge(scheduleRecord), MonthlyDayScheduleBridge {

    override val dayOfMonth get() = monthlyDayScheduleRecord.dayOfMonth

    override val beginningOfMonth get() = monthlyDayScheduleRecord.beginningOfMonth

    override val customTimeKey get() = monthlyDayScheduleRecord.customTimeId?.let { CustomTimeKey.LocalCustomTimeKey(it) }

    override val hour get() = monthlyDayScheduleRecord.hour

    override val minute get() = monthlyDayScheduleRecord.minute

    override fun delete() {
        scheduleRecord.delete()
        monthlyDayScheduleRecord.delete()
    }
}
