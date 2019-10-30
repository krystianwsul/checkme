package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.domain.Instance
import com.krystianwsul.common.domain.Task
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ScheduleType

class SingleSchedule(
        rootTask: RemoteTask<*>,
        private val singleScheduleBridge: SingleScheduleBridge
) : Schedule(rootTask) {

    override val scheduleBridge get() = singleScheduleBridge

    val date get() = Date(singleScheduleBridge.year, singleScheduleBridge.month, singleScheduleBridge.day)

    val dateTime get() = DateTime(date, time)

    override val scheduleType get() = ScheduleType.SINGLE

    fun getInstance(task: Task) = task.getInstance(dateTime)

    override fun getNextAlarm(now: ExactTimeStamp) = dateTime.timeStamp.takeIf { it.toExactTimeStamp() > now }

    override fun getInstances(
            task: Task,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp
    ): Sequence<Instance> {
        val singleScheduleExactTimeStamp = dateTime.timeStamp.toExactTimeStamp()

        if (givenStartExactTimeStamp != null && givenStartExactTimeStamp > singleScheduleExactTimeStamp)
            return emptySequence()

        if (givenExactEndTimeStamp <= singleScheduleExactTimeStamp)
            return emptySequence()

        val endExactTimeStamp = getEndExactTimeStamp()
        if (endExactTimeStamp != null && singleScheduleExactTimeStamp >= endExactTimeStamp)// timezone hack
            return emptySequence()

        return sequenceOf(getInstance(task))
    }

    override fun isVisible(task: Task, now: ExactTimeStamp, hack24: Boolean): Boolean {
        check(current(now))

        return getInstance(task).isVisible(now, hack24)
    }
}
