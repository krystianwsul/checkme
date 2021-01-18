package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz

actual fun DateTimeTz.formatDate() = toString("yyyy-MM-dd")

actual fun DateTimeTz.formatTime() = toString("HH:mm")
actual fun TimeSoy.formatTime() = format("HH:mm")

actual fun DateSoy.formatDate() = format("yyyy-MM-dd")

actual fun DayOfWeek.format() = name