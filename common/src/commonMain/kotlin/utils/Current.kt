package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

interface Current {

    val startExactTimeStamp: ExactTimeStamp.Local
    val endExactTimeStamp: ExactTimeStamp.Local?

    fun notDeleted(exactTimeStamp: ExactTimeStamp.Local) = endExactTimeStamp?.let { it > exactTimeStamp } != false

    fun afterStart(exactTimeStamp: ExactTimeStamp.Local) = startExactTimeStamp <= exactTimeStamp

    fun current(exactTimeStamp: ExactTimeStamp.Local) = afterStart(exactTimeStamp) && notDeleted(exactTimeStamp)

    fun requireNotDeleted(exactTimeStamp: ExactTimeStamp.Local) {
        if (!notDeleted(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    fun requireCurrent(exactTimeStamp: ExactTimeStamp.Local) {
        if (!current(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    fun requireNotCurrent(exactTimeStamp: ExactTimeStamp.Local) {
        if (current(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    private fun throwTime(exactTimeStamp: ExactTimeStamp.Local): Nothing = throw TimeException(
            "$this exactTimeStamps start: $startExactTimeStamp, end: $endExactTimeStamp, time: $exactTimeStamp"
    )

    class TimeException(message: String) : Exception(message)
}