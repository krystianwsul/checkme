package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.schedule.MonthlyDayScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth

class MonthlyDaySchedule<T : ProjectType>(
        rootTask: Task<T>,
        override val repeatingScheduleRecord: MonthlyDayScheduleRecord<T>,
) : RepeatingSchedule<T>(rootTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val dayOfMonth get() = repeatingScheduleRecord.dayOfMonth

    val beginningOfMonth get() = repeatingScheduleRecord.beginningOfMonth

    override val scheduleType = ScheduleType.MONTHLY_DAY

    override fun containsDate(date: Date): Boolean {
        val dateThisMonth = getDateInMonth(date.year, date.month)

        return dateThisMonth == date
    }

    fun getDateInMonth(year: Int, month: Int) = getDateInMonth(
            year,
            month,
            repeatingScheduleRecord.dayOfMonth,
            repeatingScheduleRecord.beginningOfMonth,
    )
}
