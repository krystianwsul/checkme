package com.krystianwsul.common.firebase.models.taskhierarchy

import com.krystianwsul.common.firebase.models.TaskParentEntry
import com.krystianwsul.common.firebase.models.cache.ClearableInvalidatableManager
import com.krystianwsul.common.firebase.models.cache.InvalidatableCache
import com.krystianwsul.common.firebase.models.cache.invalidatableCache
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.taskhierarchy.TaskHierarchyRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


sealed class TaskHierarchy(
    clearableInvalidatableManager: ClearableInvalidatableManager,
    private val taskHierarchyRecord: TaskHierarchyRecord<*>,
    parentTaskDelegateFactory: ParentTaskDelegate.Factory,
) : TaskParentEntry {

    private val parentTaskDelegate = parentTaskDelegateFactory.newDelegate(taskHierarchyRecord)

    val startExactTimeStamp by lazy { ExactTimeStamp.Local(taskHierarchyRecord.startTime) }

    final override val startExactTimeStampOffset by lazy {
        taskHierarchyRecord.run { ExactTimeStamp.Offset.fromOffset(startTime, startTimeOffset) }
    }

    final override val endExactTimeStamp get() = taskHierarchyRecord.endTime?.let { ExactTimeStamp.Local(it) }

    final override val endExactTimeStampOffset
        get() = taskHierarchyRecord.endTime?.let {
            ExactTimeStamp.Offset.fromOffset(it, taskHierarchyRecord.endTimeOffset)
        }

    val parentTaskKey by lazy { parentTaskDelegate.getTaskKey(taskHierarchyRecord.parentTaskId) }
    abstract val childTaskKey: TaskKey

    val id by lazy { taskHierarchyRecord.id }

    private val parentTaskCache = invalidatableCache<Task>(clearableInvalidatableManager) { invalidatableCache ->
        try {
            val parentTask = parentTaskDelegate.getTask(parentTaskId)

            val removable = parentTask.clearableInvalidatableManager.addInvalidatable(invalidatableCache)

            InvalidatableCache.ValueHolder(parentTask) { removable.remove() }
        } catch (exception: Exception) {
            throw GetParentTaskException(exception)
        }
    }

    private inner class GetParentTaskException(cause: Throwable) : Exception("taskHierarchyKey: $taskHierarchyKey", cause)

    val parentTask by parentTaskCache
    abstract val childTask: Task

    val parentTaskId by lazy { taskHierarchyRecord.parentTaskId }
    abstract val childTaskId: String

    abstract val taskHierarchyKey: TaskHierarchyKey

    override fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrentOffset(endExactTimeStamp)

        taskHierarchyRecord.endTime = endExactTimeStamp.long
        taskHierarchyRecord.endTimeOffset = endExactTimeStamp.offset

        invalidateTasks()
    }

    final override fun clearEndExactTimeStamp() {
        requireDeleted()

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

    override fun toString() =
        super.toString() + ", taskHierarchyKey: $taskHierarchyKey, startExactTimeStamp: $startExactTimeStamp, endExactTimeStamp: $endExactTimeStamp, parentTaskKey: $parentTaskKey, childTaskKey: $childTaskKey"

    abstract fun fixOffsets()
}
