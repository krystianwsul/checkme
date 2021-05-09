package com.krystianwsul.common.firebase.records.taskhierarchy

import com.krystianwsul.common.firebase.json.taskhierarchies.ProjectTaskHierarchyJson
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.utils.TaskHierarchyId


class ProjectTaskHierarchyRecord(
    id: TaskHierarchyId,
    private val projectRecord: ProjectRecord<*>,
    createObject: ProjectTaskHierarchyJson,
) : TaskHierarchyRecord<ProjectTaskHierarchyJson>(false, id, createObject) {

    override val key get() = projectRecord.childKey + "/" + TASK_HIERARCHIES + "/" + id

    override var startTimeOffset by Committer(createObject::startTimeOffset)

    override val childTaskId get() = createObject.childTaskId

    override fun deleteFromParent() = check(projectRecord.taskHierarchyRecords.remove(id) == this)
}
