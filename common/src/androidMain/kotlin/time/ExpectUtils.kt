package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import java.time.format.TextStyle
import java.util.*

private typealias JavaDay = java.time.DayOfWeek

private val dateFormat = DateTimeFormat.forStyle("S-")
private val timeFormat = DateTimeFormat.forStyle("-S")

actual fun DateTimeTz.formatDate() = dateFormat.print(LocalDate(yearInt, month1, dayOfMonth))!!
actual fun DateTimeTz.formatTime() = timeFormat.print(LocalTime(hours, minutes))!!
actual fun TimeSoy.formatTime() = timeFormat.print(LocalTime(hour, minute))!!
actual fun DateSoy.formatDate() = dateFormat.print(LocalDate(year, month1, day))!!

actual fun DayOfWeek.format() = JavaDay.valueOf(name)
        .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        .capitalize(Locale.getDefault())