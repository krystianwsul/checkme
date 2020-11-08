package com.krystianwsul.common.time

const val testLong = 1589803853323

fun ExactTimeStamp.getHourString() = toString().substring(8, 13)

fun getLocalExactTimeStamp() = ExactTimeStamp.Local(testLong)

fun hoursToOffset(hours: Int) = hours * 60 * 60 * 1000.0

fun getOffsetExactTimeStamp(offsetHours: Int?): ExactTimeStamp.Offset {
    val offset = offsetHours?.let(::hoursToOffset)

    return ExactTimeStamp.Offset.fromOffset(testLong, offset)
}