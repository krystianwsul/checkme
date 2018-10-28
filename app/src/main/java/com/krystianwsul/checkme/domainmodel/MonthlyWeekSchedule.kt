package com.krystianwsul.checkme.domainmodel

import android.content.Context
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date

import java.util.*

class MonthlyWeekSchedule(
        kotlinDomainFactory: KotlinDomainFactory,
        private val monthlyWeekScheduleBridge: MonthlyWeekScheduleBridge) : RepeatingSchedule(kotlinDomainFactory) {

    override val scheduleBridge get() = monthlyWeekScheduleBridge

    private val time: Time
        get() {
            val customTimeKey = monthlyWeekScheduleBridge.customTimeKey
            return if (customTimeKey != null) {
                kotlinDomainFactory.getCustomTime(customTimeKey)
            } else {
                val hour = monthlyWeekScheduleBridge.hour!!
                val minute = monthlyWeekScheduleBridge.minute!!
                NormalTime(hour, minute)
            }
        }

    val timePair: TimePair
        get() {
            val customTimeKey = monthlyWeekScheduleBridge.customTimeKey
            val hour = monthlyWeekScheduleBridge.hour
            val minute = monthlyWeekScheduleBridge.minute

            return if (customTimeKey != null) {
                check(hour == null)
                check(minute == null)

                TimePair(customTimeKey)
            } else {
                TimePair(HourMinute(hour!!, minute!!))
            }
        }

    val dayOfMonth get() = monthlyWeekScheduleBridge.dayOfMonth

    val dayOfWeek get() = DayOfWeek.values()[monthlyWeekScheduleBridge.dayOfWeek]

    val beginningOfMonth get() = monthlyWeekScheduleBridge.beginningOfMonth

    override val customTimeKey get() = monthlyWeekScheduleBridge.customTimeKey

    override val scheduleType get() = ScheduleType.MONTHLY_WEEK

    override fun getScheduleText(context: Context): String {
        val day = monthlyWeekScheduleBridge.dayOfMonth.toString() + " " + dayOfWeek + " " + context.getString(R.string.monthDayStart) + " " + context.resources.getStringArray(R.array.month)[if (monthlyWeekScheduleBridge.beginningOfMonth) 0 else 1] + " " + context.getString(R.string.monthDayEnd)

        return "$day: $time"
    }

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

        return kotlinDomainFactory.getInstance(task.taskKey, scheduleDateTime)
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

            val dateNextMonth = Date(calendar)

            val nextMonth = DateTime(dateNextMonth, time).timeStamp

            return if (endExactTimeStamp != null && endExactTimeStamp <= nextMonth.toExactTimeStamp())
                null
            else
                nextMonth
        }
    }

    private fun getDate(year: Int, month: Int) = Utils.getDateInMonth(year, month, monthlyWeekScheduleBridge.dayOfMonth, dayOfWeek, monthlyWeekScheduleBridge.beginningOfMonth)
}
