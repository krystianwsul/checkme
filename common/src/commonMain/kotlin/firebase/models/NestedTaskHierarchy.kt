package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.records.NestedTaskHierarchyRecord
import com.krystianwsul.common.utils.ProjectType


class NestedTaskHierarchy<T : ProjectType>(
        override val childTask: Task<T>,
        taskHierarchyRecord: NestedTaskHierarchyRecord,
) : TaskHierarchy<T, NestedTaskHierarchyJson>(childTask.project, taskHierarchyRecord) {

    override val childTaskKey get() = childTask.taskKey
    override val childTaskId get() = childTaskKey.taskId

    override fun deleteFromParent() {
        // todo taskhierarchy childTask.deleteTaskHierarchy(this)
    }

    override fun fixOffsets() {}
}
