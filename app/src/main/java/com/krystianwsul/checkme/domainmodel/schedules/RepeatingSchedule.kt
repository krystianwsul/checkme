package com.krystianwsul.checkme.domainmodel.schedules

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.domainmodel.Task
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.HourMilli

import java.util.*

abstract class RepeatingSchedule(domainFactory: DomainFactory) : Schedule(domainFactory) {

    override fun getInstances(task: Task, givenStartExactTimeStamp: ExactTimeStamp?, givenExactEndTimeStamp: ExactTimeStamp): List<Instance> {
        val myStartTimeStamp = startExactTimeStamp
        val myEndTimeStamp = getEndExactTimeStamp()

        val instances = ArrayList<Instance?>()

        val startExactTimeStamp = if (givenStartExactTimeStamp == null || givenStartExactTimeStamp < myStartTimeStamp)
            myStartTimeStamp
        else
            givenStartExactTimeStamp

        val endExactTimeStamp = if (myEndTimeStamp == null || myEndTimeStamp > givenExactEndTimeStamp)
            givenExactEndTimeStamp
        else
            myEndTimeStamp

        if (startExactTimeStamp >= endExactTimeStamp)
            return instances.filterNotNull()

        check(startExactTimeStamp < endExactTimeStamp)

        if (startExactTimeStamp.date == endExactTimeStamp.date) {
            instances.add(getInstanceInDate(task, startExactTimeStamp.date, startExactTimeStamp.hourMilli, endExactTimeStamp.hourMilli))
        } else {
            instances.add(getInstanceInDate(task, startExactTimeStamp.date, startExactTimeStamp.hourMilli, null))

            val loopStartCalendar = startExactTimeStamp.date.calendar
            loopStartCalendar.add(Calendar.DATE, 1)
            val loopEndCalendar = endExactTimeStamp.date.calendar

            while (loopStartCalendar.before(loopEndCalendar)) {
                instances.add(getInstanceInDate(task, Date(loopStartCalendar), null, null))
                loopStartCalendar.add(Calendar.DATE, 1)
            }

            instances.add(getInstanceInDate(task, endExactTimeStamp.date, null, endExactTimeStamp.hourMilli))
        }

        return instances.filterNotNull()
    }

    protected abstract fun getInstanceInDate(task: Task, date: Date, startHourMilli: HourMilli?, endHourMilli: HourMilli?): Instance?

    override fun isVisible(task: Task, now: ExactTimeStamp, hack24: Boolean, ignoreCurrent: Boolean): Boolean {
        return current(now) // todo search?
    }
}
