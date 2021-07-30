package com.krystianwsul.common.firebase.models.noscheduleorparent

import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.records.noscheduleorparent.ProjectNoScheduleOrParentRecord

class ProjectNoScheduleOrParent(
    private val projectTask: ProjectTask,
    private val projectNoScheduleOrParentRecord: ProjectNoScheduleOrParentRecord,
) : NoScheduleOrParent(projectTask, projectNoScheduleOrParentRecord) {

    override fun deleteFromParent() = projectTask.deleteNoScheduleOrParent(this)

    fun fixOffsets() {
        if (projectNoScheduleOrParentRecord.startTimeOffset == null)
            projectNoScheduleOrParentRecord.startTimeOffset = startExactTimeStamp.offset

        endExactTimeStamp?.let {
            if (projectNoScheduleOrParentRecord.endTimeOffset == null)
                projectNoScheduleOrParentRecord.endTimeOffset = it.offset
        }
    }
}