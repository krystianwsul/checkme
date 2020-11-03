package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.Schedule
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.ProjectType

class ScheduleInterval<T : ProjectType>(
        override val startExactTimeStamp: ExactTimeStamp,
        override val endExactTimeStamp: ExactTimeStamp?,
        val schedule: Schedule<T>
) : Current {

    fun isVisible(
        task: Task<T>,
        now: ExactTimeStamp,
        hack24: Boolean
    ) = schedule.isVisible(this, task, now, hack24)

    fun getDateTimesInRange(
            givenStartDateTime: DateTime?,
            givenExactDateTime: DateTime?
    ) = schedule.getDateTimesInRange(this, givenStartDateTime, givenExactDateTime)

    fun matchesScheduleDateTime(scheduleDateTime: DateTime, checkOldestVisible: Boolean) =
        schedule.matchesScheduleDateTime(this, scheduleDateTime, checkOldestVisible)

    fun updateOldestVisible(now: ExactTimeStamp) = schedule.updateOldestVisible(this, now)
}