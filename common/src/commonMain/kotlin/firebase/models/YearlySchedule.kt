package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.YearlyScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.HourMilli
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType

class YearlySchedule<T : ProjectType>(
        rootTask: Task<T>,
        override val repeatingScheduleRecord: YearlyScheduleRecord<T>
) : RepeatingSchedule<T>(rootTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val month get() = repeatingScheduleRecord.month
    val day get() = repeatingScheduleRecord.day

    override val scheduleType = ScheduleType.YEARLY

    override fun <T : ProjectType> getInstanceInDate(
            task: Task<T>,
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?
    ): Instance<T>? {
        val dateThisYear = getDate(date.year)

        if (dateThisYear != date)
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

    private fun getDate(year: Int) = Date(year, month, day)

    override fun matchesScheduleDateTimeRepeatingHelper(scheduleDateTime: DateTime): Boolean {
        val date = getDate(scheduleDateTime.date.year)

        return date == scheduleDateTime.date
    }
}
