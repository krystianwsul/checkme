package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.TaskHierarchyRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


abstract class TaskHierarchy<T : ProjectType>(private val project: Project<T>) : TaskParentEntry {

    companion object {

        // todo after flipping this, remove all code for creating ProjectTaskHierarchies
        const val WRITE_NESTED_TASK_HIERARCHIES = false
    }

    protected abstract val taskHierarchyRecord: TaskHierarchyRecord<*>

    final override val startExactTimeStamp by lazy { ExactTimeStamp.Local(taskHierarchyRecord.startTime) }

    final override val startExactTimeStampOffset by lazy {
        taskHierarchyRecord.run { ExactTimeStamp.Offset.fromOffset(startTime, startTimeOffset) }
    }

    final override val endExactTimeStamp get() = taskHierarchyRecord.endTime?.let { ExactTimeStamp.Local(it) }

    final override val endExactTimeStampOffset
        get() = taskHierarchyRecord.endTime?.let {
            ExactTimeStamp.Offset.fromOffset(it, taskHierarchyRecord.endTimeOffset)
        }

    val parentTaskKey by lazy { TaskKey(project.projectKey, taskHierarchyRecord.parentTaskId) }
    abstract val childTaskKey: TaskKey

    val id by lazy { taskHierarchyRecord.id }

    val parentTask by lazy { project.getTaskForce(parentTaskId) }
    abstract val childTask: Task<T>

    val parentTaskId by lazy { taskHierarchyRecord.parentTaskId }
    abstract val childTaskId: String

    val taskHierarchyKey by lazy { TaskHierarchyKey(project.projectKey, taskHierarchyRecord.id) }

    final override fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrentOffset(endExactTimeStamp)

        taskHierarchyRecord.endTime = endExactTimeStamp.long
        taskHierarchyRecord.endTimeOffset = endExactTimeStamp.offset

        invalidateTasks()
    }

    final override fun clearEndExactTimeStamp(now: ExactTimeStamp.Local) {
        requireNotCurrent(now)

        taskHierarchyRecord.endTime = null
        taskHierarchyRecord.endTimeOffset = null

        invalidateTasks()
    }

    fun invalidateTasks() { // todo taskhierarchy write
        parentTask.invalidateChildTaskHierarchies()
        childTask.invalidateParentTaskHierarchies()
    }

    protected abstract fun deleteFromParent()

    fun delete() {
        deleteFromParent()

        taskHierarchyRecord.delete()
    }

    override fun toString() = super.toString() + ", taskHierarchyKey: $taskHierarchyKey, startExactTimeStamp: $startExactTimeStamp, endExactTimeStamp: $endExactTimeStamp, parentTaskKey: $parentTaskKey, childTaskKey: $childTaskKey"

    abstract fun fixOffsets()
}
