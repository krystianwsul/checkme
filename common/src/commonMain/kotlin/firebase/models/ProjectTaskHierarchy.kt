package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.ProjectTaskHierarchyRecord
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey


class ProjectTaskHierarchy<T : ProjectType>(
        private val project: Project<T>,
        override val taskHierarchyRecord: ProjectTaskHierarchyRecord,
) : TaskHierarchy<T>(project) {

    override val childTaskKey by lazy { TaskKey(project.projectKey, taskHierarchyRecord.childTaskId) }

    override val childTask by lazy { project.getTaskForce(childTaskId) }

    override val childTaskId by lazy { taskHierarchyRecord.childTaskId }

    override fun deleteFromParent() = project.deleteTaskHierarchy(this)

    override fun fixOffsets() {
        if (taskHierarchyRecord.startTimeOffset == null)
            taskHierarchyRecord.startTimeOffset = startExactTimeStamp.offset

        endExactTimeStamp?.let {
            if (taskHierarchyRecord.endTimeOffset == null) taskHierarchyRecord.endTimeOffset = it.offset
        }
    }
}
