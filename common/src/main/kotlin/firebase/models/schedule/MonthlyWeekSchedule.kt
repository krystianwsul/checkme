package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.MonthlyWeekScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateSoy
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth
import firebase.models.schedule.generators.DateTimeSequenceGenerator
import firebase.models.schedule.generators.DateTimeSequenceGenerator.Companion.toDate
import firebase.models.schedule.generators.MonthlyNextValidDateTimeSequenceGenerator

class MonthlyWeekSchedule(topLevelTask: Task, override val repeatingScheduleRecord: MonthlyWeekScheduleRecord) :
        RepeatingSchedule(topLevelTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val weekOfMonth get() = repeatingScheduleRecord.weekOfMonth

    val dayOfWeek get() = DayOfWeek.values()[repeatingScheduleRecord.dayOfWeek]

    val beginningOfMonth get() = repeatingScheduleRecord.beginningOfMonth

    override val scheduleType get() = ScheduleType.MONTHLY_WEEK

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = MonthlyWeekNextValidDateTimeSequenceGenerator()

    private fun getDateInMonth(year: Int, month: Int) = getDateInMonth(
        year,
        month,
        repeatingScheduleRecord.weekOfMonth,
        dayOfWeek,
        repeatingScheduleRecord.beginningOfMonth,
    )

    private inner class MonthlyWeekNextValidDateTimeSequenceGenerator : MonthlyNextValidDateTimeSequenceGenerator() {

        override fun getDateInMonth(year: Int, month: Int) = this@MonthlyWeekSchedule.getDateInMonth(year, month)

        override fun containsDate(date: Date) = date == getDateInMonth(date.year, date.month)

        override fun containsDateSoy(dateSoy: DateSoy) = dateSoy.toDate() == getDateInMonth(dateSoy.year, dateSoy.month1)
    }
}
