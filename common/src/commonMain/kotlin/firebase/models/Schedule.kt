package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.records.schedule.ScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.UserKey

abstract class Schedule<T : ProjectType>(val rootTask: Task<T>) : TaskParentEntry {

    protected abstract val scheduleRecord: ScheduleRecord<T>

    override val startExactTimeStamp by lazy { ExactTimeStamp.Local(scheduleRecord.startTime) }

    override val startExactTimeStampOffset by lazy {
        scheduleRecord.run { ExactTimeStamp.Offset.fromOffset(startTime, startTimeOffset) }
    }

    override val endExactTimeStamp get() = scheduleRecord.endTime?.let { ExactTimeStamp.Local(it) }

    override val endExactTimeStampOffset
        get() = scheduleRecord.endTime?.let {
            ExactTimeStamp.Offset.fromOffset(it, scheduleRecord.endTimeOffset)
        }

    val customTimeKey get() = scheduleRecord.customTimeKey

    abstract val scheduleType: ScheduleType

    val timePair get() = scheduleRecord.timePair

    val time get() = timePair.toTime()

    protected fun TimePair.toTime() = customTimeKey
            ?.let { rootTask.project.getCustomTime(it.customTimeId) }
            ?: Time.Normal(hourMinute!!)

    override fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrentOffset(endExactTimeStamp)

        scheduleRecord.endTime = endExactTimeStamp.long
        scheduleRecord.endTimeOffset = endExactTimeStamp.offset

        rootTask.invalidateIntervals()
    }

    override fun clearEndExactTimeStamp(now: ExactTimeStamp.Local) {
        requireNotCurrent(now)

        scheduleRecord.endTime = null
        scheduleRecord.endTimeOffset = null

        rootTask.invalidateIntervals()
    }

    abstract fun getDateTimesInRange(
            scheduleInterval: ScheduleInterval<T>,
            givenStartExactTimeStamp: ExactTimeStamp.Offset?,
            givenEndExactTimeStamp: ExactTimeStamp.Offset?,
            originalDateTime: Boolean = false,
            checkOldestVisible: Boolean = true,
    ): Sequence<DateTime>

    abstract fun isAfterOldestVisible(exactTimeStamp: ExactTimeStamp): Boolean

    fun delete() {
        rootTask.deleteSchedule(this)
        scheduleRecord.delete()
    }

    val scheduleId get() = scheduleRecord.scheduleId

    sealed class OldestVisible {

        open val date: Date? = null

        object Single : OldestVisible()

        object RepeatingNull : OldestVisible()

        data class RepeatingNonNull(override val date: Date) : OldestVisible()
    }

    abstract val oldestVisible: OldestVisible

    abstract fun updateOldestVisible(scheduleInterval: ScheduleInterval<T>, now: ExactTimeStamp.Local)

    val assignedTo get() = scheduleRecord.assignedTo.map { UserKey(it) }.toSet()

    override fun toString() = super.toString() + ", scheduleId: $scheduleId, type: ${this::class.simpleName}, startExactTimeStamp: $startExactTimeStamp, endExactTimeStamp: $endExactTimeStamp"

    fun fixOffsets() {
        if (scheduleRecord.startTimeOffset == null) scheduleRecord.startTimeOffset = startExactTimeStamp.offset

        endExactTimeStamp?.let {
            if (scheduleRecord.endTimeOffset == null) scheduleRecord.endTimeOffset = it.offset
        }
    }
}
