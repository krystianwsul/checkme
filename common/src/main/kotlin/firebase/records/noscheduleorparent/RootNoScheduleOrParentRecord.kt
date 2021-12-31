package com.krystianwsul.common.firebase.records.noscheduleorparent

import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.ProjectKey

class RootNoScheduleOrParentRecord(
    private val rootTaskRecord: RootTaskRecord,
    private val rootNoScheduleOrParentJson: RootNoScheduleOrParentJson,
    id: String,
    create: Boolean,
) : NoScheduleOrParentRecord(rootTaskRecord, rootNoScheduleOrParentJson, id, create) {

    override val createObject = rootNoScheduleOrParentJson

    override val startTimeOffset get() = rootNoScheduleOrParentJson.startTimeOffset

    var projectId by Committer(rootNoScheduleOrParentJson::projectId)
        private set

    override fun deleteFromParent() = check(rootTaskRecord.noScheduleOrParentRecords.remove(id) == this)

    override fun updateProject(projectKey: ProjectKey<*>) {
        if (rootNoScheduleOrParentJson.projectId == projectKey.key) return

        projectId = projectKey.key
    }
}