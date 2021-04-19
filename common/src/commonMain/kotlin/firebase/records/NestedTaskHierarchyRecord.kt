package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.NestedTaskHierarchyJson


class NestedTaskHierarchyRecord(
        create: Boolean,
        id: String,
        private val taskRecord: TaskRecord<*>,
        createObject: NestedTaskHierarchyJson,
) : TaskHierarchyRecord<NestedTaskHierarchyJson>(create, id, createObject) {

    override val key get() = taskRecord.key + "/" + TASK_HIERARCHIES + "/" + id

    override val childTaskId get() = taskRecord.id

    constructor(
            id: String,
            taskRecord: TaskRecord<*>,
            taskHierarchyJson: NestedTaskHierarchyJson,
    ) : this(false, id, taskRecord, taskHierarchyJson)

    constructor(
            taskRecord: TaskRecord<*>,
            taskHierarchyJson: NestedTaskHierarchyJson,
    ) : this(
            true,
            taskRecord.newTaskHierarchyRecordId(),
            taskRecord,
            taskHierarchyJson,
    )

    override fun deleteFromParent() {
        // check(taskRecord.taskHierarchyRecords.remove(id) == this) todo taskhierarchy
    }
}
