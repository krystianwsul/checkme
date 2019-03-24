package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import java.util.*

class SingleSchedule(
        domainFactory: DomainFactory,
        private val singleScheduleBridge: SingleScheduleBridge) : Schedule(domainFactory) {

    override val scheduleBridge get() = singleScheduleBridge

    val date get() = Date(singleScheduleBridge.year, singleScheduleBridge.month, singleScheduleBridge.day)

    private val dateTime get() = DateTime(date, time)

    override val scheduleType get() = ScheduleType.SINGLE

    override fun getScheduleText() = dateTime.getDisplayText()

    fun getInstance(task: Task) = domainFactory.getInstance(InstanceKey(task.taskKey, date, timePair))

    override fun getNextAlarm(now: ExactTimeStamp) = dateTime.timeStamp.takeIf { it.toExactTimeStamp() > now }

    override fun getInstances(task: Task, givenStartExactTimeStamp: ExactTimeStamp?, givenExactEndTimeStamp: ExactTimeStamp): List<Instance> {
        val instances = ArrayList<Instance>()

        val singleScheduleExactTimeStamp = dateTime.timeStamp.toExactTimeStamp()

        if (givenStartExactTimeStamp != null && givenStartExactTimeStamp > singleScheduleExactTimeStamp)
            return instances

        if (givenExactEndTimeStamp <= singleScheduleExactTimeStamp)
            return instances

        val endExactTimeStamp = getEndExactTimeStamp()
        if (endExactTimeStamp != null && singleScheduleExactTimeStamp >= endExactTimeStamp)
        // timezone hack
            return instances

        instances.add(getInstance(task))

        return instances
    }

    override fun isVisible(task: Task, now: ExactTimeStamp, hack24: Boolean): Boolean {
        check(current(now))

        return getInstance(task).isVisible(now, hack24)
    }
}
