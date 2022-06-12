package com.krystianwsul.common.time

fun DateTimeSoy.toLocalExactTimeStamp() = ExactTimeStamp.Local(unixMillisLong)

fun DateTimePair?.orNextHour() = this ?: HourMinute.nextHour.let { DateTimePair(it.first, it.second) }