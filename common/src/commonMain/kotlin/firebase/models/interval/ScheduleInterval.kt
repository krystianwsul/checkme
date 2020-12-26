package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.Schedule
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CurrentOffset
import com.krystianwsul.common.utils.ProjectType

class ScheduleInterval<T : ProjectType>(
        override val startExactTimeStampOffset: ExactTimeStamp.Offset,
        override val endExactTimeStampOffset: ExactTimeStamp.Offset?,
        val schedule: Schedule<T>,
) : CurrentOffset {

    fun isUnlimited(): Boolean {
        if (endExactTimeStampOffset != null) return false

        return schedule.isUnlimited()
    }

    fun getDateTimesInRange(
            givenStartExactTimeStamp: ExactTimeStamp.Offset?,
            givenEndExactTimeStamp: ExactTimeStamp.Offset?,
            originalDateTime: Boolean = false,
            checkOldestVisible: Boolean = true,
    ) = schedule.getDateTimesInRange(
            this,
            givenStartExactTimeStamp,
            givenEndExactTimeStamp,
            originalDateTime,
            checkOldestVisible,
    )

    fun updateOldestVisible(now: ExactTimeStamp.Local) = schedule.updateOldestVisible(this, now)
}