package com.krystianwsul.common.firebase.models.taskhierarchy

import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.taskhierarchy.NestedTaskHierarchyRecord
import com.krystianwsul.common.utils.TaskHierarchyKey


class NestedTaskHierarchy(
    override val childTask: Task,
    taskHierarchyRecord: NestedTaskHierarchyRecord,
    parentTaskDelegateFactory: ParentTaskDelegate.Factory,
) : TaskHierarchy(childTask.clearableInvalidatableManager, taskHierarchyRecord, parentTaskDelegateFactory) {

    override val childTaskKey get() = childTask.taskKey

    override val taskHierarchyKey by lazy { TaskHierarchyKey.Nested(childTaskKey, taskHierarchyRecord.id) }

    override fun invalidateTasks() {
        parentTask.invalidateChildTaskHierarchies()
        childTask.invalidateIntervals()
    }

    override fun deleteFromParent() {
        ProjectRootTaskIdTracker.checkTracking()

        childTask.deleteNestedTaskHierarchy(this)
    }

    override fun fixOffsets() {}
}
