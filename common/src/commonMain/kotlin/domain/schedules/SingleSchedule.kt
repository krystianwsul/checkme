package com.krystianwsul.common.domain.schedules


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType

class SingleSchedule<T : ProjectType>(
        rootTask: Task<T>,
        val singleScheduleBridge: SingleScheduleBridge<T>
) : Schedule<T>(rootTask) {

    override val scheduleBridge get() = singleScheduleBridge

    val date get() = Date(singleScheduleBridge.year, singleScheduleBridge.month, singleScheduleBridge.day)

    private val dateTime get() = DateTime(date, time)

    override val scheduleType get() = ScheduleType.SINGLE

    fun <T : ProjectType> getInstance(task: Task<T>) = task.getInstance(dateTime)

    override fun getNextAlarm(now: ExactTimeStamp) = dateTime.timeStamp.takeIf { it.toExactTimeStamp() > now }

    override fun <T : ProjectType> getInstances(
            task: Task<T>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp?
    ): Pair<Sequence<Instance<T>>, Boolean> {
        val singleScheduleExactTimeStamp = dateTime.timeStamp.toExactTimeStamp()

        if (givenStartExactTimeStamp?.let { it > singleScheduleExactTimeStamp } == true)
            return Pair(emptySequence(), false)

        if (givenExactEndTimeStamp?.let { it <= singleScheduleExactTimeStamp } == true)
            return Pair(emptySequence(), false)

        if (endExactTimeStamp?.let { singleScheduleExactTimeStamp >= it } == true)// timezone hack
            return Pair(emptySequence(), false)

        return Pair(sequenceOf(getInstance(task)), false)
    }

    override fun isVisible(task: Task<*>, now: ExactTimeStamp, hack24: Boolean): Boolean {
        requireCurrent(now)

        return getInstance(task).isVisible(now, hack24)
    }
}
