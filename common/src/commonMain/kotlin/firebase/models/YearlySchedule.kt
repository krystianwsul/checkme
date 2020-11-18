package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.schedule.YearlyScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType

class YearlySchedule<T : ProjectType>(
        rootTask: Task<T>,
        override val repeatingScheduleRecord: YearlyScheduleRecord<T>,
) : RepeatingSchedule<T>(rootTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val month get() = repeatingScheduleRecord.month
    val day get() = repeatingScheduleRecord.day

    override val scheduleType = ScheduleType.YEARLY

    override fun containsDate(date: Date): Boolean {
        val dateThisYear = getDateInYear(date.year)

        return dateThisYear == date
    }

    private fun getDateInYear(year: Int) = Date(year, month, day)

    override fun matchesScheduleDateRepeatingHelper(scheduleDate: Date): Boolean {
        val date = getDateInYear(scheduleDate.year)

        return date == scheduleDate
    }
}
