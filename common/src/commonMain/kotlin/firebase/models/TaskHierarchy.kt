package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.TaskHierarchyRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


class TaskHierarchy<T : ProjectType>(
        private val project: Project<T>,
        private val taskHierarchyRecord: TaskHierarchyRecord
) : Current {

    override val startExactTimeStamp by lazy { ExactTimeStamp(taskHierarchyRecord.startTime) }
    override val endExactTimeStamp get() = taskHierarchyRecord.endTime?.let { ExactTimeStamp(it) }

    val parentTaskKey by lazy { TaskKey(project.projectKey, taskHierarchyRecord.parentTaskId) }
    val childTaskKey by lazy { TaskKey(project.projectKey, taskHierarchyRecord.childTaskId) }

    val id by lazy { taskHierarchyRecord.id }

    val parentTask by lazy { project.getTaskForce(parentTaskId) }
    val childTask by lazy { project.getTaskForce(childTaskId) }

    val parentTaskId by lazy { taskHierarchyRecord.parentTaskId }
    val childTaskId by lazy { taskHierarchyRecord.childTaskId }

    var ordinal: Double
        get() = taskHierarchyRecord.ordinal ?: taskHierarchyRecord.startTime.toDouble()
        set(ordinal) = taskHierarchyRecord.setOrdinal(ordinal)

    val taskHierarchyKey by lazy { TaskHierarchyKey(project.projectKey, taskHierarchyRecord.id) }

    fun setEndExactTimeStamp(now: ExactTimeStamp) {
        requireCurrent(now)

        taskHierarchyRecord.endTime = now.long

        invalidateTasks()
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        requireNotCurrent(now)

        taskHierarchyRecord.endTime = null

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
}
