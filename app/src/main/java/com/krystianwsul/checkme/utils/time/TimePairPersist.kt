package com.krystianwsul.checkme.utils.time

import android.os.Parcel
import android.os.Parcelable
import com.krystianwsul.checkme.utils.CustomTimeKey
import kotlin.properties.Delegates

class TimePairPersist private constructor(var customTimeKey: CustomTimeKey?, _hourMinute: HourMinute) : Parcelable {

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<TimePairPersist> = object : Parcelable.Creator<TimePairPersist> {
            override fun createFromParcel(source: Parcel): TimePairPersist {
                val hourMinute = source.readParcelable<HourMinute>(HourMinute::class.java.classLoader)!!

                val customTimeKey: CustomTimeKey? = if (source.readInt() == 1) {
                    source.readParcelable(CustomTimeKey::class.java.classLoader)!!
                } else {
                    null
                }

                return TimePairPersist(customTimeKey, hourMinute)
            }

            override fun newArray(size: Int) = arrayOfNulls<TimePairPersist>(size)
        }
    }

    var hourMinute by Delegates.observable(_hourMinute) { _, _, _ -> customTimeKey = null }

    val timePair get() = customTimeKey?.let { TimePair(it) } ?: TimePair(hourMinute)

    constructor(timePair: TimePair) : this(timePair.mCustomTimeKey, timePair.mHourMinute
            ?: HourMinute.getNextHour().second)

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.run {
            writeParcelable(hourMinute, 0)
            if (customTimeKey != null) {
                writeInt(1)
                writeParcelable(customTimeKey, 0)
            } else {
                writeInt(0)
            }
        }
    }
}
