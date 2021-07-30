package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

interface CurrentOffset {

    val startExactTimeStampOffset: ExactTimeStamp
    val endExactTimeStampOffset: ExactTimeStamp?

    // todo now
    fun notDeletedOffset(exactTimeStamp: ExactTimeStamp) = endExactTimeStampOffset?.let { it > exactTimeStamp } != false

    // todo now
    fun afterStartOffset(exactTimeStamp: ExactTimeStamp) = startExactTimeStampOffset <= exactTimeStamp

    // todo now
    fun currentOffset(exactTimeStamp: ExactTimeStamp) = afterStartOffset(exactTimeStamp) && notDeletedOffset(exactTimeStamp)

    // todo now
    fun requireNotDeletedOffset(exactTimeStamp: ExactTimeStamp) {
        if (!notDeletedOffset(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    // todo now
    fun requireCurrentOffset(exactTimeStamp: ExactTimeStamp) {
        if (!currentOffset(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    // todo now
    fun requireNotCurrentOffset(exactTimeStamp: ExactTimeStamp) {
        if (currentOffset(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    private fun throwTime(exactTimeStamp: ExactTimeStamp): Nothing = throw Endable.TimeException(
            "$this offsets start: $startExactTimeStampOffset, end: $endExactTimeStampOffset, time: $exactTimeStamp"
    )
}