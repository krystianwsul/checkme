package com.krystianwsul.checkme.domainmodel

import android.content.Context
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import junit.framework.Assert
import java.util.*

class WeeklySchedule(domainFactory: DomainFactory, private val mWeeklyScheduleBridge: WeeklyScheduleBridge) : RepeatingSchedule(domainFactory) {

    private val time get() = mWeeklyScheduleBridge.run { customTimeKey?.let { mDomainFactory.getCustomTime(it) } ?: NormalTime(hour!!, minute!!) }

    val timePair get() = mWeeklyScheduleBridge.run { customTimeKey?.let { TimePair(it) } ?: TimePair(HourMinute(hour!!, minute!!)) }

    val dayOfWeek get() = DayOfWeek.values()[mWeeklyScheduleBridge.dayOfWeek]

    override fun getScheduleBridge() = mWeeklyScheduleBridge

    override fun getScheduleText(context: Context) = dayOfWeek.toString() + ": " + time

    override fun getInstanceInDate(task: Task, date: Date, startHourMilli: HourMilli?, endHourMilli: HourMilli?): Instance? {
        val day = date.dayOfWeek

        if (dayOfWeek != day)
            return null

        val hourMinute = time.getHourMinute(day)

        if (startHourMilli != null && startHourMilli > hourMinute.toHourMilli())
            return null

        if (endHourMilli != null && endHourMilli <= hourMinute.toHourMilli())
            return null

        val scheduleDateTime = DateTime(date, time)
        Assert.assertTrue(task.current(scheduleDateTime.timeStamp.toExactTimeStamp()))

        return mDomainFactory.getInstance(task.taskKey, scheduleDateTime)
    }

    override fun getNextAlarm(now: ExactTimeStamp): TimeStamp {
        val today = Date.today()

        val dayOfWeek = today.dayOfWeek
        val nowHourMinute = HourMinute(now.calendar)

        val ordinalDifference = dayOfWeek.ordinal - dayOfWeek.ordinal
        val thisCalendar = today.calendar
        if (ordinalDifference > 0 || ordinalDifference == 0 && time.getHourMinute(dayOfWeek) > nowHourMinute)
            thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference)
        else
            thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference + 7)
        val thisDate = Date(thisCalendar)

        return DateTime(thisDate, time).timeStamp
    }

    override fun getCustomTimeKey() = mWeeklyScheduleBridge.customTimeKey

    override fun getScheduleType() = ScheduleType.WEEKLY
}
