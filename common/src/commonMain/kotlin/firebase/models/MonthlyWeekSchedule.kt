package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.MonthlyWeekScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth

class MonthlyWeekSchedule<T : ProjectType>(
        rootTask: Task<T>,
        override val repeatingScheduleRecord: MonthlyWeekScheduleRecord<T>
) : RepeatingSchedule<T>(rootTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val dayOfMonth get() = repeatingScheduleRecord.dayOfMonth

    val dayOfWeek get() = DayOfWeek.values()[repeatingScheduleRecord.dayOfWeek]

    val beginningOfMonth get() = repeatingScheduleRecord.beginningOfMonth

    override val scheduleType get() = ScheduleType.MONTHLY_WEEK

    override fun containsDate(date: Date): Boolean {
        val dateThisMonth = getDate(date.year, date.month)

        return dateThisMonth == date
    }

    private fun getDate(year: Int, month: Int) = getDateInMonth(year, month, repeatingScheduleRecord.dayOfMonth, dayOfWeek, repeatingScheduleRecord.beginningOfMonth)

    override fun matchesScheduleDateTimeRepeatingHelper(scheduleDateTime: DateTime): Boolean {
        val date = scheduleDateTime.date.run { getDate(year, month) }

        return date == scheduleDateTime.date
    }
}
