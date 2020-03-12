package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.RemoteCustomTimeId

@Parcelize
data class TimePair(
        val customTimeKey: CustomTimeKey<*, *>?,
        val hourMinute: HourMinute?
) : Parcelable {

    constructor(customTimeKey: CustomTimeKey<*, *>) : this(customTimeKey, null)

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

    fun destructureRemote(): Triple<RemoteCustomTimeId?, Int?, Int?> {
        val remoteCustomTimeId: RemoteCustomTimeId?
        val hour: Int?
        val minute: Int?

        if (customTimeKey != null) {
            check(hourMinute == null)

            remoteCustomTimeId = customTimeKey.remoteCustomTimeId
            hour = null
            minute = null
        } else {
            remoteCustomTimeId = null
            hour = hourMinute!!.hour
            minute = hourMinute.minute
        }

        return Triple(remoteCustomTimeId, hour, minute)
    }
}
