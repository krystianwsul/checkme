package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.YearlyScheduleRecord
import com.krystianwsul.common.time.DateSoy
import com.krystianwsul.common.utils.ScheduleType
import firebase.models.schedule.generators.DateTimeSequenceGenerator
import firebase.models.schedule.generators.NextValidDateTimeSequenceGenerator

class YearlySchedule(topLevelTask: Task, override val repeatingScheduleRecord: YearlyScheduleRecord) :
        RepeatingSchedule(topLevelTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val month get() = repeatingScheduleRecord.month
    val day get() = repeatingScheduleRecord.day

    override val scheduleType = ScheduleType.YEARLY

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = YearlyNextValidDateTimeSequenceGenerator()

    fun getDateSoyInYear(year: Int) = DateSoy(year, month, day)

    private inner class YearlyNextValidDateTimeSequenceGenerator : NextValidDateTimeSequenceGenerator() {

        override fun getFirstDateSoy(startDateSoy: DateSoy): DateSoy {
            val dateSoyThisYear = getDateSoyInYear(startDateSoy.year)

            return when (dateSoyThisYear.compareTo(startDateSoy)) {
                1 -> dateSoyThisYear
                -1 -> getDateSoyInYear(startDateSoy.year + 1)
                else -> startDateSoy
            }
        }

        override fun getNextDateSoy(currentDateSoy: DateSoy) = getDateSoyInYear(currentDateSoy.year + 1)
    }
}
