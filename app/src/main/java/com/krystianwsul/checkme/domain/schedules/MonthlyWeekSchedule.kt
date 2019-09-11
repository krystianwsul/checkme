package com.krystianwsul.checkme.domain.schedules

import com.krystianwsul.checkme.domain.Instance
import com.krystianwsul.checkme.domain.Task
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.models.RemoteTask
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import java.util.*

class MonthlyWeekSchedule(
        domainFactory: DomainFactory,
        rootTask: RemoteTask<*>,
        private val monthlyWeekScheduleBridge: MonthlyWeekScheduleBridge
) : RepeatingSchedule(domainFactory, rootTask) {

    override val scheduleBridge get() = monthlyWeekScheduleBridge

    val dayOfMonth get() = monthlyWeekScheduleBridge.dayOfMonth

    val dayOfWeek get() = DayOfWeek.values()[monthlyWeekScheduleBridge.dayOfWeek]

    val beginningOfMonth get() = monthlyWeekScheduleBridge.beginningOfMonth

    override val scheduleType get() = ScheduleType.MONTHLY_WEEK

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

        return getInstance(task, scheduleDateTime)
    }

    override fun getNextAlarm(now: ExactTimeStamp): TimeStamp? {
        val today = now.date

        val dateThisMonth = getDate(today.year, today.month)
        val time = time
        val thisMonth = DateTime(dateThisMonth, time).timeStamp

        val endExactTimeStamp = getEndExactTimeStamp()

        if (thisMonth.toExactTimeStamp() > now) {
            return if (endExactTimeStamp != null && endExactTimeStamp <= thisMonth.toExactTimeStamp())
                null
            else
                thisMonth
        } else {
            val calendar = now.calendar
            calendar.add(Calendar.MONTH, 1)

            val dateNextMonth = Date(calendar.toDateTimeTz())

            val nextMonth = DateTime(dateNextMonth, time).timeStamp

            return if (endExactTimeStamp != null && endExactTimeStamp <= nextMonth.toExactTimeStamp())
                null
            else
                nextMonth
        }
    }

    private fun getDate(year: Int, month: Int) = Utils.getDateInMonth(year, month, monthlyWeekScheduleBridge.dayOfMonth, dayOfWeek, monthlyWeekScheduleBridge.beginningOfMonth)
}
