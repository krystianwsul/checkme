package com.krystianwsul.common.domain.schedules


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.records.YearlyScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType
import com.soywiz.klock.years

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

    override fun getNextAlarm(now: ExactTimeStamp): TimeStamp? {
        val dateThisYear = now.date.run { getDate(year) }
        val thisMonth = DateTime(dateThisYear, time)

        val endExactTimeStamp = endExactTimeStamp

        val checkMonth = if (thisMonth.toExactTimeStamp() > now) {
            thisMonth
        } else {
            DateTime(Date(now.toDateTimeTz() + 1.years), time)
        }.timeStamp

        return if (endExactTimeStamp?.let { it <= checkMonth.toExactTimeStamp() } != false)
            null
        else
            checkMonth
    }

    private fun getDate(year: Int) = Date(year, month, day)
}
