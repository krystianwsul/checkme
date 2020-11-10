package com.krystianwsul.common.time

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.Serializable
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.ISO8601
import com.soywiz.klock.parse

@Parcelize
data class Date(val year: Int, val month: Int, val day: Int) : Comparable<Date>, Parcelable, Serializable {

    companion object {

        fun today() = Date(DateTimeTz.nowLocal())

        fun fromJson(json: String) = ISO8601.DATE_CALENDAR_COMPLETE
                .parse(json)
                .utc
                .let { Date(it.yearInt, it.month1, it.dayOfMonth) }
    }

    val dayOfWeek get() = DayOfWeek.fromDate(this)

    init {
        check(month in (1..12))
        check(day in (1..31))
    }

    constructor(dateTimeTz: DateTimeTz) : this(dateTimeTz.yearInt, dateTimeTz.month1, dateTimeTz.dayOfMonth)

    override fun compareTo(other: Date) = compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

    override fun toString() = toDateSoy().formatDate()

    fun toJson() = toDateSoy().format(ISO8601.DATE_CALENDAR_COMPLETE)

    fun toDateSoy() = DateSoy(year, month, day)

    fun toMidnightExactTimeStamp() = ExactTimeStamp.Local(this, HourMilli(0, 0, 0, 0))
}
