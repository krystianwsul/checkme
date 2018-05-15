package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.TaskHierarchyKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

abstract class TaskHierarchy protected constructor(protected val mDomainFactory: DomainFactory) {

    abstract val startExactTimeStamp: ExactTimeStamp

    protected abstract fun getEndExactTimeStamp(): ExactTimeStamp?

    abstract var ordinal: Double

    abstract val parentTaskKey: TaskKey

    abstract val childTaskKey: TaskKey

    abstract val parentTask: Task

    abstract val childTask: Task

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

    abstract fun delete()
}
