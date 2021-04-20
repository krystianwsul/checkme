package com.krystianwsul.common.firebase.models.taskhierarchy

import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.records.taskhierarchy.NestedTaskHierarchyRecord
import com.krystianwsul.common.utils.TaskHierarchyKey


class NestedTaskHierarchy(
        override val childTask: Task<*>,
        override val taskHierarchyRecord: NestedTaskHierarchyRecord,
) : TaskHierarchy(childTask.project) {

    override val childTaskKey get() = childTask.taskKey
    override val childTaskId get() = childTaskKey.taskId

    override val taskHierarchyKey by lazy { TaskHierarchyKey.Nested(childTaskKey, taskHierarchyRecord.id) }

    override fun invalidateTasks() {
        parentTask.invalidateChildTaskHierarchies()
        childTask.invalidateIntervals()
    }

    override fun deleteFromParent() = childTask.deleteNestedTaskHierarchy(this)

    override fun fixOffsets() {}
}
