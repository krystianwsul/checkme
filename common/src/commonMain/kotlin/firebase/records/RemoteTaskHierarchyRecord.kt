package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.TaskHierarchyJson


class RemoteTaskHierarchyRecord(
        create: Boolean,
        val id: String,
        private val remoteProjectRecord: RemoteProjectRecord<*, *>,
        override val createObject: TaskHierarchyJson
) : RemoteRecord(create) {

    companion object {

        const val TASK_HIERARCHIES = "taskHierarchies"
    }

    override val key get() = remoteProjectRecord.childKey + "/" + TASK_HIERARCHIES + "/" + id

    val startTime get() = createObject.startTime

    val parentTaskId get() = createObject.parentTaskId

    val childTaskId get() = createObject.childTaskId

    val ordinal get() = createObject.ordinal

    constructor(
            id: String,
            remoteProjectRecord: RemoteProjectRecord<*, *>,
            taskHierarchyJson: TaskHierarchyJson
    ) : this(false, id, remoteProjectRecord, taskHierarchyJson)

    constructor(
            remoteProjectRecord: RemoteProjectRecord<*, *>,
            taskHierarchyJson: TaskHierarchyJson
    ) : this(
            true,
            remoteProjectRecord.getTaskHierarchyRecordId(),
            remoteProjectRecord,
            taskHierarchyJson
    )

    var endTime by Committer(createObject::endTime)

    fun setOrdinal(ordinal: Double) = setProperty(createObject::ordinal, ordinal)

    override fun deleteFromParent() = check(remoteProjectRecord.remoteTaskHierarchyRecords.remove(id) == this)
}
