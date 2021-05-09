package com.krystianwsul.common.firebase.records.noscheduleorparent

import com.krystianwsul.common.firebase.json.noscheduleorparent.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.records.schedule.ProjectHelper
import com.krystianwsul.common.firebase.records.task.RootTaskRecord

class RootNoScheduleOrParentRecord(
    private val rootTaskRecord: RootTaskRecord,
    createObject: NoScheduleOrParentJson,
    _id: String?,
    projectHelper: ProjectHelper,
) : NoScheduleOrParentRecord(rootTaskRecord, createObject, _id, projectHelper) {

    override fun deleteFromParent() = check(rootTaskRecord.noScheduleOrParentRecords.remove(id) == this)
}