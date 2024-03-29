package com.krystianwsul.common.firebase.records.noscheduleorparent

import com.krystianwsul.common.firebase.json.noscheduleorparent.ProjectNoScheduleOrParentJson
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.utils.ProjectKey

class ProjectNoScheduleOrParentRecord(
    private val projectTaskRecord: ProjectTaskRecord,
    private val projectNoScheduleOrParentJson: ProjectNoScheduleOrParentJson,
    id: String,
) : NoScheduleOrParentRecord(projectTaskRecord, projectNoScheduleOrParentJson, id, false) {

    override val createObject = projectNoScheduleOrParentJson

    override var startTimeOffset by Committer(projectNoScheduleOrParentJson::startTimeOffset)

    override fun deleteFromParent() = check(projectTaskRecord.noScheduleOrParentRecords.remove(id) == this)

    override fun updateProject(projectKey: ProjectKey<*>) = throw UnsupportedOperationException()
}