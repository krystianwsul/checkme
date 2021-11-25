package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.FeatureFlagManager
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
import firebase.models.schedule.generators.NewDateTimeSequenceGenerator
import firebase.models.schedule.generators.ProxyDateTimeSequenceGenerator

class WeeklySchedule(topLevelTask: Task, override val repeatingScheduleRecord: WeeklyScheduleRecord) :
    RepeatingSchedule(topLevelTask) {

    val dayOfWeek = DayOfWeek.values()[repeatingScheduleRecord.dayOfWeek]

    override val scheduleRecord get() = repeatingScheduleRecord

    override val scheduleType = ScheduleType.WEEKLY

    val interval = repeatingScheduleRecord.interval

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = ProxyDateTimeSequenceGenerator(
        WeeklyDateTimeSequenceGenerator(),
        WeeklyNewDateTimeSequenceGenerator(),
        FeatureFlagManager.Flag.NEW_WEEKLY_SCHEDULE,
    )

    /*
    todo sequence optimize: I think all the related methods can benefit from a SoyDayOfWeek - DayOfWeek mapping
     */
    fun containsDate(date: Date): Boolean { // todo sequence optimize
        val day = date.dayOfWeek

        if (dayOfWeek != day) return false

        if (interval != 1) {
            val timeSpan = date.toDateSoy().dateTimeDayStart - from!!.toDateSoy().dateTimeDayStart
            if (timeSpan.weeks.toInt().rem(interval) != 0) return false
        }

        return true
    }

    private inner class WeeklyDateTimeSequenceGenerator : DailyDateTimeSequenceGenerator() {

        override fun containsDate(date: Date) = this@WeeklySchedule.containsDate(date)
    }

    private inner class WeeklyNewDateTimeSequenceGenerator : NewDateTimeSequenceGenerator() {

        override fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy {
            val startDate = startDateSoy.toDate() // todo sequence optimize find way to check day of week for Soy

            val startDayOfWeek = startDate.dayOfWeek

            val dayOfWeekDiff = dayOfWeek.ordinal - startDayOfWeek.ordinal

            val newStartDateSoy = when {
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
                else -> startDateSoy // todo sequence doubles above
            }

            val newStartDate = newStartDateSoy.toDate()
            check(newStartDate.dayOfWeek == dayOfWeek) // todo sequence

            return if (containsDate(newStartDate)) {
                newStartDateSoy
            } else {
                check(interval != 1)

                val timeSpan = newStartDate.toDateSoy().dateTimeDayStart - from!!.toDateSoy().dateTimeDayStart
                val remainder = timeSpan.weeks.toInt().rem(interval)

                val newestStartDateSoy = newStartDateSoy + remainder.weeks
                val newestStartDate = newestStartDateSoy.toDate()
                check(containsDate(newestStartDate)) // todo sequence optimize

                newestStartDateSoy
            }
        }

        override fun containsDate(date: Date) = this@WeeklySchedule.containsDate(date)

        override fun getScheduleTime() = time
    }
}
