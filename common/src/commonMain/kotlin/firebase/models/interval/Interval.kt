package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CurrentOffset

sealed class Interval : CurrentOffset {

    abstract val type: Type

    abstract override val startExactTimeStampOffset: ExactTimeStamp.Offset
    abstract override val endExactTimeStampOffset: ExactTimeStamp.Offset?

    open fun containsExactTimeStamp(exactTimeStamp: ExactTimeStamp) = startExactTimeStampOffset <= exactTimeStamp

    data class Current(
            override val type: Type,
            override val startExactTimeStampOffset: ExactTimeStamp.Offset,
    ) : Interval() {

        override val endExactTimeStampOffset: ExactTimeStamp.Offset? = null
    }

    data class Ended(
            override val type: Type,
            override val startExactTimeStampOffset: ExactTimeStamp.Offset,
            override val endExactTimeStampOffset: ExactTimeStamp.Offset,
    ) : Interval() {

        override fun containsExactTimeStamp(exactTimeStamp: ExactTimeStamp): Boolean {
            if (!super.containsExactTimeStamp(exactTimeStamp)) return false

            return endExactTimeStampOffset > exactTimeStamp
        }

        fun correctEndExactTimeStamps() {
            type.taskParentEntries.forEach {
                if (it.endExactTimeStampOffset?.let { it > endExactTimeStampOffset } != false) {
                    try {
                        it.setEndExactTimeStamp(endExactTimeStampOffset)
                    } catch (exception: Exception) {
                        throw Exception("error setting endExactTimeStamp for $it", exception)
                    }
                }
            }
        }
    }
}