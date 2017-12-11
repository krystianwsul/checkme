package com.krystianwsul.checkme.utils.time

import android.os.Parcel
import android.os.Parcelable
import com.krystianwsul.checkme.utils.CustomTimeKey

class TimePairPersist : Parcelable {

    var customTimeKey: CustomTimeKey? = null
    var hourMinute: HourMinute
        set(value) {
            field = value
            customTimeKey = null
        }

    val timePair get() = customTimeKey?.let { TimePair(it) } ?: TimePair(hourMinute)

    private constructor(customTimeKey: CustomTimeKey?, hourMinute: HourMinute) {
        this.customTimeKey = customTimeKey
        this.hourMinute = hourMinute
    }

    constructor(timePair: TimePair) {
        customTimeKey = timePair.mCustomTimeKey
        hourMinute = timePair.mHourMinute ?: HourMinute.getNextHour().second
    }

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

    companion object {

        var CREATOR: Parcelable.Creator<TimePairPersist> = object : Parcelable.Creator<TimePairPersist> {
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
}
