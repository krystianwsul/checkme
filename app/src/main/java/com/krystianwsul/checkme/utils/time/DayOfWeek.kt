package com.krystianwsul.checkme.utils.time

import android.text.TextUtils

import java.text.DateFormatSymbols
import java.util.*

enum class DayOfWeek {
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY;

    companion object {

        fun getDayFromCalendar(calendar: Calendar): DayOfWeek {
            val day = calendar.get(Calendar.DAY_OF_WEEK)

            return values()[day - 1]
        }
    }

    override fun toString(): String {
        val weekDay = DateFormatSymbols.getInstance().weekdays[ordinal + 1]
        check(!TextUtils.isEmpty(weekDay))

        return weekDay
    }
}
