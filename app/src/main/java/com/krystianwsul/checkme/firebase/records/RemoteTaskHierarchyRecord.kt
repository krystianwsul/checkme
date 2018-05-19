package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson

import junit.framework.Assert

class RemoteTaskHierarchyRecord : RemoteRecord {

    companion object {

        const val TASK_HIERARCHIES = "taskHierarchies"
    }

    val id: String

    private val remoteProjectRecord: RemoteProjectRecord

    override val createObject: TaskHierarchyJson

    override val key get() = remoteProjectRecord.key + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + TASK_HIERARCHIES + "/" + id

    val startTime get() = createObject.startTime

    val endTime get() = createObject.endTime

    val parentTaskId get() = createObject.parentTaskId

    val childTaskId get() = createObject.childTaskId

    val ordinal get() = createObject.ordinal

    constructor(id: String, remoteProjectRecord: RemoteProjectRecord, taskHierarchyJson: TaskHierarchyJson) : super(false) {
        this.id = id
        this.remoteProjectRecord = remoteProjectRecord
        createObject = taskHierarchyJson
    }

    constructor(remoteProjectRecord: RemoteProjectRecord, taskHierarchyJson: TaskHierarchyJson) : super(true) {
        id = DatabaseWrapper.getTaskHierarchyRecordId(remoteProjectRecord.id)
        this.remoteProjectRecord = remoteProjectRecord
        createObject = taskHierarchyJson
    }

    fun setEndTime(endTime: Long) {
        Assert.assertTrue(createObject.endTime == null)

        createObject.endTime = endTime
        addValue("$key/endTime", endTime)
    }

    fun setOrdinal(ordinal: Double) {
        createObject.ordinal = ordinal
        addValue("$key/ordinal", ordinal)
    }
}
