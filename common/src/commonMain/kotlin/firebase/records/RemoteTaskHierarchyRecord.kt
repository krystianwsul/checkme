package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.TaskHierarchyJson


class RemoteTaskHierarchyRecord(
        create: Boolean,
        val id: String,
        private val projectRecord: ProjectRecord<*>,
        override val createObject: TaskHierarchyJson
) : RemoteRecord(create) {

    companion object {

        const val TASK_HIERARCHIES = "taskHierarchies"
    }

    override val key get() = projectRecord.childKey + "/" + TASK_HIERARCHIES + "/" + id

    val startTime get() = createObject.startTime

    val parentTaskId get() = createObject.parentTaskId

    val childTaskId get() = createObject.childTaskId

    val ordinal get() = createObject.ordinal

    constructor(
            id: String,
            projectRecord: ProjectRecord<*>,
            taskHierarchyJson: TaskHierarchyJson
    ) : this(false, id, projectRecord, taskHierarchyJson)

    constructor(
            projectRecord: ProjectRecord<*>,
            taskHierarchyJson: TaskHierarchyJson
    ) : this(
            true,
            projectRecord.getTaskHierarchyRecordId(),
            projectRecord,
            taskHierarchyJson
    )

    var endTime by Committer(createObject::endTime)

    fun setOrdinal(ordinal: Double) = setProperty(createObject::ordinal, ordinal)

    override fun deleteFromParent() = check(projectRecord.remoteTaskHierarchyRecords.remove(id) == this)
}
