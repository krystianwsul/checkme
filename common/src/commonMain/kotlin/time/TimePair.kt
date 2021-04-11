package com.krystianwsul.common.time

import com.krystianwsul.common.utils.*

@Parcelize
data class TimePair(
        val customTimeKey: CustomTimeKey.Project<*>?, // todo customtime timepair
        val hourMinute: HourMinute?,
) : Parcelable, Serializable {

    constructor(customTimeKey: CustomTimeKey.Project<*>) : this(customTimeKey, null)

    constructor(hourMinute: HourMinute) : this(null, hourMinute)

    init {
        check((customTimeKey == null) != (hourMinute == null))
    }

    fun destructureRemote(): Triple<CustomTimeId.Project<*>?, Int?, Int?> {
        val customTimeId: CustomTimeId.Project<*>?
        val hour: Int?
        val minute: Int?

        if (customTimeKey != null) {
            check(hourMinute == null)

            customTimeId = customTimeKey.customTimeId
            hour = null
            minute = null
        } else {
            customTimeId = null
            hour = hourMinute!!.hour
            minute = hourMinute.minute
        }

        return Triple(customTimeId, hour, minute)
    }
}
