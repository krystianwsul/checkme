package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.WeeklyScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateSoy
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.ScheduleType
import com.soywiz.klock.days
import com.soywiz.klock.plus
import com.soywiz.klock.weeks
import firebase.models.schedule.generators.DateTimeSequenceGenerator
import firebase.models.schedule.generators.DateTimeSequenceGenerator.Companion.toDate
import firebase.models.schedule.generators.NextValidDateTimeSequenceGenerator

class WeeklySchedule(topLevelTask: Task, override val repeatingScheduleRecord: WeeklyScheduleRecord) :
    RepeatingSchedule(topLevelTask) {

    val dayOfWeek = DayOfWeek.values()[repeatingScheduleRecord.dayOfWeek]

    override val scheduleRecord get() = repeatingScheduleRecord

    override val scheduleType = ScheduleType.WEEKLY

    val interval = repeatingScheduleRecord.interval

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = WeeklyNextValidDateTimeSequenceGenerator()

    private inner class WeeklyNextValidDateTimeSequenceGenerator : NextValidDateTimeSequenceGenerator() {

        override fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy {
            val dayOfWeekDiff = dayOfWeek.ordinal - startDateSoy.dayOfWeekInt

            val fixedDayDateSoy = when {
                dayOfWeekDiff > 0 -> {
                    /*
                    For example, the schedule date is Wednesday 4. The start date is Monday 2.  diff = 2
                     */
                    startDateSoy + dayOfWeekDiff.days
                }
                dayOfWeekDiff < 0 -> {
                    /*
                    For example, the schedule date is Monday 2. The start date is Wednesday 4.  diff = -2
                     */
                    startDateSoy + (7 + dayOfWeekDiff).days
                }
                else -> startDateSoy
            }

            check(fixedDayDateSoy.dayOfWeek.ordinal == dayOfWeek.ordinal) // todo sequence checks

            return if (interval == 1) {
                containsDateSoy(fixedDayDateSoy) // todo sequence checks

                fixedDayDateSoy
            } else {
                val timeSpan = fixedDayDateSoy.dateTimeDayStart - from!!.toDateSoy().dateTimeDayStart
                val remainder = timeSpan.weeks.toInt().rem(interval)

                if (remainder == 0) {
                    check(containsDateSoy(fixedDayDateSoy)) // todo sequence checks

                    fixedDayDateSoy
                } else {
                    val fixedWeekDateSoy = fixedDayDateSoy + remainder.weeks
                    val newestStartDate = fixedWeekDateSoy.toDate() // todo sequence optimize
                    check(containsDate(newestStartDate)) // todo sequence checks

                    fixedWeekDateSoy
                }
            }
        }

        override fun containsDate(date: Date): Boolean { // todo sequence optimize
            if (dayOfWeek != date.dayOfWeek) return false

            if (interval != 1) {
                val timeSpan = date.toDateSoy().dateTimeDayStart - from!!.toDateSoy().dateTimeDayStart
                if (timeSpan.weeks.toInt().rem(interval) != 0) return false
            }

            return true
        }

        override fun containsDateSoy(dateSoy: DateSoy): Boolean { // todo sequence optimize
            if (dayOfWeek.ordinal != dateSoy.dayOfWeek.ordinal) return false

            if (interval != 1) {
                val timeSpan = dateSoy.dateTimeDayStart - from!!.toDateSoy().dateTimeDayStart
                if (timeSpan.weeks.toInt().rem(interval) != 0) return false
            }

            return true
        }
    }
}
