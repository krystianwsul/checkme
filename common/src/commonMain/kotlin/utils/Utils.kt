package com.krystianwsul.common.utils

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.soywiz.klock.Month

fun getDateInMonth(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        beginningOfMonth: Boolean
) = Date(year, month, if (beginningOfMonth) dayOfMonth else Month(month).days(year) - dayOfMonth + 1)

fun getDateInMonth(year: Int, month: Int, dayOfMonth: Int, dayOfWeek: DayOfWeek, beginningOfMonth: Boolean): Date {
    if (beginningOfMonth) {
        val first = Date(year, month, 1)

        val day = if (dayOfWeek.ordinal >= first.dayOfWeek.ordinal) {
            (dayOfMonth - 1) * 7 + (dayOfWeek.ordinal - first.dayOfWeek.ordinal) + 1
        } else {
            dayOfMonth * 7 + (dayOfWeek.ordinal - first.dayOfWeek.ordinal) + 1
        }

        return Date(year, month, day)
    } else {
        val daysInMonth = Month(month).days(year)

        val last = Date(year, month, daysInMonth)

        val day = if (dayOfWeek.ordinal <= last.dayOfWeek.ordinal) {
            (dayOfMonth - 1) * 7 + (last.dayOfWeek.ordinal - dayOfWeek.ordinal) + 1
        } else {
            dayOfMonth * 7 + (last.dayOfWeek.ordinal - dayOfWeek.ordinal) + 1
        }

        return Date(year, month, daysInMonth - day + 1)
    }
}