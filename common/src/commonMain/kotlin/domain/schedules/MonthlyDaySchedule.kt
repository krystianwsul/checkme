package com.krystianwsul.common.domain.schedules


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth
import com.soywiz.klock.months

class MonthlyDaySchedule(
        rootTask: Task<*, *>,
        override val repeatingScheduleBridge: MonthlyDayScheduleBridge<*, *>
) : RepeatingSchedule(rootTask) {

    override val scheduleBridge get() = repeatingScheduleBridge

    val dayOfMonth get() = repeatingScheduleBridge.dayOfMonth

    val beginningOfMonth get() = repeatingScheduleBridge.beginningOfMonth

    override val scheduleType = ScheduleType.MONTHLY_DAY

    override fun <T : RemoteCustomTimeId, U : ProjectKey> getInstanceInDate(
            task: Task<T, U>,
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?
    ): Instance<T, U>? {
        val dateThisMonth = getDate(date.year, date.month)

        if (dateThisMonth != date)
            return null

        val hourMinute = time.getHourMinute(date.dayOfWeek)

        if (startHourMilli != null && startHourMilli > hourMinute.toHourMilli())
            return null

        if (endHourMilli != null && endHourMilli <= hourMinute.toHourMilli())
            return null

        val scheduleDateTime = DateTime(date, time)
        check(task.current(scheduleDateTime.timeStamp.toExactTimeStamp()))

        return task.getInstance(scheduleDateTime)
    }

    override fun getNextAlarm(now: ExactTimeStamp): TimeStamp? {
        val dateThisMonth = now.date.run { getDate(year, month) }
        val thisMonth = DateTime(dateThisMonth, time)

        val endExactTimeStamp = getEndExactTimeStamp()

        val checkMonth = if (thisMonth.toExactTimeStamp() > now) {
            thisMonth
        } else {
            DateTime(Date(now.toDateTimeTz() + 1.months), time)
        }.timeStamp

        return if (endExactTimeStamp?.let { it <= checkMonth.toExactTimeStamp() } != false)
            null
        else
            checkMonth
    }

    private fun getDate(year: Int, month: Int) = getDateInMonth(year, month, repeatingScheduleBridge.dayOfMonth, repeatingScheduleBridge.beginningOfMonth)
}
