package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.SingleScheduleBridge
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord
import com.krystianwsul.checkme.persistencemodel.SingleScheduleRecord
import com.krystianwsul.checkme.utils.CustomTimeKey

class LocalSingleScheduleBridge(scheduleRecord: ScheduleRecord, private val singleScheduleRecord: SingleScheduleRecord) : LocalScheduleBridge(scheduleRecord), SingleScheduleBridge {

    override val year get() = singleScheduleRecord.year

    override val month get() = singleScheduleRecord.month

    override val day get() = singleScheduleRecord.day

    override val customTimeKey get() = singleScheduleRecord.customTimeId?.let { CustomTimeKey(it) }

    override val hour get() = singleScheduleRecord.hour

    override val minute get() = singleScheduleRecord.minute

    override fun delete() {
        scheduleRecord.delete()
        singleScheduleRecord.delete()
    }
}
