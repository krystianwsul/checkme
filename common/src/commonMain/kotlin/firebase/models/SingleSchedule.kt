package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.models.interval.IntervalBuilder
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

    val mockInstance get() = getInstance(rootTask).takeIf { it.exists() }

    override val scheduleRecord get() = mockInstance?.let { MockRecord(it) } ?: singleScheduleRecord

    val date get() = scheduleRecord.date

    private val dateTime get() = DateTime(date, time)

    override val scheduleType get() = ScheduleType.SINGLE

    fun <T : ProjectType> getInstance(task: Task<T>) = singleScheduleRecord.run { // specifically not scheduleRecord
        task.getInstance(DateTime(date, originalTimePair.toTime()))
    }

    override fun getNextAlarm(
            scheduleInterval: IntervalBuilder.ScheduleInterval<T>,
            now: ExactTimeStamp
    ) = dateTime.timeStamp.takeIf { it.toExactTimeStamp() > now }

    override fun getInstances(
            scheduleInterval: IntervalBuilder.ScheduleInterval<T>,
            task: Task<T>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp?
    ): Pair<Sequence<Instance<T>>, Boolean> {
        val singleScheduleExactTimeStamp = dateTime.timeStamp.toExactTimeStamp()

        if (givenStartExactTimeStamp?.let { it > singleScheduleExactTimeStamp } == true)
            return Pair(emptySequence(), false)

        if (givenExactEndTimeStamp?.let { it <= singleScheduleExactTimeStamp } == true)
            return Pair(emptySequence(), true)

        if (endExactTimeStamp?.let { singleScheduleExactTimeStamp >= it } == true)// timezone hack
            return Pair(emptySequence(), false)

        if (scheduleInterval.endExactTimeStamp?.let { singleScheduleExactTimeStamp >= it } == true)// timezone hack
            return Pair(emptySequence(), false)

        return Pair(sequenceOf(getInstance(task)), false)
    }

    override fun isVisible(
            scheduleInterval: IntervalBuilder.ScheduleInterval<T>,
            task: Task<T>,
            now: ExactTimeStamp,
            hack24: Boolean
    ): Boolean {
        scheduleInterval.requireCurrent(now)
        requireCurrent(now)

        return getInstance(task).isVisible(now, hack24)
    }

    override val oldestVisible: Date? = null

    override fun updateOldestVisible(
            scheduleInterval: IntervalBuilder.ScheduleInterval<T>,
            now: ExactTimeStamp
    ) = Unit

    private inner class MockRecord(private val instance: Instance<T>) : SingleScheduleRecord<T>(
            singleScheduleRecord.taskRecord,
            singleScheduleRecord.createObject,
            singleScheduleRecord.id
    ) {

        override val date get() = instance.instanceDate

        override val timePair get() = instance.instanceTimePair

        override val originalTimePair get() = singleScheduleRecord.timePair
    }

    override fun matchesScheduleDateTimeHelper(scheduleDateTime: DateTime) = scheduleDateTime.date == date
}
