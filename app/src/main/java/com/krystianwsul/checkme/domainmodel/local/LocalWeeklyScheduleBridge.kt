package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.WeeklyScheduleBridge
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord
import com.krystianwsul.checkme.persistencemodel.WeeklyScheduleRecord
import com.krystianwsul.checkme.utils.CustomTimeKey

internal class LocalWeeklyScheduleBridge(scheduleRecord: ScheduleRecord, private val mWeeklyScheduleRecord: WeeklyScheduleRecord) : LocalScheduleBridge(scheduleRecord), WeeklyScheduleBridge {

    override val daysOfWeek get() = setOf(mWeeklyScheduleRecord.dayOfWeek)

    override val customTimeKey get() = mWeeklyScheduleRecord.customTimeId?.let { CustomTimeKey(it) }

    override val hour get() = mWeeklyScheduleRecord.hour

    override val minute get() = mWeeklyScheduleRecord.minute

    override fun delete() {
        scheduleRecord.delete()
        mWeeklyScheduleRecord.delete()
    }
}
