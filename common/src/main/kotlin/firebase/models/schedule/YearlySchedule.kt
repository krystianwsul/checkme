package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.YearlyScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ScheduleType

class YearlySchedule(topLevelTask: Task, override val repeatingScheduleRecord: YearlyScheduleRecord) :
        RepeatingSchedule(topLevelTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val month get() = repeatingScheduleRecord.month
    val day get() = repeatingScheduleRecord.day

    override val scheduleType = ScheduleType.YEARLY

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = YearlyDateTimeSequenceGenerator()

    fun getDateInYear(year: Int) = Date(year, month, day)

    private inner class YearlyDateTimeSequenceGenerator : DateTimeSequenceGenerator() {

        override fun containsDate(date: Date): Boolean {
            val dateThisYear = getDateInYear(date.year)

            return dateThisYear == date
        }
    }
}
