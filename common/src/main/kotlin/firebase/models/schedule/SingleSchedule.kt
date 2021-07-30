package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.json.schedule.WriteAssignedToJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.ProjectHelper
import com.krystianwsul.common.firebase.records.schedule.SingleScheduleRecord
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.UserKey

class SingleSchedule(topLevelTask: Task, val singleScheduleRecord: SingleScheduleRecord) : Schedule(topLevelTask) {

    private val mockInstance get() = getInstance(topLevelTask).takeIf { it.exists() }

    override val scheduleRecord
        get() =
            mockInstance?.let { MockRecord(it, singleScheduleRecord.projectHelper) } ?: singleScheduleRecord

    val date get() = scheduleRecord.date

    private val dateTime get() = DateTime(date, time)

    override val scheduleType get() = ScheduleType.SINGLE

    private val originalScheduleDateTime
        get() = singleScheduleRecord.run { // specifically not scheduleRecord
            DateTime(originalDate, originalTimePair.toTime())
        }

    fun getInstance(task: Task) = task.getInstance(originalScheduleDateTime)

    override fun getDateTimesInRange(
        scheduleInterval: ScheduleInterval,
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

    override fun updateOldestVisible(scheduleInterval: ScheduleInterval, now: ExactTimeStamp.Local) = Unit

    fun setAssignedTo(assignedTo: Set<UserKey>) {
        val writeAssignedToJson = singleScheduleRecord.singleScheduleJson as? WriteAssignedToJson
            ?: throw UnsupportedOperationException()

        topLevelTask.project
            .assignedToHelper
            .setAssignedTo(writeAssignedToJson, singleScheduleRecord, assignedTo.map { it.key }.toSet())
    }

    override fun toString() = super.toString() + ", dateTime: $dateTime"

    private inner class MockRecord(private val instance: Instance, projectHelper: ProjectHelper) : SingleScheduleRecord(
        singleScheduleRecord.taskRecord,
        singleScheduleRecord.createObject,
        projectHelper,
        singleScheduleRecord.projectRootDelegate,
        singleScheduleRecord.id,
        false,
    ) {

        override val date get() = instance.instanceDate

        override val timePair get() = instance.instanceTime.timePair

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

        override fun updateProject(projectKey: ProjectKey<*>) = singleScheduleRecord.updateProject(projectKey)
    }
}
