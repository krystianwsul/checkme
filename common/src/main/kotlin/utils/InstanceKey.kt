package com.krystianwsul.common.utils

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.TimePair

@Parcelize
data class InstanceKey(val taskKey: TaskKey, val instanceScheduleKey: InstanceScheduleKey) : Parcelable, Serializable {

    constructor(taskKey: TaskKey, scheduleDate: Date, scheduleTimePair: TimePair) :
            this(taskKey, InstanceScheduleKey(scheduleDate, scheduleTimePair))
}
