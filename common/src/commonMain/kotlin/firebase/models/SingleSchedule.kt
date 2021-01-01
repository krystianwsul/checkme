package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.json.schedule.WriteAssignedToJson
import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.records.schedule.SingleScheduleRecord
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.UserKey

class SingleSchedule<T : ProjectType>(
        rootTask: Task<T>,
        val singleScheduleRecord: SingleScheduleRecord<T>,
) : Schedule<T>(rootTask) {

    private val mockInstance get() = getInstance(rootTask).takeIf { it.exists() }

    override val scheduleRecord get() = mockInstance?.let { MockRecord(it) } ?: singleScheduleRecord

    val date get() = scheduleRecord.date

    private val dateTime get() = DateTime(date, time)

    override val scheduleType get() = ScheduleType.SINGLE

    private val originalScheduleDateTime
        get() = singleScheduleRecord.run { // specifically not scheduleRecord
            DateTime(originalDate, originalTimePair.toTime())
        }

    fun <T : ProjectType> getInstance(task: Task<T>) = task.getInstance(originalScheduleDateTime)

    override fun getDateTimesInRange(
            scheduleInterval: ScheduleInterval<T>,
            givenStartExactTimeStamp: ExactTimeStamp.Offset?,
            givenEndExactTimeStamp: ExactTimeStamp.Offset?,
            originalDateTime: Boolean,
            checkOldestVisible: Boolean,
    ): Sequence<DateTime> {
        val dateTime = if (originalDateTime) originalScheduleDateTime else dateTime

        val scheduleExactTimeStamp = dateTime.timeStamp.toLocalExactTimeStamp()

        if (givenStartExactTimeStamp?.let { it > scheduleExactTimeStamp } == true) return emptySequence()

        if (givenEndExactTimeStamp?.let { it <= scheduleExactTimeStamp } == true) return emptySequence()

        if (endExactTimeStampOffset?.let { scheduleExactTimeStamp >= it } == true) return emptySequence()

        if (scheduleInterval.endExactTimeStampOffset?.let { scheduleExactTimeStamp >= it } == true)
            return emptySequence()

        return sequenceOf(originalScheduleDateTime)
    }

    override fun isAfterOldestVisible(exactTimeStamp: ExactTimeStamp) = true

    override val oldestVisible = OldestVisible.Single

    override fun updateOldestVisible(
            scheduleInterval: ScheduleInterval<T>,
            now: ExactTimeStamp.Local,
    ) = Unit

    fun setAssignedTo(assignedTo: Set<UserKey>) {
        val writeAssignedToJson = singleScheduleRecord.singleScheduleJson as? WriteAssignedToJson
                ?: throw UnsupportedOperationException()

        rootTask.project.assignedToHelper.setAssignedTo(
                writeAssignedToJson,
                singleScheduleRecord,
                assignedTo.map { it.key }.toSet(),
        )
    }

    private inner class MockRecord(private val instance: Instance<T>) : SingleScheduleRecord<T>(
            singleScheduleRecord.taskRecord,
            singleScheduleRecord.createObject,
            singleScheduleRecord.id,
    ) {

        override val date get() = instance.instanceDate

        override val timePair get() = instance.instanceTimePair

        override val originalTimePair get() = singleScheduleRecord.timePair

        override val originalDate get() = singleScheduleRecord.date

        override var startTimeOffset: Double?
            get() = singleScheduleRecord.startTimeOffset
            set(value) {
                singleScheduleRecord.startTimeOffset = value
            }

        override var endTime
            get() = singleScheduleRecord.endTime
            set(value) {
                singleScheduleRecord.endTime = value
            }

        override var endTimeOffset
            get() = singleScheduleRecord.endTimeOffset
            set(value) {
                singleScheduleRecord.endTimeOffset = value
            }

        override fun delete() = singleScheduleRecord.delete()
    }
}
