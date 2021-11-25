package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.FeatureFlagManager
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.MonthlyDayScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth
import firebase.models.schedule.generators.DateTimeSequenceGenerator
import firebase.models.schedule.generators.MonthlyNewDateTimeSequenceGenerator
import firebase.models.schedule.generators.ProxyDateTimeSequenceGenerator

class MonthlyDaySchedule(topLevelTask: Task, override val repeatingScheduleRecord: MonthlyDayScheduleRecord) :
        RepeatingSchedule(topLevelTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val dayOfMonth get() = repeatingScheduleRecord.dayOfMonth

    val beginningOfMonth get() = repeatingScheduleRecord.beginningOfMonth

    override val scheduleType = ScheduleType.MONTHLY_DAY

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = ProxyDateTimeSequenceGenerator(
        MonthlyDayDateTimeSequenceGenerator(),
        MonthlyDayNewDateTimeSequenceGenerator(),
        FeatureFlagManager.Flag.NEW_MONTHLY_DAY_SCHEDULE,
    )

    fun getDateInMonth(year: Int, month: Int) = getDateInMonth(
        year,
        month,
        repeatingScheduleRecord.dayOfMonth,
        repeatingScheduleRecord.beginningOfMonth,
    )

    private fun containsDate(date: Date): Boolean {
        val dateThisMonth = getDateInMonth(date.year, date.month)

        return dateThisMonth == date
    }

    private inner class MonthlyDayDateTimeSequenceGenerator : DailyDateTimeSequenceGenerator() {

        override fun containsDate(date: Date) = this@MonthlyDaySchedule.containsDate(date)
    }

    private inner class MonthlyDayNewDateTimeSequenceGenerator : MonthlyNewDateTimeSequenceGenerator() {

        override fun getDateInMonth(year: Int, month: Int) = this@MonthlyDaySchedule.getDateInMonth(year, month)

        override fun containsDate(date: Date) = this@MonthlyDaySchedule.containsDate(date)

        override fun getScheduleTime() = time
    }
}
