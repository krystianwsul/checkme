package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

interface CurrentOffset {

    val startExactTimeStampOffset: ExactTimeStamp
    val endExactTimeStampOffset: ExactTimeStamp?

    fun notDeletedOffset(exactTimeStamp: ExactTimeStamp) = endExactTimeStampOffset?.let { it > exactTimeStamp } != false

    fun afterStartOffset(exactTimeStamp: ExactTimeStamp) = startExactTimeStampOffset <= exactTimeStamp

    fun currentOffset(exactTimeStamp: ExactTimeStamp) = afterStartOffset(exactTimeStamp) && notDeletedOffset(exactTimeStamp)

    fun requireNotDeletedOffset(exactTimeStamp: ExactTimeStamp) {
        if (!notDeletedOffset(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    fun requireCurrentOffset(exactTimeStamp: ExactTimeStamp) {
        if (!currentOffset(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    fun requireNotCurrentOffset(exactTimeStamp: ExactTimeStamp) {
        if (currentOffset(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    private fun throwTime(exactTimeStamp: ExactTimeStamp): Nothing = throw Current.TimeException(
            "$this offsets start: $startExactTimeStampOffset, end: $endExactTimeStampOffset, time: $exactTimeStamp"
    )
}