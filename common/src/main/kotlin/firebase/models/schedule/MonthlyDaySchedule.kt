package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.MonthlyDayScheduleRecord
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateSoyInMonth
import firebase.models.schedule.generators.DateTimeSequenceGenerator
import firebase.models.schedule.generators.MonthlyNextValidDateTimeSequenceGenerator

class MonthlyDaySchedule(topLevelTask: Task, override val repeatingScheduleRecord: MonthlyDayScheduleRecord) :
        RepeatingSchedule(topLevelTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val dayOfMonth get() = repeatingScheduleRecord.dayOfMonth

    val beginningOfMonth get() = repeatingScheduleRecord.beginningOfMonth

    override val scheduleType = ScheduleType.MONTHLY_DAY

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = MonthlyDayNextValidDateTimeSequenceGenerator()

    fun getDateSoyInMonth(year: Int, month: Int) = getDateSoyInMonth(
        year,
        month,
        repeatingScheduleRecord.dayOfMonth,
        repeatingScheduleRecord.beginningOfMonth,
    )

    private inner class MonthlyDayNextValidDateTimeSequenceGenerator : MonthlyNextValidDateTimeSequenceGenerator() {

        override fun getDateSoyInMonth(year: Int, month: Int) = this@MonthlyDaySchedule.getDateSoyInMonth(year, month)
    }
}
