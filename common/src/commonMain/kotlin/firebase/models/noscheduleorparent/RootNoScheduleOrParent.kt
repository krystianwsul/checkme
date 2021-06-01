package com.krystianwsul.common.firebase.models.noscheduleorparent

import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.records.noscheduleorparent.RootNoScheduleOrParentRecord

class RootNoScheduleOrParent(
    private val rootTask: RootTask,
    private val rootNoScheduleOrParentRecord: RootNoScheduleOrParentRecord,
) : NoScheduleOrParent(rootTask, rootNoScheduleOrParentRecord) {

    val projectId get() = rootNoScheduleOrParentRecord.projectId

    override fun deleteFromParent() = rootTask.deleteNoScheduleOrParent(this)
}