package com.krystianwsul.common.time

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.PatternDateFormat
import com.soywiz.klock.format

actual fun DateTimeTz.formatDate() = toString("yyyy-MM-dd")

actual fun DateTimeTz.formatTime() = toString("HH:mm")
actual fun TimeSoy.formatTime() = format("HH:mm")

actual fun DateSoy.formatDate() = PatternDateFormat("yyyy-MM-dd").format(this)

actual fun DayOfWeek.format() = name