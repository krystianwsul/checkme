package com.krystianwsul.common.firebase.records.noscheduleorparent

import com.krystianwsul.common.firebase.json.noscheduleorparent.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.records.schedule.ProjectHelper
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.ProjectKey

class RootNoScheduleOrParentRecord(
    private val rootTaskRecord: RootTaskRecord,
    createObject: NoScheduleOrParentJson,
    _id: String?,
    private val projectHelper: ProjectHelper,
) : NoScheduleOrParentRecord(rootTaskRecord, createObject, _id) {

    override val projectId get() = projectHelper.getProjectId(createObject)

    override fun deleteFromParent() = check(rootTaskRecord.noScheduleOrParentRecords.remove(id) == this)

    override fun updateProject(projectKey: ProjectKey<*>) =
        projectHelper.setProjectId(createObject, projectKey.key, ::addValue)
}