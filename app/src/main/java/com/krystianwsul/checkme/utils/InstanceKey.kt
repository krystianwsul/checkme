package com.krystianwsul.checkme.utils

import android.os.Parcelable
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.TimePair
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class InstanceKey(val taskKey: TaskKey, val scheduleKey: ScheduleKey) : Parcelable, Serializable {

    val type get() = taskKey.type

    constructor(taskKey: TaskKey, scheduleDate: Date, scheduleTimePair: TimePair) : this(taskKey, ScheduleKey(scheduleDate, scheduleTimePair))

    override fun toString() = super.toString() + ": " + taskKey + ", " + scheduleKey
}
