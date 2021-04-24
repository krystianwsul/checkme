package com.krystianwsul.common.utils

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.TimePair

@Parcelize
data class InstanceKey(val taskKey: TaskKey, val scheduleKey: ScheduleKey) : Parcelable, Serializable {

    constructor(taskKey: TaskKey, scheduleDate: Date, scheduleTimePair: TimePair) :
            this(taskKey, ScheduleKey(scheduleDate, scheduleTimePair))
}
