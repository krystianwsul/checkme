package com.krystianwsul.checkme.utils.time

import androidx.annotation.StringRes
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.days
import java.util.*
import org.joda.time.DateTime as DateTimeJoda

fun Date.getDisplayText(capitalize: Boolean = true): String {
    val now = DateTimeTz.nowLocal()

    val todayDate = Date(now)
    val yesterdayDate = Date(now - 1.days)
    val tomorrowDate = Date(now + 1.days)

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

fun DateTimeJoda.toExactTimeStamp() = ExactTimeStamp.Local(millis)

val TimeStamp.calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = long }

fun DateTime.getDisplayText() = date.getDisplayText() + ", " + time.toString()