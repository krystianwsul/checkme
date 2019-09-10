package com.krystianwsul.checkme.domainmodel.schedules

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.domainmodel.Task
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import java.util.*

class SingleSchedule(
        domainFactory: DomainFactory,
        private val singleScheduleBridge: SingleScheduleBridge) : Schedule(domainFactory) {

    override val scheduleBridge get() = singleScheduleBridge

    val date get() = Date(singleScheduleBridge.year, singleScheduleBridge.month, singleScheduleBridge.day)

    val dateTime get() = DateTime(date, time)

    override val scheduleType get() = ScheduleType.SINGLE

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
        if (endExactTimeStamp != null && singleScheduleExactTimeStamp >= endExactTimeStamp)// timezone hack
            return instances

        instances.add(getInstance(task))

        return instances
    }

    override fun isVisible(task: Task, now: ExactTimeStamp, hack24: Boolean): Boolean {
        check(current(now))

        return getInstance(task).isVisible(now, hack24)
    }
}
