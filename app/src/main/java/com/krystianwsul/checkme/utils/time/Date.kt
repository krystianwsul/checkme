package com.krystianwsul.checkme.utils.time

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.Serializable
import java.util.*

@Parcelize
data class Date(val year: Int, val month: Int, val day: Int) : Comparable<Date>, Parcelable, Serializable {

    companion object {

        private const val PATTERN = "yyyy-MM-dd"

        private val format = DateTimeFormat.forPattern(PATTERN)

        fun today() = Date(Calendar.getInstance())

        fun fromJson(json: String) = format.parseLocalDate(json).let { Date(it.year, it.monthOfYear, it.dayOfMonth) }
    }

    val dayOfWeek get() = DayOfWeek.fromDate(this)

    val calendar get() = GregorianCalendar(year, month - 1, day)

    constructor(calendar: Calendar) : this(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))

    override fun compareTo(other: Date) = compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

    override fun toString() = DateTimeFormat.forStyle("S-").print(LocalDate(year, month, day))!!

    fun toJson() = LocalDate(year, month, day).toString(PATTERN)!!
}
