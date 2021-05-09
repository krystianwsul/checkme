package com.krystianwsul.common.firebase.records.noscheduleorparent

import com.krystianwsul.common.firebase.json.noscheduleorparent.ProjectNoScheduleOrParentJson
import com.krystianwsul.common.firebase.records.schedule.ProjectHelper
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.utils.ProjectKey

class ProjectNoScheduleOrParentRecord(
    private val projectTaskRecord: ProjectTaskRecord,
    private val projectNoScheduleOrParentJson: ProjectNoScheduleOrParentJson,
    _id: String?,
    private val projectHelper: ProjectHelper,
) : NoScheduleOrParentRecord(projectTaskRecord, projectNoScheduleOrParentJson, _id) {

    override val createObject = projectNoScheduleOrParentJson

    override val projectId get() = projectHelper.getProjectId(createObject)

    override fun deleteFromParent() = check(projectTaskRecord.noScheduleOrParentRecords.remove(id) == this)

    override fun updateProject(projectKey: ProjectKey<*>) =
        projectHelper.setProjectId(createObject, projectKey.key, ::addValue)
}