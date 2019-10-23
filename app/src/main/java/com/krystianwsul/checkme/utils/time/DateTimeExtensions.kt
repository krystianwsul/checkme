package com.krystianwsul.checkme.utils.time

import androidx.annotation.StringRes
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import java.util.*
import org.joda.time.DateTime as DateTimeJoda

fun Date.getDisplayText(capitalize: Boolean = true): String {
    val todayCalendar = Calendar.getInstance()
    val todayDate = Date(todayCalendar.toDateTimeTz())

    val yesterdayCalendar = Calendar.getInstance()
    yesterdayCalendar.add(Calendar.DATE, -1)
    val yesterdayDate = Date(yesterdayCalendar.toDateTimeTz())

    val tomorrowCalendar = Calendar.getInstance()
    tomorrowCalendar.add(Calendar.DATE, 1)
    val tomorrowDate = Date(tomorrowCalendar.toDateTimeTz())

    fun getString(@StringRes id: Int) = MyApplication.context
            .getString(id)
            .let {
                if (capitalize)
                    it
                else
                    it.toLowerCase(Locale.getDefault())
            }

    return when (this) {
        todayDate -> getString(R.string.today)
        yesterdayDate -> getString(R.string.yesterday)
        tomorrowDate -> getString(R.string.tomorrow)
        else -> dayOfWeek.toString() + ", " + toString()
    }
}

fun Calendar.toDateTimeSoy() = DateTimeSoy.fromUnix(timeInMillis)

fun Calendar.toDateTimeTz() = toDateTimeSoy().local

val ExactTimeStamp.calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = long }

fun DateTimeJoda.toExactTimeStamp() = ExactTimeStamp(millis)

val TimeStamp.calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = long }

fun DateTime.getDisplayText() = date.getDisplayText() + ", " + time.toString()