package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.DirectTaskParentEntry
import com.krystianwsul.common.firebase.models.ProjectIdOwner
import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.ScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

sealed class Schedule(val topLevelTask: Task) : DirectTaskParentEntry, ProjectIdOwner {

    protected abstract val scheduleRecord: ScheduleRecord

    val id get() = scheduleRecord.id
    val key get() = scheduleRecord.scheduleKey

    val startExactTimeStamp by lazy { ExactTimeStamp.Local(scheduleRecord.startTime) }

    protected val startExactTimeStampOffsetProperty = invalidatableLazyCallbacks {
        scheduleRecord.run { ExactTimeStamp.Offset.fromOffset(startTime, startTimeOffset) }
    }
    override val startExactTimeStampOffset by startExactTimeStampOffsetProperty

    private val endExactTimeStampProperty = invalidatableLazy {
        scheduleRecord.endTime?.let { ExactTimeStamp.Local(it) }
    }
    override val endExactTimeStamp by endExactTimeStampProperty

    protected val endExactTimeStampOffsetProperty = invalidatableLazyCallbacks {
        scheduleRecord.endTime?.let {
            ExactTimeStamp.Offset.fromOffset(it, scheduleRecord.endTimeOffset)
        }
    }
    override val endExactTimeStampOffset by endExactTimeStampOffsetProperty

    val customTimeKey get() = scheduleRecord.customTimeKey

    abstract val scheduleType: ScheduleType

    val timePair get() = scheduleRecord.timePair

    val time get() = timePair.toTime()

    override val projectId get() = scheduleRecord.projectId
    override val projectKey get() = scheduleRecord.projectKey

    protected fun TimePair.toTime() =
        customTimeKey?.let(topLevelTask.customTimeProvider::getCustomTime) ?: Time.Normal(hourMinute!!)

    private fun invalidateEnd() {
        endExactTimeStampProperty.invalidate()
        endExactTimeStampOffsetProperty.invalidate()
    }

    override fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrentOffset(endExactTimeStamp)

        scheduleRecord.endTime = endExactTimeStamp.long
        scheduleRecord.endTimeOffset = endExactTimeStamp.offset

        invalidateEnd()
        topLevelTask.invalidateIntervals()
    }

    override fun clearEndExactTimeStamp() {
        requireDeleted()

        scheduleRecord.endTime = null
        scheduleRecord.endTimeOffset = null

        invalidateEnd()
        topLevelTask.invalidateIntervals()
    }

    abstract fun getDateTimesInRange(
        scheduleInterval: ScheduleInterval,
            givenStartExactTimeStamp: ExactTimeStamp.Offset?,
            givenEndExactTimeStamp: ExactTimeStamp.Offset?,
            originalDateTime: Boolean = false,
            checkOldestVisible: Boolean = true,
    ): Sequence<DateTime>

    abstract fun matchesScheduleDate(scheduleDate: Date): Boolean

    fun delete() {
        topLevelTask.deleteSchedule(this)
        scheduleRecord.delete()
    }

    sealed interface OldestVisible {

        val date: Date? get() = null

        object Single : OldestVisible

        data class Repeating(val repeatingOldestVisible: RepeatingSchedule.RepeatingOldestVisible) : OldestVisible
    }

    abstract val oldestVisible: OldestVisible

    abstract fun updateOldestVisible(scheduleInterval: ScheduleInterval, now: ExactTimeStamp.Local)

    protected val assignedToProperty = invalidatableLazy { scheduleRecord.assignedTo.map { UserKey(it) }.toSet() }
    val assignedTo by assignedToProperty

    override fun toString() =
        super.toString() + ", taskKey: ${topLevelTask.taskKey}, id: $id, type: ${this::class.simpleName}, startExactTimeStamp: $startExactTimeStamp, endExactTimeStamp: $endExactTimeStamp"

    fun fixOffsets() {
        if (scheduleRecord.startTimeOffset == null) {
            scheduleRecord.startTimeOffset = startExactTimeStamp.offset

            startExactTimeStampOffsetProperty.invalidate()
        }

        endExactTimeStamp?.let {
            if (scheduleRecord.endTimeOffset == null) {
                scheduleRecord.endTimeOffset = it.offset

                endExactTimeStampOffsetProperty.invalidate()
            }
        }
    }

    override fun updateProject(projectKey: ProjectKey<*>) = scheduleRecord.updateProject(projectKey)
}
