package com.krystianwsul.checkme.utils.time

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class HourMilli(val hour: Int, val minute: Int, val second: Int, val milli: Int) : Comparable<HourMilli>, Parcelable {

    constructor(calendar: Calendar) : this(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), calendar.get(Calendar.MILLISECOND))

    override fun compareTo(other: HourMilli) = compareValuesBy(this, other, { it.hour }, { it.minute }, { it.second }, { it.milli })

    @SuppressLint("DefaultLocale")
    override fun toString() = String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second) + ":" + String.format("%03d", milli)
}
