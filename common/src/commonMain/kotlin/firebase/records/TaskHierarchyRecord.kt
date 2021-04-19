package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.ProjectTaskHierarchyJson


class TaskHierarchyRecord(
        create: Boolean,
        val id: String,
        private val projectRecord: ProjectRecord<*>,
        override val createObject: ProjectTaskHierarchyJson,
) : RemoteRecord(create) {

    companion object {

        const val TASK_HIERARCHIES = "taskHierarchies"
    }

    override val key get() = projectRecord.childKey + "/" + TASK_HIERARCHIES + "/" + id

    val startTime get() = createObject.startTime
    var startTimeOffset by Committer(createObject::startTimeOffset)

    var endTime by Committer(createObject::endTime)
    var endTimeOffset by Committer(createObject::endTimeOffset)

    val parentTaskId get() = createObject.parentTaskId

    val childTaskId get() = createObject.childTaskId

    constructor(
            id: String,
            projectRecord: ProjectRecord<*>,
            taskHierarchyJson: ProjectTaskHierarchyJson,
    ) : this(false, id, projectRecord, taskHierarchyJson)

    constructor(
            projectRecord: ProjectRecord<*>,
            taskHierarchyJson: ProjectTaskHierarchyJson,
    ) : this(
            true,
            projectRecord.getTaskHierarchyRecordId(),
            projectRecord,
            taskHierarchyJson
    )

    override fun deleteFromParent() = check(projectRecord.taskHierarchyRecords.remove(id) == this)
}
