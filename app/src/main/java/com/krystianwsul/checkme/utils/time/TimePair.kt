package com.krystianwsul.checkme.utils.time

import android.os.Parcelable
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.firebase.RemoteProjectFactory
import com.krystianwsul.checkme.utils.CustomTimeKey

import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class TimePair(val customTimeKey: CustomTimeKey?, val hourMinute: HourMinute?) : Parcelable, Serializable {

    constructor(customTimeKey: CustomTimeKey) : this(customTimeKey, null)

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

    fun destructure(remoteProjectFactory: RemoteProjectFactory, remoteProject: RemoteProject): Triple<String?, Int?, Int?> {
        val remoteCustomTimeId: String?
        val hour: Int?
        val minute: Int?
        if (customTimeKey != null) {
            check(hourMinute == null)

            remoteCustomTimeId = remoteProjectFactory.getRemoteCustomTimeId(customTimeKey as CustomTimeKey.LocalCustomTimeKey, remoteProject)
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
