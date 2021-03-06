package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.domain.Instance
import com.krystianwsul.common.domain.Task
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleType
import com.soywiz.klock.days

class WeeklySchedule(
        rootTask: RemoteTask<*>,
        override val repeatingScheduleBridge: WeeklyScheduleBridge
) : RepeatingSchedule(rootTask) {

    val daysOfWeek
        get() = repeatingScheduleBridge.daysOfWeek
                .asSequence()
                .map { DayOfWeek.values()[it] }
                .toSet()

    override val scheduleBridge get() = repeatingScheduleBridge

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

        return task.getInstance(scheduleDateTime)
    }

    override fun getNextAlarm(now: ExactTimeStamp): TimeStamp {
        val today = Date.today()

        val nowDayOfWeek = today.dayOfWeek
        val nowHourMinute = HourMinute(now.toDateTimeTz())

        val nextDayOfWeek = daysOfWeek.sorted().run {
            if (time.getHourMinute(nowDayOfWeek) > nowHourMinute) {
                firstOrNull { it >= nowDayOfWeek }
            } else {
                firstOrNull { it > nowDayOfWeek }
            } ?: first()
        }

        val ordinalDifference = nextDayOfWeek.ordinal - nowDayOfWeek.ordinal
        val addDays = if (ordinalDifference == 0 && time.getHourMinute(nowDayOfWeek) > nowHourMinute)
            0
        else if (ordinalDifference > 0)
            ordinalDifference
        else
            ordinalDifference + 7

        val thisDate = Date(today.toDateTimeTz() + addDays.days)

        return DateTime(thisDate, time).timeStamp
    }

    override val scheduleType = ScheduleType.WEEKLY
}
