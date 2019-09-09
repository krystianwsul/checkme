package com.krystianwsul.checkme.utils.time

import android.os.Parcelable
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class TimePair(
        val customTimeKey: CustomTimeKey<*>?,
        val hourMinute: HourMinute?) : Parcelable, Serializable {

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

    fun <T : RemoteCustomTimeId> destructureRemote(remoteProject: RemoteProject<T>): Triple<T?, Int?, Int?> {
        val remoteCustomTimeId: T?
        val hour: Int?
        val minute: Int?
        if (customTimeKey != null) {
            check(hourMinute == null)

            remoteCustomTimeId = remoteProject.getRemoteCustomTimeKey(customTimeKey).remoteCustomTimeId

            hour = null
            minute = null
        } else {
            remoteCustomTimeId = null
            hour = hourMinute!!.hour
            minute = hourMinute.minute
        }

        return Triple(remoteCustomTimeId, hour, minute)
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
