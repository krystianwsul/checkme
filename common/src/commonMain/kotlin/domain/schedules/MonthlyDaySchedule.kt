package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.domain.Instance
import com.krystianwsul.common.domain.Task
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth
import com.soywiz.klock.months

class MonthlyDaySchedule(
        rootTask: RemoteTask<*>,
        private val monthlyDayScheduleBridge: MonthlyDayScheduleBridge
) : RepeatingSchedule(rootTask) {

    override val scheduleBridge get() = monthlyDayScheduleBridge

    val dayOfMonth get() = monthlyDayScheduleBridge.dayOfMonth

    val beginningOfMonth get() = monthlyDayScheduleBridge.beginningOfMonth

    override val scheduleType = ScheduleType.MONTHLY_DAY

    override fun getInstanceInDate(task: Task, date: Date, startHourMilli: HourMilli?, endHourMilli: HourMilli?): Instance? {
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

    private fun getDate(year: Int, month: Int) = getDateInMonth(year, month, monthlyDayScheduleBridge.dayOfMonth, monthlyDayScheduleBridge.beginningOfMonth)
}
