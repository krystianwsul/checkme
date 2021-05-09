package com.krystianwsul.common.firebase.records.noscheduleorparent

import com.krystianwsul.common.firebase.json.noscheduleorparent.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.records.schedule.ProjectHelper
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord

class ProjectNoScheduleOrParentRecord(
    private val projectTaskRecord: ProjectTaskRecord,
    createObject: NoScheduleOrParentJson,
    _id: String?,
    projectHelper: ProjectHelper,
) : NoScheduleOrParentRecord(projectTaskRecord, createObject, _id, projectHelper) {

    override fun deleteFromParent() = check(projectTaskRecord.noScheduleOrParentRecords.remove(id) == this)
}