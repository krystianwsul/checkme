package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey

abstract class TaskHierarchy {

    abstract val startExactTimeStamp: ExactTimeStamp

    protected abstract fun getEndExactTimeStamp(): ExactTimeStamp?

    abstract var ordinal: Double

    abstract val parentTaskKey: TaskKey

    abstract val childTaskKey: TaskKey

    abstract val parentTask: Task<*, *>

    abstract val childTask: Task<*, *>

    abstract val taskHierarchyKey: TaskHierarchyKey

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val startExactTimeStamp = startExactTimeStamp
        val endExactTimeStamp = getEndExactTimeStamp()

        return startExactTimeStamp <= exactTimeStamp && (endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp)
    }

    fun notDeleted(exactTimeStamp: ExactTimeStamp): Boolean {
        val endExactTimeStamp = getEndExactTimeStamp()

        return endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp
    }

    abstract fun setEndExactTimeStamp(now: ExactTimeStamp)

    abstract fun clearEndExactTimeStamp(now: ExactTimeStamp)

    abstract fun delete()
}
