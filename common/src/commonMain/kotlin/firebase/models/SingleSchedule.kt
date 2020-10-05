package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.SingleScheduleRecord
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceSequenceData
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType
import firebase.models.interval.ScheduleInterval

class SingleSchedule<T : ProjectType>(
        rootTask: Task<T>,
        val singleScheduleRecord: SingleScheduleRecord<T>
) : Schedule<T>(rootTask) {

    val mockInstance get() = getInstance(rootTask).takeIf { it.exists() }

    override val scheduleRecord get() = mockInstance?.let { MockRecord(it) } ?: singleScheduleRecord

    val date get() = scheduleRecord.date

    private val dateTime get() = DateTime(date, time)

    val originalDateTime get() = DateTime(scheduleRecord.originalDate, scheduleRecord.originalTimePair.toTime())

    override val scheduleType get() = ScheduleType.SINGLE

    private val originalScheduleDateTime
        get() = singleScheduleRecord.run { // specifically not scheduleRecord
            DateTime(date, originalTimePair.toTime())
        }

    fun <T : ProjectType> getInstance(task: Task<T>) = task.getInstance(originalScheduleDateTime)

    override fun getDateTimesInRange(
            scheduleInterval: ScheduleInterval<T>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp?
    ): InstanceSequenceData {
        val singleScheduleExactTimeStamp = dateTime.timeStamp.toExactTimeStamp()

        if (givenStartExactTimeStamp?.let { it > singleScheduleExactTimeStamp } == true)
            return InstanceSequenceData(emptySequence(), false)

        if (givenExactEndTimeStamp?.let { it <= singleScheduleExactTimeStamp } == true)
            return InstanceSequenceData(emptySequence(), true)

        if (endExactTimeStamp?.let { singleScheduleExactTimeStamp >= it } == true)// timezone hack
            return InstanceSequenceData(emptySequence(), false)

        if (scheduleInterval.endExactTimeStamp?.let { singleScheduleExactTimeStamp >= it } == true)// timezone hack
            return InstanceSequenceData(emptySequence(), false)

        return InstanceSequenceData(sequenceOf(originalScheduleDateTime), false)
    }

    override fun isVisible(
            scheduleInterval: ScheduleInterval<T>,
            task: Task<T>,
            now: ExactTimeStamp,
            hack24: Boolean
    ): Boolean {
        scheduleInterval.requireCurrent(now)
        requireCurrent(now)

        return getInstance(task).isVisible(now, hack24)
    }

    override val oldestVisible = OldestVisible.Single

    override fun updateOldestVisible(
            scheduleInterval: ScheduleInterval<T>,
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

        override val originalDate get() = singleScheduleRecord.date

        override var endTime
            get() = singleScheduleRecord.endTime
            set(value) {
                singleScheduleRecord.endTime = value
            }
    }

    override fun matchesScheduleDateTimeHelper(scheduleDateTime: DateTime, checkOldestVisible: Boolean): Boolean {
        if (singleScheduleRecord.originalTimePair != scheduleDateTime.time.timePair)
            return false

        return scheduleDateTime.date == date
    }

    val group = singleScheduleRecord.group
}
