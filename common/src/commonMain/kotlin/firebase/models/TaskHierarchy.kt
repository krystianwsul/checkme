package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.RemoteTaskHierarchyRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


class TaskHierarchy<T : ProjectType>(
        private val project: Project<T>,
        private val remoteTaskHierarchyRecord: RemoteTaskHierarchyRecord
) : Current {

    override val startExactTimeStamp by lazy { ExactTimeStamp(remoteTaskHierarchyRecord.startTime) }
    override val endExactTimeStamp get() = remoteTaskHierarchyRecord.endTime?.let { ExactTimeStamp(it) }

    val parentTaskKey by lazy { TaskKey(project.id, remoteTaskHierarchyRecord.parentTaskId) }
    val childTaskKey by lazy { TaskKey(project.id, remoteTaskHierarchyRecord.childTaskId) }

    val id by lazy { remoteTaskHierarchyRecord.id }

    val parentTask by lazy { project.getTaskForce(parentTaskId) }
    val childTask by lazy { project.getTaskForce(childTaskId) }

    val parentTaskId by lazy { remoteTaskHierarchyRecord.parentTaskId }
    val childTaskId by lazy { remoteTaskHierarchyRecord.childTaskId }

    var ordinal: Double
        get() = remoteTaskHierarchyRecord.ordinal ?: remoteTaskHierarchyRecord.startTime.toDouble()
        set(ordinal) = remoteTaskHierarchyRecord.setOrdinal(ordinal)

    val taskHierarchyKey by lazy { TaskHierarchyKey(project.id, remoteTaskHierarchyRecord.id) }

    fun setEndExactTimeStamp(now: ExactTimeStamp) {
        requireCurrent(now)

        remoteTaskHierarchyRecord.endTime = now.long
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        requireNotCurrent(now)

        remoteTaskHierarchyRecord.endTime = null
    }

    fun delete() {
        project.deleteTaskHierarchy(this)

        remoteTaskHierarchyRecord.delete()
    }
}
