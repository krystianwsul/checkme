package com.krystianwsul.common.firebase.models.noscheduleorparent

import com.krystianwsul.common.firebase.models.ProjectIdOwner
import com.krystianwsul.common.firebase.models.TaskParentEntry
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.noscheduleorparent.NoScheduleOrParentRecord
import com.krystianwsul.common.time.ExactTimeStamp

abstract class NoScheduleOrParent(
    private val task: Task,
    private val noScheduleOrParentRecord: NoScheduleOrParentRecord,
) : TaskParentEntry, ProjectIdOwner by noScheduleOrParentRecord {

    val startExactTimeStamp = ExactTimeStamp.Local(noScheduleOrParentRecord.startTime)

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

    override fun clearEndExactTimeStamp() {
        requireDeleted()

        noScheduleOrParentRecord.endTime = null
        noScheduleOrParentRecord.endTimeOffset = null

        task.invalidateIntervals()
    }

    protected abstract fun deleteFromParent()

    fun delete() {
        deleteFromParent()
        noScheduleOrParentRecord.delete()
    }
}