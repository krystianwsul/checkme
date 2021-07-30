package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

interface Endable {

    val endExactTimeStamp: ExactTimeStamp.Local?

    val notDeleted get() = endExactTimeStamp == null

    fun requireNotDeleted() {
        if (!notDeleted) throwTime()
    }

    fun requireDeleted() {
        if (notDeleted) throwTime()
    }

    fun throwTime(): Nothing = throw TimeException("$this exactTimeStamps start: end: $endExactTimeStamp")

    class TimeException(message: String) : Exception(message)
}