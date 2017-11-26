package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.DailyScheduleBridge
import com.krystianwsul.checkme.persistencemodel.DailyScheduleRecord
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord
import com.krystianwsul.checkme.utils.CustomTimeKey

internal class LocalDailyScheduleBridge(scheduleRecord: ScheduleRecord, private val mDailyScheduleRecord: DailyScheduleRecord) : LocalScheduleBridge(scheduleRecord), DailyScheduleBridge {

    override fun getCustomTimeKey() = mDailyScheduleRecord.customTimeId?.let { CustomTimeKey(it) }

    override fun getHour() = mDailyScheduleRecord.hour

    override fun getMinute() = mDailyScheduleRecord.minute

    override fun delete() {
        mScheduleRecord.delete()
        mDailyScheduleRecord.delete()
    }
}
