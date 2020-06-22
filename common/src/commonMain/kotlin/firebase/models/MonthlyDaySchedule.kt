package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.MonthlyDayScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.HourMilli
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth

class MonthlyDaySchedule<T : ProjectType>(
        rootTask: Task<T>,
        override val repeatingScheduleRecord: MonthlyDayScheduleRecord<T>
) : RepeatingSchedule<T>(rootTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val dayOfMonth get() = repeatingScheduleRecord.dayOfMonth

    val beginningOfMonth get() = repeatingScheduleRecord.beginningOfMonth

    override val scheduleType = ScheduleType.MONTHLY_DAY

    override fun <T : ProjectType> getInstanceInDate(
            task: Task<T>,
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?
    ): Instance<T>? {
        val dateThisMonth = getDate(date.year, date.month)

        if (dateThisMonth != date)
            return null

        val hourMinute = time.getHourMinute(date.dayOfWeek)

        if (startHourMilli != null && startHourMilli > hourMinute.toHourMilli())
            return null

        if (endHourMilli != null && endHourMilli <= hourMinute.toHourMilli())
            return null

        val scheduleDateTime = DateTime(date, time)
        task.requireCurrent(scheduleDateTime.timeStamp.toExactTimeStamp())

        return task.getInstance(scheduleDateTime)
    }

    private fun getDate(year: Int, month: Int) = getDateInMonth(
            year,
            month,
            repeatingScheduleRecord.dayOfMonth,
            repeatingScheduleRecord.beginningOfMonth
    )

    override fun matchesScheduleDateTimeRepeatingHelper(scheduleDateTime: DateTime): Boolean {
        val date = scheduleDateTime.date.run { getDate(year, month) }

        return date == scheduleDateTime.date
    }
}
