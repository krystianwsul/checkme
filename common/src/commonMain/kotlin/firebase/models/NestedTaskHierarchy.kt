package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.NestedTaskHierarchyRecord
import com.krystianwsul.common.utils.ProjectType


class NestedTaskHierarchy<T : ProjectType>(
        override val childTask: Task<T>,
        override val taskHierarchyRecord: NestedTaskHierarchyRecord,
) : TaskHierarchy<T>(childTask.project) {

    override val childTaskKey get() = childTask.taskKey
    override val childTaskId get() = childTaskKey.taskId

    override fun deleteFromParent() = childTask.deleteTaskHierarchy(this)

    override fun fixOffsets() {}
}
