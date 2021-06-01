package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.ProjectIdOwner
import com.krystianwsul.common.firebase.models.TaskParentEntry
import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.ScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

abstract class Schedule(val topLevelTask: Task) : TaskParentEntry, ProjectIdOwner {

    protected abstract val scheduleRecord: ScheduleRecord

    val id get() = scheduleRecord.id

    override val startExactTimeStamp by lazy { ExactTimeStamp.Local(scheduleRecord.startTime) }

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

    val projectId get() = scheduleRecord.projectId

    protected fun TimePair.toTime() =
            customTimeKey?.let(topLevelTask.customTimeProvider::getCustomTime) ?: Time.Normal(hourMinute!!)

    private fun invalidateEnd() {
        endExactTimeStampProperty.invalidate()
        endExactTimeStampOffsetProperty.invalidate()
    }

    override fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        ProjectRootTaskIdTracker.checkTracking()

        requireCurrentOffset(endExactTimeStamp)

        scheduleRecord.endTime = endExactTimeStamp.long
        scheduleRecord.endTimeOffset = endExactTimeStamp.offset

        invalidateEnd()
        topLevelTask.invalidateIntervals()
    }

    override fun clearEndExactTimeStamp(now: ExactTimeStamp.Local) {
        requireNotCurrent(now)

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

    abstract fun isAfterOldestVisible(exactTimeStamp: ExactTimeStamp): Boolean

    fun delete() {
        topLevelTask.deleteSchedule(this)
        scheduleRecord.delete()
    }

    sealed class OldestVisible {

        open val date: Date? = null

        object Single : OldestVisible()

        object RepeatingNull : OldestVisible()

        data class RepeatingNonNull(override val date: Date) : OldestVisible()
    }

    abstract val oldestVisible: OldestVisible

    abstract fun updateOldestVisible(scheduleInterval: ScheduleInterval, now: ExactTimeStamp.Local)

    val assignedTo get() = scheduleRecord.assignedTo.map { UserKey(it) }.toSet()

    override fun toString() = super.toString() + ", taskKey: ${topLevelTask.taskKey}, id: $id, type: ${this::class.simpleName}, startExactTimeStamp: $startExactTimeStamp, endExactTimeStamp: $endExactTimeStamp"

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
