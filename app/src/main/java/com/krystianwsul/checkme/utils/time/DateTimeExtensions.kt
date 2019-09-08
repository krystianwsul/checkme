package com.krystianwsul.checkme.utils.time

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import org.joda.time.DateTime
import java.util.*

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

fun Calendar.toDateTimeTz() = DateTimeSoy.fromUnix(timeInMillis).local

val Date.calendar get() = GregorianCalendar(year, month - 1, day)

val ExactTimeStamp.calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = long }

fun DateTime.toExactTimeStamp() = ExactTimeStamp(millis)