package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.WeeklyScheduleRecord
import com.krystianwsul.common.time.DateSoy
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.ScheduleType
import com.soywiz.klock.days
import com.soywiz.klock.plus
import com.soywiz.klock.weeks
import firebase.models.schedule.generators.DateTimeSequenceGenerator
import firebase.models.schedule.generators.NextValidDateTimeSequenceGenerator

class WeeklySchedule(topLevelTask: Task, override val repeatingScheduleRecord: WeeklyScheduleRecord) :
    RepeatingSchedule(topLevelTask) {

    val dayOfWeek = DayOfWeek.values()[repeatingScheduleRecord.dayOfWeek]

    override val scheduleRecord get() = repeatingScheduleRecord

    override val scheduleType = ScheduleType.WEEKLY

    val interval = repeatingScheduleRecord.interval

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = WeeklyNextValidDateTimeSequenceGenerator()

    private inner class WeeklyNextValidDateTimeSequenceGenerator : NextValidDateTimeSequenceGenerator() {

        override fun getFirstDateSoy(startDateSoy: DateSoy): DateSoy {
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

            return if (interval == 1) {
                fixedDayDateSoy
            } else {
                val timeSpan = fixedDayDateSoy.dateTimeDayStart - from!!.toDateTimeSoy()
                val remainder = timeSpan.weeks.toInt().rem(interval)

                if (remainder == 0)
                    fixedDayDateSoy
                else
                    fixedDayDateSoy + (interval - remainder).weeks
            }
        }

        override fun getNextDateSoy(currentDateSoy: DateSoy) = currentDateSoy + interval.weeks
    }
}
