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

    fun getDateSoyInYear(year: Int) = DateSoy(year, month, day)

    private inner class YearlyNextValidDateTimeSequenceGenerator : NextValidDateTimeSequenceGenerator() {

        override fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy {
            val startDate = startDateSoy.toDate()
            val dateThisYear = getDateInYear(startDate.year)

            return when (val comparison = dateThisYear.compareTo(startDate)) {
                1 -> dateThisYear.toDateSoy()
                -1 -> getDateInYear(startDate.year + 1).toDateSoy()
                else -> {
                    check(comparison == 0) // todo sequence checks

                    startDateSoy
                }
            }
        }

        override fun containsDateSoy(dateSoy: DateSoy) = dateSoy == getDateSoyInYear(dateSoy.year)
    }
}
