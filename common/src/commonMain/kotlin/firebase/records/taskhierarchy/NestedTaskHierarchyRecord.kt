package com.krystianwsul.common.firebase.records.taskhierarchy

import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.records.task.TaskRecord


class NestedTaskHierarchyRecord(
        create: Boolean,
        id: String,
        private val taskRecord: TaskRecord,
        createObject: NestedTaskHierarchyJson,
) : TaskHierarchyRecord<NestedTaskHierarchyJson>(create, id, createObject) {

    override val key get() = taskRecord.key + "/" + TASK_HIERARCHIES + "/" + id

    override val startTimeOffset: Double get() = createObject.startTimeOffset

    override val childTaskId get() = taskRecord.id

    constructor(
            id: String,
            taskRecord: TaskRecord,
            taskHierarchyJson: NestedTaskHierarchyJson,
    ) : this(false, id, taskRecord, taskHierarchyJson)

    constructor(
            taskRecord: TaskRecord,
            taskHierarchyJson: NestedTaskHierarchyJson,
    ) : this(
            true,
            taskRecord.newTaskHierarchyRecordId(),
            taskRecord,
            taskHierarchyJson,
    )

    override fun deleteFromParent() = check(taskRecord.taskHierarchyRecords.remove(id) == this)
}
