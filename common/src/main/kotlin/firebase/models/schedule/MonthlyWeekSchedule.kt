package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.FeatureFlagManager
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.MonthlyWeekScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth
import firebase.models.schedule.generators.DateTimeSequenceGenerator
import firebase.models.schedule.generators.MonthlyNewDateTimeSequenceGenerator
import firebase.models.schedule.generators.ProxyDateTimeSequenceGenerator

class MonthlyWeekSchedule(topLevelTask: Task, override val repeatingScheduleRecord: MonthlyWeekScheduleRecord) :
        RepeatingSchedule(topLevelTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val weekOfMonth get() = repeatingScheduleRecord.weekOfMonth

    val dayOfWeek get() = DayOfWeek.values()[repeatingScheduleRecord.dayOfWeek]

    val beginningOfMonth get() = repeatingScheduleRecord.beginningOfMonth

    override val scheduleType get() = ScheduleType.MONTHLY_WEEK

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = ProxyDateTimeSequenceGenerator(
        MonthlyWeekDateTimeSequenceGenerator(),
        MonthlyWeekNewDateTimeSequenceGenerator(),
        FeatureFlagManager.Flag.NEW_MONTHLY_WEEK_SCHEDULE,
    )

    private fun getDateInMonth(year: Int, month: Int) = getDateInMonth(
        year,
        month,
        repeatingScheduleRecord.weekOfMonth,
        dayOfWeek,
        repeatingScheduleRecord.beginningOfMonth,
    )

    private fun containsDate(date: Date): Boolean {
        val dateThisMonth = getDateInMonth(date.year, date.month)

        return dateThisMonth == date
    }

    private inner class MonthlyWeekDateTimeSequenceGenerator : DailyDateTimeSequenceGenerator() {

        override fun containsDate(date: Date) = this@MonthlyWeekSchedule.containsDate(date)
    }

    private inner class MonthlyWeekNewDateTimeSequenceGenerator : MonthlyNewDateTimeSequenceGenerator() {

        override fun getDateInMonth(year: Int, month: Int) = this@MonthlyWeekSchedule.getDateInMonth(year, month)

        override fun containsDate(date: Date) = this@MonthlyWeekSchedule.containsDate(date)

        override fun getScheduleTime() = time
    }
}
