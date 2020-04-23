package com.krystianwsul.common.time

import com.krystianwsul.common.utils.*

@Parcelize
data class TimePair(
        val customTimeKey: CustomTimeKey<*>?,
        val hourMinute: HourMinute?
) : Parcelable, Serializable {

    constructor(customTimeKey: CustomTimeKey<*>) : this(customTimeKey, null)

    constructor(hourMinute: HourMinute) : this(null, hourMinute)

    init {
        check((customTimeKey == null) != (hourMinute == null))
    }

    override fun toString(): String {
        return if (customTimeKey != null) {
            check(hourMinute == null)

            super.toString() + ": " + customTimeKey
        } else {
            super.toString() + ": " + hourMinute!!
        }
    }

    fun destructureRemote(): Triple<CustomTimeId<*>?, Int?, Int?> {
        val customTimeId: CustomTimeId<*>?
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
