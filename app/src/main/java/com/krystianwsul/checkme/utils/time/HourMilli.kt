package com.krystianwsul.checkme.utils.time

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class HourMilli(
        val hour: Int,
        val minute: Int,
        val second: Int,
        val milli: Int) : Comparable<HourMilli>, Parcelable {

    constructor(dateTimeSoy: DateTimeSoy) : this(
            dateTimeSoy.hours,
            dateTimeSoy.minutes,
            dateTimeSoy.seconds,
            dateTimeSoy.milliseconds
    )

    override fun compareTo(other: HourMilli) = compareValuesBy(this, other, { it.hour }, { it.minute }, { it.second }, { it.milli })

    override fun toString() = String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second) + ":" + String.format("%03d", milli)
}
