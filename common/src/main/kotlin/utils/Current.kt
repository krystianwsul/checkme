package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

interface Current : Endable {

    val startExactTimeStamp: ExactTimeStamp.Local

    fun afterStart(exactTimeStamp: ExactTimeStamp.Local) = startExactTimeStamp <= exactTimeStamp

    // todo now
    fun current(exactTimeStamp: ExactTimeStamp.Local) = afterStart(exactTimeStamp) && notDeleted(exactTimeStamp)

    fun requireCurrent(exactTimeStamp: ExactTimeStamp.Local) {
        if (!current(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    fun requireNotCurrent(exactTimeStamp: ExactTimeStamp.Local) {
        if (current(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    override fun throwTime(exactTimeStamp: ExactTimeStamp.Local?): Nothing = throw Endable.TimeException(
        "$this exactTimeStamps start: $startExactTimeStamp, end: $endExactTimeStamp, time: $exactTimeStamp"
    )
}