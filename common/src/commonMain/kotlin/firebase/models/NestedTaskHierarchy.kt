package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.NestedTaskHierarchyRecord
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskHierarchyKey


class NestedTaskHierarchy<T : ProjectType>(
        override val childTask: Task<T>,
        override val taskHierarchyRecord: NestedTaskHierarchyRecord,
) : TaskHierarchy<T>(childTask.project) {

    override val childTaskKey get() = childTask.taskKey
    override val childTaskId get() = childTaskKey.taskId

    override val taskHierarchyKey by lazy { TaskHierarchyKey.Nested(childTaskKey, taskHierarchyRecord.id) }

    override fun invalidateTasks() = parentTask.invalidateChildTaskHierarchies()

    override fun deleteFromParent() = childTask.deleteNestedTaskHierarchy(this)

    override fun fixOffsets() {}
}
