package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.YearlyScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleType
import firebase.models.schedule.generators.DateTimeSequenceGenerator
import firebase.models.schedule.generators.DateTimeSequenceGenerator.Companion.toDate
import firebase.models.schedule.generators.NextValidDateTimeSequenceGenerator

class YearlySchedule(topLevelTask: Task, override val repeatingScheduleRecord: YearlyScheduleRecord) :
        RepeatingSchedule(topLevelTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val month get() = repeatingScheduleRecord.month
    val day get() = repeatingScheduleRecord.day

    override val scheduleType = ScheduleType.YEARLY

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = YearlyNextValidDateTimeSequenceGenerator()

    fun getDateInYear(year: Int) = Date(year, month, day)

    private fun containsDate(date: Date): Boolean {
        val dateThisYear = getDateInYear(date.year)

        return dateThisYear == date
    }

    private inner class YearlyNextValidDateTimeSequenceGenerator : NextValidDateTimeSequenceGenerator() {

        override fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy {
            val date = startDateSoy.toDate()

            return if (containsDate(date)) {
                startDateSoy
            } else {
                getDateInYear(date.year + 1).toDateSoy()
            }
        }

        override fun containsDate(date: Date) = this@YearlySchedule.containsDate(date)
    }
}
