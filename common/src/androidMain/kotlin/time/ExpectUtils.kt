package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat

private val dateFormat = DateTimeFormat.forStyle("S-")
private val timeFormat = DateTimeFormat.forStyle("-S")

actual fun DateTimeTz.formatDate() = dateFormat.print(LocalDate(yearInt, month1, dayOfMonth))!!
actual fun DateTimeTz.formatTime() = timeFormat.print(LocalTime(hours, minutes))!!