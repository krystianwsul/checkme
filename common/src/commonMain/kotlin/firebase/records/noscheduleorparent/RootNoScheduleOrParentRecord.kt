package com.krystianwsul.common.firebase.records.noscheduleorparent

import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.ProjectKey

class RootNoScheduleOrParentRecord(
    private val rootTaskRecord: RootTaskRecord,
    private val rootNoScheduleOrParentJson: RootNoScheduleOrParentJson,
    _id: String?,
) : NoScheduleOrParentRecord(rootTaskRecord, rootNoScheduleOrParentJson, _id) {

    override val createObject = rootNoScheduleOrParentJson

    val projectId get() = rootNoScheduleOrParentJson.projectId!!

    override fun deleteFromParent() = check(rootTaskRecord.noScheduleOrParentRecords.remove(id) == this)

    override fun updateProject(projectKey: ProjectKey<*>) {
        if (rootNoScheduleOrParentJson.projectId == projectId) return

        rootNoScheduleOrParentJson.projectId = projectId

        addValue("projectId", projectId)
    }
}