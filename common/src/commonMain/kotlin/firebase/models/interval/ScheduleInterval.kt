package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.Schedule
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CurrentOffset
import com.krystianwsul.common.utils.ProjectType

class ScheduleInterval<T : ProjectType>(
        override val startExactTimeStampOffset: ExactTimeStamp.Offset,
        override val endExactTimeStampOffset: ExactTimeStamp.Offset?,
        val schedule: Schedule<T>,
) : CurrentOffset {

    fun isVisible(
            task: Task<T>,
            now: ExactTimeStamp.Local,
            hack24: Boolean,
    ) = schedule.isVisible(this, task, now, hack24)

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

    fun matchesScheduleDateTime(scheduleDateTime: DateTime, checkOldestVisible: Boolean) =
        schedule.matchesScheduleDateTime(this, scheduleDateTime, checkOldestVisible)

    fun updateOldestVisible(now: ExactTimeStamp.Local) = schedule.updateOldestVisible(this, now)
}