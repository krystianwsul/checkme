package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.taskhierarchies.ProjectTaskHierarchyJson


class ProjectTaskHierarchyRecord(
        create: Boolean,
        id: String,
        private val projectRecord: ProjectRecord<*>,
        createObject: ProjectTaskHierarchyJson,
) : TaskHierarchyRecord<ProjectTaskHierarchyJson>(create, id, createObject) {

    override val key get() = projectRecord.childKey + "/" + TASK_HIERARCHIES + "/" + id

    override var startTimeOffset by Committer(createObject::startTimeOffset)

    override val childTaskId get() = createObject.childTaskId

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
            projectRecord.getProjectTaskHierarchyRecordId(),
            projectRecord,
            taskHierarchyJson,
    )

    override fun deleteFromParent() = check(projectRecord.taskHierarchyRecords.remove(id) == this)
}
