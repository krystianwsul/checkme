package com.krystianwsul.common.time

typealias DateTimeSoy = com.soywiz.klock.DateTime

fun DateTimeSoy.toExactTimeStamp() = ExactTimeStamp(unixMillisLong)