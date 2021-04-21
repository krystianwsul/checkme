package com.krystianwsul.common.firebase.models.taskhierarchy

import com.krystianwsul.common.firebase.models.TaskParentEntry
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.taskhierarchy.TaskHierarchyRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


abstract class TaskHierarchy(private val project: Project<*>) : TaskParentEntry {

    companion object {

        // todo after flipping this, remove all code for creating ProjectTaskHierarchies
        const val WRITE_NESTED_TASK_HIERARCHIES = false // todo published on 2021.04.20
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

    val parentTaskKey: TaskKey by lazy { TaskKey.Project(project.projectKey, taskHierarchyRecord.parentTaskId) } // todo task model
    abstract val childTaskKey: TaskKey

    val id by lazy { taskHierarchyRecord.id }

    val parentTask by lazy { project.getTaskForce(parentTaskId) }
    abstract val childTask: Task

    val parentTaskId by lazy { taskHierarchyRecord.parentTaskId }
    abstract val childTaskId: String

    abstract val taskHierarchyKey: TaskHierarchyKey

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

    abstract fun invalidateTasks()

    protected abstract fun deleteFromParent()

    fun delete() {
        deleteFromParent()

        taskHierarchyRecord.delete()
    }

    override fun toString() = super.toString() + ", taskHierarchyKey: $taskHierarchyKey, startExactTimeStamp: $startExactTimeStamp, endExactTimeStamp: $endExactTimeStamp, parentTaskKey: $parentTaskKey, childTaskKey: $childTaskKey"

    abstract fun fixOffsets()
}
