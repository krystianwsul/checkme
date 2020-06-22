package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.WeeklyScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMilli
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType

class WeeklySchedule<T : ProjectType>(
        rootTask: Task<T>,
        override val repeatingScheduleRecord: WeeklyScheduleRecord<T>
) : RepeatingSchedule<T>(rootTask) {

    val dayOfWeek = DayOfWeek.values()[repeatingScheduleRecord.dayOfWeek]

    override val scheduleRecord get() = repeatingScheduleRecord

    override val scheduleType = ScheduleType.WEEKLY

    val interval = repeatingScheduleRecord.interval

    override fun <T : ProjectType> getInstanceInDate(
            task: Task<T>,
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?
    ): Instance<T>? {
        val day = date.dayOfWeek

        if (dayOfWeek != day)
            return null

        if (interval != 1) {
            val timeSpan = date.toDateTimeTz() - from!!.toDateTimeTz()
            if (timeSpan.weeks.toInt().rem(interval) != 0)
                return null
        }

        val hourMinute = time.getHourMinute(day)

        if (startHourMilli != null && startHourMilli > hourMinute.toHourMilli())
            return null

        if (endHourMilli != null && endHourMilli <= hourMinute.toHourMilli())
            return null

        val scheduleDateTime = DateTime(date, time)
        task.requireCurrent(scheduleDateTime.timeStamp.toExactTimeStamp())

        return task.getInstance(scheduleDateTime)
    }

    override fun matchesScheduleDateTimeRepeatingHelper(scheduleDateTime: DateTime) =
            scheduleDateTime.date.dayOfWeek == dayOfWeek
}
