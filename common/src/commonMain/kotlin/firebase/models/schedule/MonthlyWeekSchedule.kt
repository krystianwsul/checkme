package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.records.schedule.MonthlyWeekScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth

class MonthlyWeekSchedule(rootTask: Task, override val repeatingScheduleRecord: MonthlyWeekScheduleRecord) :
        RepeatingSchedule(rootTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val weekOfMonth get() = repeatingScheduleRecord.weekOfMonth

    val dayOfWeek get() = DayOfWeek.values()[repeatingScheduleRecord.dayOfWeek]

    val beginningOfMonth get() = repeatingScheduleRecord.beginningOfMonth

    override val scheduleType get() = ScheduleType.MONTHLY_WEEK

    override fun containsDate(date: Date): Boolean {
        val dateThisMonth = getDateInMonth(date.year, date.month)

        return dateThisMonth == date
    }

    private fun getDateInMonth(year: Int, month: Int) = getDateInMonth(
            year,
            month,
            repeatingScheduleRecord.weekOfMonth,
            dayOfWeek,
            repeatingScheduleRecord.beginningOfMonth
    )
}
