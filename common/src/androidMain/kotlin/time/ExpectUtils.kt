package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat

actual fun DateTimeTz.formatDate() = DateTimeFormat.forStyle("S-").print(LocalDate(yearInt, month1, dayOfMonth))!!

actual fun DateTimeTz.formatTime() = DateTimeFormat.forStyle("-S").print(LocalTime(hours, minutes))!!