package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.TaskHierarchy
import com.krystianwsul.common.firebase.records.RemoteTaskHierarchyRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


class RemoteTaskHierarchy<T : RemoteCustomTimeId>(
        private val remoteProject: RemoteProject<T>,
        private val remoteTaskHierarchyRecord: RemoteTaskHierarchyRecord
) : TaskHierarchy() {

    override val startExactTimeStamp get() = ExactTimeStamp(remoteTaskHierarchyRecord.startTime)

    override val parentTaskKey by lazy { TaskKey(remoteProject.id, remoteTaskHierarchyRecord.parentTaskId) }

    override val childTaskKey by lazy { TaskKey(remoteProject.id, remoteTaskHierarchyRecord.childTaskId) }

    val id by lazy { remoteTaskHierarchyRecord.id }

    override val parentTask by lazy { remoteProject.getRemoteTaskForce(parentTaskId) }

    override val childTask by lazy { remoteProject.getRemoteTaskForce(childTaskId) }

    val parentTaskId by lazy { remoteTaskHierarchyRecord.parentTaskId }
    val childTaskId by lazy { remoteTaskHierarchyRecord.childTaskId }

    override var ordinal: Double
        get() = remoteTaskHierarchyRecord.ordinal ?: remoteTaskHierarchyRecord.startTime.toDouble()
        set(ordinal) = remoteTaskHierarchyRecord.setOrdinal(ordinal)

    override val taskHierarchyKey by lazy { TaskHierarchyKey.Remote(remoteProject.id, remoteTaskHierarchyRecord.id) }

    public override fun getEndExactTimeStamp() = remoteTaskHierarchyRecord.endTime?.let { ExactTimeStamp(it) }

    override fun setEndExactTimeStamp(now: ExactTimeStamp) {
        check(current(now))

        remoteTaskHierarchyRecord.endTime = now.long
    }

    override fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        check(!current(now))

        remoteTaskHierarchyRecord.endTime = null
    }

    override fun delete() {
        remoteProject.deleteTaskHierarchy(this)

        remoteTaskHierarchyRecord.delete()
    }
}
