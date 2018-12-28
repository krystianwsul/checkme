package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.WeeklyScheduleBridge
import com.krystianwsul.checkme.persistencemodel.DailyScheduleRecord
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.time.DayOfWeek

class LocalDailyScheduleBridge(scheduleRecord: ScheduleRecord, private val mDailyScheduleRecord: DailyScheduleRecord) : LocalScheduleBridge(scheduleRecord), WeeklyScheduleBridge {

    override val customTimeKey get() = mDailyScheduleRecord.customTimeId?.let { CustomTimeKey.LocalCustomTimeKey(it) }

    override val hour get() = mDailyScheduleRecord.hour

    override val minute get() = mDailyScheduleRecord.minute

    override fun delete() {
        scheduleRecord.delete()
        mDailyScheduleRecord.delete()
    }

    override val daysOfWeek = DayOfWeek.values()
            .map { it.ordinal }
            .toSet()
}
