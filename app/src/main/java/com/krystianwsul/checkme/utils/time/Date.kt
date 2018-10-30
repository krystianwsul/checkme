package com.krystianwsul.checkme.utils.time

import android.os.Parcelable
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import kotlinx.android.parcel.Parcelize
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.Serializable
import java.util.*

@Parcelize
data class Date(val year: Int, val month: Int, val day: Int) : Comparable<Date>, Parcelable, Serializable {

    companion object {

        fun today() = Date(Calendar.getInstance())
    }

    val dayOfWeek get() = DayOfWeek.getDayFromCalendar(GregorianCalendar(year, month - 1, day))

    val calendar get() = GregorianCalendar(year, month - 1, day)

    constructor(calendar: Calendar) : this(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))

    override fun compareTo(other: Date) = compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

    override fun toString() = DateTimeFormat.forStyle("S-").print(LocalDate(year, month, day))!!

    fun getDisplayText(): String {
        val todayCalendar = Calendar.getInstance()
        val todayDate = Date(todayCalendar)

        val yesterdayCalendar = Calendar.getInstance()
        yesterdayCalendar.add(Calendar.DATE, -1)
        val yesterdayDate = Date(yesterdayCalendar)

        val tomorrowCalendar = Calendar.getInstance()
        tomorrowCalendar.add(Calendar.DATE, 1)
        val tomorrowDate = Date(tomorrowCalendar)

        val context = MyApplication.context

        return when (this) {
            todayDate -> context.getString(R.string.today)
            yesterdayDate -> context.getString(R.string.yesterday)
            tomorrowDate -> context.getString(R.string.tomorrow)
            else -> dayOfWeek.toString() + ", " + toString()
        }
    }
}
