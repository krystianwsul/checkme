package com.krystianwsul.common.utils

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.TimePair

@Parcelize
data class ScheduleKey(val scheduleDate: Date, val scheduleTimePair: TimePair) : Parcelable, Serializable {

    override fun toString() = super.toString() + ": " + scheduleDate + ", " + scheduleTimePair
}
