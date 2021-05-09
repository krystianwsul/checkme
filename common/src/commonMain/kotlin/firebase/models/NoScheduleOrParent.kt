package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.noscheduleorparent.NoScheduleOrParentRecord
import com.krystianwsul.common.time.ExactTimeStamp

class NoScheduleOrParent(
    private val task: Task,
    private val noScheduleOrParentRecord: NoScheduleOrParentRecord,
) : TaskParentEntry, ProjectIdOwner by noScheduleOrParentRecord {

    override val startExactTimeStamp = ExactTimeStamp.Local(noScheduleOrParentRecord.startTime)

    override val startExactTimeStampOffset by lazy {
        noScheduleOrParentRecord.run { ExactTimeStamp.Offset.fromOffset(startTime, startTimeOffset) }
    }

    override val endExactTimeStamp get() = noScheduleOrParentRecord.endTime?.let(ExactTimeStamp::Local)

    override val endExactTimeStampOffset
        get() = noScheduleOrParentRecord.endTime?.let {
            ExactTimeStamp.Offset.fromOffset(it, noScheduleOrParentRecord.endTimeOffset)
        }

    val id = noScheduleOrParentRecord.id

    val projectId get() = noScheduleOrParentRecord.projectId

    override fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrentOffset(endExactTimeStamp)

        noScheduleOrParentRecord.endTime = endExactTimeStamp.long
        noScheduleOrParentRecord.endTimeOffset = endExactTimeStamp.offset

        task.invalidateIntervals()
    }

    override fun clearEndExactTimeStamp(now: ExactTimeStamp.Local) {
        requireNotCurrent(now)

        noScheduleOrParentRecord.endTime = null
        noScheduleOrParentRecord.endTimeOffset = null

        task.invalidateIntervals()
    }

    fun delete() {
        task.deleteNoScheduleOrParent(this)
        noScheduleOrParentRecord.delete()
    }

    fun fixOffsets() {
        if (noScheduleOrParentRecord.startTimeOffset == null)
            noScheduleOrParentRecord.startTimeOffset = startExactTimeStamp.offset

        endExactTimeStamp?.let {
            if (noScheduleOrParentRecord.endTimeOffset == null) noScheduleOrParentRecord.endTimeOffset = it.offset
        }
    }
}