package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.records.schedule.YearlyScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType

class YearlySchedule<T : ProjectType>(
        rootTask: Task<T>,
        override val repeatingScheduleRecord: YearlyScheduleRecord,
) : RepeatingSchedule<T>(rootTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val month get() = repeatingScheduleRecord.month
    val day get() = repeatingScheduleRecord.day

    override val scheduleType = ScheduleType.YEARLY

    override fun containsDate(date: Date): Boolean {
        val dateThisYear = getDateInYear(date.year)

        return dateThisYear == date
    }

    fun getDateInYear(year: Int) = Date(year, month, day)
}
