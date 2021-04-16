package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

interface Endable {

    val endExactTimeStamp: ExactTimeStamp.Local?

    fun notDeleted(exactTimeStamp: ExactTimeStamp.Local) = endExactTimeStamp?.let { it > exactTimeStamp } != false

    fun requireNotDeleted(exactTimeStamp: ExactTimeStamp.Local) {
        if (!notDeleted(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    fun throwTime(exactTimeStamp: ExactTimeStamp.Local): Nothing = throw TimeException(
            "$this exactTimeStamps start: end: $endExactTimeStamp, time: $exactTimeStamp"
    )

    class TimeException(message: String) : Exception(message)
}