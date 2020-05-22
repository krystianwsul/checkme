package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.NoScheduleOrParentRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.ProjectType

class NoScheduleOrParent<T : ProjectType>(
        private val task: Task<T>,
        private val noScheduleOrParentRecord: NoScheduleOrParentRecord<T>
) : Current {

    override val startExactTimeStamp = ExactTimeStamp(noScheduleOrParentRecord.startTime)
    override val endExactTimeStamp get() = noScheduleOrParentRecord.endTime?.let(::ExactTimeStamp)

    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrent(endExactTimeStamp)

        noScheduleOrParentRecord.endTime = endExactTimeStamp.long

        task.invalidateIntervals()
    }

    /*
     todo no schedule record relevance, invalidate task stuff
     */
}