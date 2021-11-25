package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.FeatureFlagManager
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.MonthlyDayScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateSoy
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth
import com.soywiz.klock.months
import com.soywiz.klock.plus

class MonthlyDaySchedule(topLevelTask: Task, override val repeatingScheduleRecord: MonthlyDayScheduleRecord) :
        RepeatingSchedule(topLevelTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val dayOfMonth get() = repeatingScheduleRecord.dayOfMonth

    val beginningOfMonth get() = repeatingScheduleRecord.beginningOfMonth

    override val scheduleType = ScheduleType.MONTHLY_DAY

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = YearlySchedule.ProxyDateTimeSequenceGenerator(
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

    private inner class MonthlyDayNewDateTimeSequenceGenerator : YearlySchedule.NewDateTimeSequenceGenerator() {

        override fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy {
            val startDate = startDateSoy.toDate()

            if (containsDate(startDate)) { // todo sequence optimize
                return startDateSoy
            } else {
                val dateSameMonth = getDateInMonth(startDate.year, startDate.month)

                val finalDate = when {
                    dateSameMonth < startDate -> {
                        val nextMonthDateSoy = startDateSoy + 1.months
                        val nextMonthDate = nextMonthDateSoy.toDate()

                        getDateInMonth(nextMonthDate.year, nextMonthDate.month)
                    }
                    dateSameMonth > startDate -> dateSameMonth
                    else -> throw IllegalStateException() // todo sequence redundant with first check
                }

                return finalDate.toDateSoy()
            }
        }

        override fun containsDate(date: Date) = this@MonthlyDaySchedule.containsDate(date)

        override fun getScheduleTime() = time
    }
}
