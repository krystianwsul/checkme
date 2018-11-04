package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import java.util.*

class WeeklySchedule(
        domainFactory: DomainFactory,
        private val mWeeklyScheduleBridge: WeeklyScheduleBridge) : RepeatingSchedule(domainFactory) {

    val daysOfWeek
        get() = mWeeklyScheduleBridge.daysOfWeek
                .asSequence()
                .map { DayOfWeek.values()[it] }
                .toSet()

    override val scheduleBridge get() = mWeeklyScheduleBridge

    // todo single tostring
    override fun getScheduleText() = daysOfWeek.joinToString(", ") + ": " + time

    override fun getInstanceInDate(task: Task, date: Date, startHourMilli: HourMilli?, endHourMilli: HourMilli?): Instance? {
        val day = date.dayOfWeek

        if (!daysOfWeek.contains(day))
            return null

        val hourMinute = time.getHourMinute(day)

        if (startHourMilli != null && startHourMilli > hourMinute.toHourMilli())
            return null

        if (endHourMilli != null && endHourMilli <= hourMinute.toHourMilli())
            return null

        val scheduleDateTime = DateTime(date, time)
        check(task.current(scheduleDateTime.timeStamp.toExactTimeStamp()))

        return domainFactory.getInstance(task.taskKey, scheduleDateTime)
    }

    override fun getNextAlarm(now: ExactTimeStamp): TimeStamp {
        val today = Date.today()

        val nowDayOfWeek = today.dayOfWeek
        val nowHourMinute = HourMinute(now.calendar)

        val nextDayOfWeek = daysOfWeek.sorted().run {
            if (time.getHourMinute(nowDayOfWeek) > nowHourMinute) {
                firstOrNull { it >= nowDayOfWeek }
            } else {
                firstOrNull { it > nowDayOfWeek }
            } ?: first()
        }

        val ordinalDifference = nextDayOfWeek.ordinal - nowDayOfWeek.ordinal
        val thisCalendar = today.calendar
        if (ordinalDifference > 0 || ordinalDifference == 0 && time.getHourMinute(nowDayOfWeek) > nowHourMinute)
            thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference)
        else
            thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference + 7)
        val thisDate = Date(thisCalendar)

        return DateTime(thisDate, time).timeStamp
    }

    override val scheduleType = ScheduleType.WEEKLY
}
