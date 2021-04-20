package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.schedule.Schedule
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CurrentOffset
import com.krystianwsul.common.utils.ProjectType

class ScheduleInterval<T : ProjectType>(
        override val startExactTimeStampOffset: ExactTimeStamp.Offset,
        override val endExactTimeStampOffset: ExactTimeStamp.Offset?,
        val schedule: Schedule<T>,
) : CurrentOffset {

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