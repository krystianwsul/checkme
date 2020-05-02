package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.SingleScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType

class SingleSchedule<T : ProjectType>(
        rootTask: Task<T>,
        val singleScheduleRecord: SingleScheduleRecord<T>
) : Schedule<T>(rootTask) {

    override val scheduleRecord get() = singleScheduleRecord

    val date get() = Date(singleScheduleRecord.year, singleScheduleRecord.month, singleScheduleRecord.day)

    private val dateTime get() = DateTime(date, time)

    override val scheduleType get() = ScheduleType.SINGLE

    fun <T : ProjectType> getInstance(task: Task<T>) = task.getInstance(DateTime(date, singleScheduleRecord.originalTimePair.toTime()))

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
