package com.krystianwsul.common.firebase.models.taskhierarchy

import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.records.taskhierarchy.ProjectTaskHierarchyRecord
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


class ProjectTaskHierarchy(
        private val project: Project<*>,
        override val taskHierarchyRecord: ProjectTaskHierarchyRecord,
) : TaskHierarchy(project) {

    override val childTaskKey by lazy { TaskKey.Project(project.projectKey, taskHierarchyRecord.childTaskId) } // todo task after model
    override val childTask by lazy { project.getTaskForce(childTaskId) }
    override val childTaskId by lazy { taskHierarchyRecord.childTaskId }

    override val taskHierarchyKey by lazy { TaskHierarchyKey.Project(project.projectKey, taskHierarchyRecord.id) }

    override fun invalidateTasks() {
        parentTask.invalidateChildTaskHierarchies()
        childTask.invalidateProjectParentTaskHierarchies()
    }

    override fun deleteFromParent() = project.deleteTaskHierarchy(this)

    override fun fixOffsets() {
        if (taskHierarchyRecord.startTimeOffset == null)
            taskHierarchyRecord.startTimeOffset = startExactTimeStamp.offset

        endExactTimeStamp?.let {
            if (taskHierarchyRecord.endTimeOffset == null) taskHierarchyRecord.endTimeOffset = it.offset
        }
    }
}
