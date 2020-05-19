package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.WeeklyScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType
import com.soywiz.klock.days
import firebase.models.interval.ScheduleInterval

class WeeklySchedule<T : ProjectType>(
        rootTask: Task<T>,
        override val repeatingScheduleRecord: WeeklyScheduleRecord<T>
) : RepeatingSchedule<T>(rootTask) {

    val dayOfWeek = DayOfWeek.values()[repeatingScheduleRecord.dayOfWeek]

    override val scheduleRecord get() = repeatingScheduleRecord

    override val scheduleType = ScheduleType.WEEKLY

    override fun <T : ProjectType> getInstanceInDate(
            task: Task<T>,
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?
    ): Instance<T>? {
        val day = date.dayOfWeek

        if (dayOfWeek != day)
            return null

        val hourMinute = time.getHourMinute(day)

        if (startHourMilli != null && startHourMilli > hourMinute.toHourMilli())
            return null

        if (endHourMilli != null && endHourMilli <= hourMinute.toHourMilli())
            return null

        val scheduleDateTime = DateTime(date, time)
        task.requireCurrent(scheduleDateTime.timeStamp.toExactTimeStamp())

        return task.getInstance(scheduleDateTime)
    }

    override fun getNextAlarm(
            scheduleInterval: ScheduleInterval<T>,
            now: ExactTimeStamp
    ): TimeStamp {
        val today = Date.today()

        val nowDayOfWeek = today.dayOfWeek
        val nowHourMinute = HourMinute(now.toDateTimeTz())

        val ordinalDifference = dayOfWeek.ordinal - nowDayOfWeek.ordinal
        val addDays = if (ordinalDifference == 0 && time.getHourMinute(nowDayOfWeek) > nowHourMinute)
            0
        else if (ordinalDifference > 0)
            ordinalDifference
        else
            ordinalDifference + 7

        val thisDate = Date(today.toDateTimeTz() + addDays.days)

        return DateTime(thisDate, time).timeStamp
    }

    override fun matchesScheduleDateTimeRepeatingHelper(scheduleDateTime: DateTime) =
            scheduleDateTime.date.dayOfWeek == dayOfWeek
}
