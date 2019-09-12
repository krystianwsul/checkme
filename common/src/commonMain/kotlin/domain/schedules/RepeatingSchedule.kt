package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.domain.Instance
import com.krystianwsul.common.domain.Task
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMilli
import com.soywiz.klock.days

abstract class RepeatingSchedule(rootTask: RemoteTask<*>) : Schedule(rootTask) {

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

            var loopStartCalendar = startExactTimeStamp.date.toDateTimeTz() + 1.days
            val loopEndCalendar = endExactTimeStamp.date.toDateTimeTz()

            while (loopStartCalendar < loopEndCalendar) {
                instances.add(getInstanceInDate(task, Date(loopStartCalendar), null, null))
                loopStartCalendar += 1.days
            }

            instances.add(getInstanceInDate(task, endExactTimeStamp.date, null, endExactTimeStamp.hourMilli))
        }

        return instances.filterNotNull()
    }

    protected abstract fun getInstanceInDate(task: Task, date: Date, startHourMilli: HourMilli?, endHourMilli: HourMilli?): Instance?

    override fun isVisible(task: Task, now: ExactTimeStamp, hack24: Boolean): Boolean {
        check(current(now))

        return true
    }
}
