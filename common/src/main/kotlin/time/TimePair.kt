package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.Serializable

@Parcelize
data class TimePair(val customTimeKey: CustomTimeKey?, val hourMinute: HourMinute?) : Parcelable, Serializable {

    companion object {}

    constructor(customTimeKey: CustomTimeKey) : this(customTimeKey, null)

    constructor(hourMinute: HourMinute) : this(null, hourMinute)

    constructor(hour: Int, minute: Int) : this(HourMinute(hour, minute))

    init {
        check((customTimeKey == null) != (hourMinute == null))
    }

    fun toJsonTime() = JsonTime.fromTimePair(this)
}
