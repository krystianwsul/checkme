package com.krystianwsul.common.time

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.soywiz.klock.DateTimeTz

@Parcelize
data class HourMilli(
        val hour: Int,
        val minute: Int,
        val second: Int,
        val milli: Int
) : Comparable<HourMilli>, Parcelable {

    constructor(dateTimeTz: DateTimeTz) : this(
            dateTimeTz.hours,
            dateTimeTz.minutes,
            dateTimeTz.seconds,
            dateTimeTz.milliseconds
    )

    override fun compareTo(other: HourMilli) = compareValuesBy(this, other, { it.hour }, { it.minute }, { it.second }, { it.milli })

    private fun Int.pad() = toString().let { if (it.length == 1) "0$it" else it }

    override fun toString() = hour.pad() + ":" + minute.pad() + ":" + second.pad() + ":" + milli.pad()
}
