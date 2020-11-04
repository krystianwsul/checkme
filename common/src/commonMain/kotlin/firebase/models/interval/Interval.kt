package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CurrentOffset
import com.krystianwsul.common.utils.ProjectType

sealed class Interval<T : ProjectType> : CurrentOffset {

    abstract val type: Type<T>

    abstract override val startExactTimeStampOffset: ExactTimeStamp
    abstract override val endExactTimeStampOffset: ExactTimeStamp?

    open fun containsExactTimeStamp(exactTimeStamp: ExactTimeStamp) = startExactTimeStampOffset <= exactTimeStamp

    data class Current<T : ProjectType>(
            override val type: Type<T>,
            override val startExactTimeStampOffset: ExactTimeStamp
    ) : Interval<T>() {

        override val endExactTimeStampOffset: ExactTimeStamp? = null
    }

    data class Ended<T : ProjectType>(
            override val type: Type<T>,
            override val startExactTimeStampOffset: ExactTimeStamp,
            override val endExactTimeStampOffset: ExactTimeStamp
    ) : Interval<T>() {

        override fun containsExactTimeStamp(exactTimeStamp: ExactTimeStamp): Boolean {
            if (!super.containsExactTimeStamp(exactTimeStamp))
                return false

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