package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.NoScheduleOrParentRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

class NoScheduleOrParent<T : ProjectType>(
        private val task: Task<T>,
        private val noScheduleOrParentRecord: NoScheduleOrParentRecord<T>
) : TaskParentEntry {

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