package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.NoScheduleOrParentRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

class NoScheduleOrParent<T : ProjectType>(
        private val task: Task<T>,
        private val noScheduleOrParentRecord: NoScheduleOrParentRecord<T>
) : TaskParentEntry {

    override val startExactTimeStamp = ExactTimeStamp(noScheduleOrParentRecord.startTime)

    override val startExactTimeStampOffset by lazy {
        noScheduleOrParentRecord.run { ExactTimeStamp.fromOffset(startTime, startTimeOffset) }
    }

    override val endExactTimeStamp get() = noScheduleOrParentRecord.endTime?.let(::ExactTimeStamp)

    override val endExactTimeStampOffset
        get() = noScheduleOrParentRecord.endTime?.let {
            ExactTimeStamp.fromOffset(it, noScheduleOrParentRecord.endTimeOffset)
        }

    val id = noScheduleOrParentRecord.id

    override fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrent(endExactTimeStamp)

        noScheduleOrParentRecord.endTime = endExactTimeStamp.long

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