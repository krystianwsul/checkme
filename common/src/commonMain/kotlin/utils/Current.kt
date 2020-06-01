package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

interface Current {

    val startExactTimeStamp: ExactTimeStamp
    val endExactTimeStamp: ExactTimeStamp?

    fun notDeleted(exactTimeStamp: ExactTimeStamp) = endExactTimeStamp?.let { it > exactTimeStamp } != false

    fun afterStart(exactTimeStamp: ExactTimeStamp) = startExactTimeStamp <= exactTimeStamp

    fun current(exactTimeStamp: ExactTimeStamp) = afterStart(exactTimeStamp) && notDeleted(exactTimeStamp)

    fun requireNotDeleted(exactTimeStamp: ExactTimeStamp) {
        if (!notDeleted(exactTimeStamp))
            throwTime(exactTimeStamp)
    }

    fun requireCurrent(exactTimeStamp: ExactTimeStamp) {
        if (!current(exactTimeStamp))
            throwTime(exactTimeStamp)
    }

    fun requireNotCurrent(exactTimeStamp: ExactTimeStamp) {
        if (current(exactTimeStamp))
            throwTime(exactTimeStamp)
    }

    fun throwTime(exactTimeStamp: ExactTimeStamp): Nothing = throw TimeException("$this start: $startExactTimeStamp, end: $endExactTimeStamp, time: $exactTimeStamp")

    private class TimeException(message: String) : Exception(message)
}