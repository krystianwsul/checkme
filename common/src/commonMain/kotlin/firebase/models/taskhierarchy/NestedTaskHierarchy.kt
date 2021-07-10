package com.krystianwsul.common.firebase.models.taskhierarchy

import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.taskhierarchy.NestedTaskHierarchyRecord
import com.krystianwsul.common.utils.TaskHierarchyKey


class NestedTaskHierarchy(
    override val childTask: Task,
    override val taskHierarchyRecord: NestedTaskHierarchyRecord,
    private val parentTaskDelegate: ParentTaskDelegate,
) : TaskHierarchy(childTask.clearableInvalidatableManager, parentTaskDelegate) {

    override val childTaskKey get() = childTask.taskKey
    override val childTaskId get() = childTaskKey.taskId

    override val taskHierarchyKey by lazy { TaskHierarchyKey.Nested(childTaskKey, taskHierarchyRecord.id) }

    override fun invalidateTasks() {
        parentTask.invalidateChildTaskHierarchies()
        childTask.invalidateIntervals()
    }

    fun deleteFromParentTask() {
        parentTaskDelegate.removeRootChildTaskFromParent(parentTask, childTask)
    }

    override fun deleteFromParent() {
        deleteFromParentTask()

        childTask.deleteNestedTaskHierarchy(this)
    }

    override fun fixOffsets() {}
}
