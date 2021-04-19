package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.ProjectTaskHierarchyRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


class TaskHierarchy<T : ProjectType>(
        private val project: Project<T>,
        private val taskHierarchyRecord: ProjectTaskHierarchyRecord,
) : TaskParentEntry {

    override val startExactTimeStamp by lazy { ExactTimeStamp.Local(taskHierarchyRecord.startTime) }

    override val startExactTimeStampOffset by lazy {
        taskHierarchyRecord.run { ExactTimeStamp.Offset.fromOffset(startTime, startTimeOffset) }
    }

    override val endExactTimeStamp get() = taskHierarchyRecord.endTime?.let { ExactTimeStamp.Local(it) }

    override val endExactTimeStampOffset
        get() = taskHierarchyRecord.endTime?.let {
            ExactTimeStamp.Offset.fromOffset(it, taskHierarchyRecord.endTimeOffset)
        }

    val parentTaskKey by lazy { TaskKey(project.projectKey, taskHierarchyRecord.parentTaskId) }
    val childTaskKey by lazy { TaskKey(project.projectKey, taskHierarchyRecord.childTaskId) }

    val id by lazy { taskHierarchyRecord.id }

    val parentTask by lazy { project.getTaskForce(parentTaskId) }
    val childTask by lazy { project.getTaskForce(childTaskId) }

    val parentTaskId by lazy { taskHierarchyRecord.parentTaskId }
    val childTaskId by lazy { taskHierarchyRecord.childTaskId }

    val taskHierarchyKey by lazy { TaskHierarchyKey(project.projectKey, taskHierarchyRecord.id) }

    override fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrentOffset(endExactTimeStamp)

        taskHierarchyRecord.endTime = endExactTimeStamp.long
        taskHierarchyRecord.endTimeOffset = endExactTimeStamp.offset

        invalidateTasks()
    }

    override fun clearEndExactTimeStamp(now: ExactTimeStamp.Local) {
        requireNotCurrent(now)

        taskHierarchyRecord.endTime = null
        taskHierarchyRecord.endTimeOffset = null

        invalidateTasks()
    }

    fun invalidateTasks() {
        parentTask.invalidateChildTaskHierarchies()
        childTask.invalidateParentTaskHierarchies()
    }

    fun delete() {
        project.deleteTaskHierarchy(this)

        taskHierarchyRecord.delete()
    }

    override fun toString() = super.toString() + ", taskHierarchyKey: $taskHierarchyKey, startExactTimeStamp: $startExactTimeStamp, endExactTimeStamp: $endExactTimeStamp, parentTaskKey: $parentTaskKey, childTaskKey: $childTaskKey"

    fun fixOffsets() {
        if (taskHierarchyRecord.startTimeOffset == null)
            taskHierarchyRecord.startTimeOffset = startExactTimeStamp.offset

        endExactTimeStamp?.let {
            if (taskHierarchyRecord.endTimeOffset == null) taskHierarchyRecord.endTimeOffset = it.offset
        }
    }
}
