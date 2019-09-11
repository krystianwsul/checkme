package com.krystianwsul.checkme.utils.time

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import java.util.*
import org.joda.time.DateTime as DateTimeJoda

fun Date.getDisplayText(): String {
    val todayCalendar = Calendar.getInstance()
    val todayDate = Date(todayCalendar.toDateTimeTz())

    val yesterdayCalendar = Calendar.getInstance()
    yesterdayCalendar.add(Calendar.DATE, -1)
    val yesterdayDate = Date(yesterdayCalendar.toDateTimeTz())

    val tomorrowCalendar = Calendar.getInstance()
    tomorrowCalendar.add(Calendar.DATE, 1)
    val tomorrowDate = Date(tomorrowCalendar.toDateTimeTz())

    val context = MyApplication.context

    return when (this) {
        todayDate -> context.getString(R.string.today)
        yesterdayDate -> context.getString(R.string.yesterday)
        tomorrowDate -> context.getString(R.string.tomorrow)
        else -> dayOfWeek.toString() + ", " + toString()
    }
}

fun Calendar.toDateTimeSoy() = DateTimeSoy.fromUnix(timeInMillis)

fun Calendar.toDateTimeTz() = toDateTimeSoy().local

val ExactTimeStamp.calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = long }

fun DateTimeJoda.toExactTimeStamp() = ExactTimeStamp(millis)

val TimeStamp.calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = long }

fun DateTime.getDisplayText() = date.getDisplayText() + ", " + time.toString()