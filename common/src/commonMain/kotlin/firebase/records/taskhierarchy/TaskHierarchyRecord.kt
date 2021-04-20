package com.krystianwsul.common.firebase.records.taskhierarchy

import com.krystianwsul.common.firebase.json.taskhierarchies.TaskHierarchyJson
import com.krystianwsul.common.firebase.records.RemoteRecord


abstract class TaskHierarchyRecord<T : TaskHierarchyJson>(
        create: Boolean,
        val id: String,
        final override val createObject: T,
) : RemoteRecord(create) {

    companion object {

        const val TASK_HIERARCHIES = "taskHierarchies"
    }

    val startTime get() = createObject.startTime
    abstract val startTimeOffset: Double?

    var endTime by Committer(createObject::endTime)
    var endTimeOffset by Committer(createObject::endTimeOffset)

    val parentTaskId get() = createObject.parentTaskId
    abstract val childTaskId: String
}
