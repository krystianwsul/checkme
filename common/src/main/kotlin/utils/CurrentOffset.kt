package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

interface CurrentOffset {

    val startExactTimeStampOffset: ExactTimeStamp
    val endExactTimeStampOffset: ExactTimeStamp?

    fun notDeletedOffset(exactTimeStamp: ExactTimeStamp? = null): Boolean {
        return if (exactTimeStamp != null)
            endExactTimeStampOffset?.let { it > exactTimeStamp } != false
        else
            endExactTimeStampOffset == null
    }

    fun afterStartOffset(exactTimeStamp: ExactTimeStamp) = startExactTimeStampOffset <= exactTimeStamp

    fun currentOffset(exactTimeStamp: ExactTimeStamp) = afterStartOffset(exactTimeStamp) && notDeletedOffset(exactTimeStamp)

    fun requireNotDeletedOffset(exactTimeStamp: ExactTimeStamp? = null) {
        if (!notDeletedOffset(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    fun requireCurrentOffset(exactTimeStamp: ExactTimeStamp) {
        if (!currentOffset(exactTimeStamp)) throwTime(exactTimeStamp)
    }

    private fun throwTime(exactTimeStamp: ExactTimeStamp?): Nothing = throw Endable.TimeException(
        "$this offsets start: $startExactTimeStampOffset, end: $endExactTimeStampOffset, time: $exactTimeStamp"
    )
}