package com.krystianwsul.common.utils

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.TimePair

@Parcelize
data class InstanceScheduleKey(val scheduleDate: Date, val scheduleTimePair: TimePair) : Parcelable, Serializable {

    constructor(dateTime: DateTime) : this(dateTime.date, dateTime.time.timePair)
}