package com.krystianwsul.common.utils

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateSoy
import com.krystianwsul.common.time.DayOfWeek
import com.soywiz.klock.Month

fun getDateSoyInMonth(
    year: Int,
    month: Int,
    dayOfMonth: Int,
    beginningOfMonth: Boolean
) = DateSoy(year, month, if (beginningOfMonth) dayOfMonth else Month(month).days(year) - dayOfMonth + 1)

fun getDateInMonth(
    year: Int,
    month: Int,
    dayOfMonth: Int,
    beginningOfMonth: Boolean
) = Date(getDateSoyInMonth(year, month, dayOfMonth, beginningOfMonth))

fun getDateSoyInMonth(year: Int, month: Int, weekOfMonth: Int, dayOfWeek: DayOfWeek, beginningOfMonth: Boolean): DateSoy {
    if (beginningOfMonth) {
        val first = Date(year, month, 1)

        val day = if (dayOfWeek.ordinal >= first.dayOfWeek.ordinal) {
            (weekOfMonth - 1) * 7 + (dayOfWeek.ordinal - first.dayOfWeek.ordinal) + 1
        } else {
            weekOfMonth * 7 + (dayOfWeek.ordinal - first.dayOfWeek.ordinal) + 1
        }

        return DateSoy(year, month, day)
    } else {
        val daysInMonth = Month(month).days(year)

        val last = Date(year, month, daysInMonth)

        val day = if (dayOfWeek.ordinal <= last.dayOfWeek.ordinal) {
            (weekOfMonth - 1) * 7 + (last.dayOfWeek.ordinal - dayOfWeek.ordinal) + 1
        } else {
            weekOfMonth * 7 + (last.dayOfWeek.ordinal - dayOfWeek.ordinal) + 1
        }

        return DateSoy(year, month, daysInMonth - day + 1)
    }
}

fun getDateInMonth(year: Int, month: Int, weekOfMonth: Int, dayOfWeek: DayOfWeek, beginningOfMonth: Boolean) =
    Date(getDateSoyInMonth(year, month, weekOfMonth, dayOfWeek, beginningOfMonth))

fun <T> Collection<T>.singleOrEmpty(): T? {
    check(size < 2) { "size is $size" }

    return singleOrNull()
}

fun <T, U : Any> Map<T, U?>.filterValuesNotNull(): Map<T, U> = filterValues { it != null }.mapValues { it.value!! }

fun <T, U, V> Map<T, U>.mapValuesNotNull(mapper: (Map.Entry<T, U>) -> V?): Map<T, V> =
        mapValues(mapper).filterValuesNotNull()