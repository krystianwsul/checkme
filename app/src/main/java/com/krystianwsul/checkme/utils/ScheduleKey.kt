package com.krystianwsul.checkme.utils

import android.os.Parcelable
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.TimePair
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class ScheduleKey(val scheduleDate: Date, val scheduleTimePair: TimePair) : Parcelable, Serializable {

    override fun toString() = super.toString() + ": " + scheduleDate + ", " + scheduleTimePair
}
