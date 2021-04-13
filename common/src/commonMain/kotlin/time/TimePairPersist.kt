package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize

@Parcelize
class TimePairPersist private constructor(
        var customTimeKey: CustomTimeKey?,
        private var _hourMinute: HourMinute,
) : Parcelable {

    val hourMinute get() = _hourMinute

    val timePair get() = customTimeKey?.let(::TimePair) ?: TimePair(hourMinute)

    constructor(timePair: TimePair) : this(
            timePair.customTimeKey,
            timePair.hourMinute ?: HourMinute.nextHour.second,
    )

    fun setHourMinute(hourMinute: HourMinute) {
        _hourMinute = hourMinute
        customTimeKey = null
    }
}
