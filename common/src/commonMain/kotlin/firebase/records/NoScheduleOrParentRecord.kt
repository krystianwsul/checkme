package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.models.ProjectIdOwner
import com.krystianwsul.common.firebase.records.schedule.ProjectHelper
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.utils.ProjectKey

class NoScheduleOrParentRecord(
        // todo task ids update stuff on creation
        private val taskRecord: TaskRecord,
        override val createObject: NoScheduleOrParentJson,
        _id: String?,
        val projectHelper: ProjectHelper,
) : RemoteRecord(_id == null), ProjectIdOwner {

    companion object {

        const val NO_SCHEDULE_OR_PARENT = "noScheduleOrParent"
    }

    val id = _id ?: taskRecord.newNoScheduleOrParentRecordId()

    override val key = "${taskRecord.key}/$NO_SCHEDULE_OR_PARENT/$id"

    val startTime = createObject.startTime
    var startTimeOffset by Committer(createObject::startTimeOffset)

    var endTime by Committer(createObject::endTime)
    var endTimeOffset by Committer(createObject::endTimeOffset)

    val projectId get() = projectHelper.getProjectId(createObject)

    override fun deleteFromParent() = check(taskRecord.noScheduleOrParentRecords.remove(id) == this)

    override fun updateProject(projectKey: ProjectKey<*>) =
            projectHelper.setProjectId(createObject, projectKey.key, ::addValue)
}