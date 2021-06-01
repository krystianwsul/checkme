package com.krystianwsul.common.firebase.models.noscheduleorparent

import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.records.noscheduleorparent.RootNoScheduleOrParentRecord
import com.krystianwsul.common.time.ExactTimeStamp

class RootNoScheduleOrParent(
    private val rootTask: RootTask,
    private val rootNoScheduleOrParentRecord: RootNoScheduleOrParentRecord,
) : NoScheduleOrParent(rootTask, rootNoScheduleOrParentRecord) {

    val projectId get() = rootNoScheduleOrParentRecord.projectId

    override fun deleteFromParent() = rootTask.deleteNoScheduleOrParent(this)

    override fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        ProjectRootTaskIdTracker.checkTracking()

        super.setEndExactTimeStamp(endExactTimeStamp)
    }
}