package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.YearlyScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateSoy
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

    private inner class YearlyNextValidDateTimeSequenceGenerator : NextValidDateTimeSequenceGenerator() {

        override fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy {
            val date = startDateSoy.toDate()

            return if (containsDate(date)) {
                startDateSoy
            } else {
                val dateThisYear = getDateInYear(date.year)

                if (dateThisYear > date) { // todo sequence redundant with containsDate
                    dateThisYear.toDateSoy()
                } else {
                    getDateInYear(date.year + 1).toDateSoy()
                }
            }
        }

        override fun containsDate(date: Date) = date == getDateInYear(date.year)
    }
}
