package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.TaskHierarchyJson


class RemoteTaskHierarchyRecord : RemoteRecord {

    companion object {

        const val TASK_HIERARCHIES = "taskHierarchies"
    }

    val id: String

    private val remoteProjectRecord: RemoteProjectRecord<*>

    override val createObject: TaskHierarchyJson

    override val key get() = remoteProjectRecord.childKey + "/" + TASK_HIERARCHIES + "/" + id

    val startTime get() = createObject.startTime

    var endTime
        get() = createObject.endTime
        set(value) {
            if (value == createObject.endTime)
                return

            createObject.endTime = value
            addValue("$key/endTime", value)
        }

    val parentTaskId get() = createObject.parentTaskId

    val childTaskId get() = createObject.childTaskId

    val ordinal get() = createObject.ordinal

    constructor(id: String, remoteProjectRecord: RemoteProjectRecord<*>, taskHierarchyJson: TaskHierarchyJson) : super(false) {
        this.id = id
        this.remoteProjectRecord = remoteProjectRecord
        createObject = taskHierarchyJson
    }

    constructor(remoteProjectRecord: RemoteProjectRecord<*>, taskHierarchyJson: TaskHierarchyJson) : super(true) {
        id = remoteProjectRecord.getTaskHierarchyRecordId()
        this.remoteProjectRecord = remoteProjectRecord
        createObject = taskHierarchyJson
    }

    fun setOrdinal(ordinal: Double) {
        if (ordinal == createObject.ordinal)
            return

        createObject.ordinal = ordinal
        addValue("$key/ordinal", ordinal)
    }

    override fun deleteFromParent() = check(remoteProjectRecord.remoteTaskHierarchyRecords.remove(id) == this)
}
