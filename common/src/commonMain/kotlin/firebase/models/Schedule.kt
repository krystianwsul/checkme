package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.records.ScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType

abstract class Schedule<T : ProjectType>(val rootTask: Task<T>) : TaskParentEntry {

    protected abstract val scheduleRecord: ScheduleRecord<T>

    override val startExactTimeStamp by lazy { ExactTimeStamp(scheduleRecord.startTime) }
    override val startDateTime by lazy { DateTime.fromOffset(scheduleRecord.startTime, scheduleRecord.startTimeOffset) }

    override val endExactTimeStamp get() = scheduleRecord.endTime?.let { ExactTimeStamp(it) }

    override val endDateTime
        get() = scheduleRecord.endTime?.let {
            DateTime.fromOffset(it, scheduleRecord.endTimeOffset)
        }

    val customTimeKey get() = scheduleRecord.customTimeKey

    abstract val scheduleType: ScheduleType

    val timePair get() = scheduleRecord.timePair

    val time get() = timePair.toTime()

    protected fun TimePair.toTime() = customTimeKey
            ?.let { rootTask.project.getCustomTime(it.customTimeId) }
            ?: Time.Normal(hourMinute!!)

    override fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrent(endExactTimeStamp)

        scheduleRecord.endTime = endExactTimeStamp.long

        scheduleRecord.endTimeOffset = endExactTimeStamp.toDateTimeTz()
                .offset
                .totalMilliseconds

        rootTask.invalidateIntervals()
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        requireNotCurrent(now)

        scheduleRecord.endTime = null
        scheduleRecord.endTimeOffset = null

        rootTask.invalidateIntervals()
    }

    abstract fun getDateTimesInRange(
            scheduleInterval: ScheduleInterval<T>,
            givenStartDateTime: DateTime?,
            givenEndDateTime: DateTime?
    ): Sequence<DateTime>

    abstract fun isVisible(
            scheduleInterval: ScheduleInterval<T>,
            task: Task<T>,
            now: ExactTimeStamp,
            hack24: Boolean
    ): Boolean

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

    abstract fun updateOldestVisible(scheduleInterval: ScheduleInterval<T>, now: ExactTimeStamp)

    fun matchesScheduleDateTime(
            scheduleInterval: ScheduleInterval<T>,
            scheduleDateTime: DateTime,
            checkOldestVisible: Boolean
    ): Boolean {
        if (scheduleDateTime < startDateTime)
            return false

        val exactTimeStamp = scheduleDateTime.toExactTimeStamp()

        if (exactTimeStamp < scheduleInterval.startExactTimeStamp)
            return false

        if (endDateTime?.let { scheduleDateTime >= it } == true)
            return false

        if (scheduleInterval.endExactTimeStamp?.let { exactTimeStamp >= it } == true)
            return false

        return matchesScheduleDateTimeHelper(scheduleDateTime, checkOldestVisible)
    }

    protected abstract fun matchesScheduleDateTimeHelper(
            scheduleDateTime: DateTime,
            checkOldestVisible: Boolean
    ): Boolean

    override fun toString() = super.toString() + ", scheduleId: $scheduleId, type: ${this::class.simpleName}, startExactTimeStamp: $startExactTimeStamp, endExactTimeStamp: $endExactTimeStamp"

    fun fixOffsets() {
        if (scheduleRecord.startTimeOffset == null) scheduleRecord.startTimeOffset = startExactTimeStamp.offset

        endExactTimeStamp?.let {
            if (scheduleRecord.endTimeOffset == null) scheduleRecord.endTimeOffset = it.offset
        }
    }
}
