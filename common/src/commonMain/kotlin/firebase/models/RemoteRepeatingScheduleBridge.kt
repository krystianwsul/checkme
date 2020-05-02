package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.schedules.RepeatingScheduleBridge
import com.krystianwsul.common.firebase.records.RepeatingScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType

abstract class RemoteRepeatingScheduleBridge<T : ProjectType>(
        private val repeatingScheduleRecord: RepeatingScheduleRecord<T>
) : RemoteScheduleBridge<T>(repeatingScheduleRecord), RepeatingScheduleBridge<T> {

    override val from by lazy {
        repeatingScheduleRecord.from?.let { Date.fromJson(it) }
    }

    override val until by lazy {
        repeatingScheduleRecord.until?.let { Date.fromJson(it) }
    }
}