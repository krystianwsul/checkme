package com.krystianwsul.checkme.domainmodel.relevance

import com.krystianwsul.common.domain.TaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


class TaskHierarchyRelevance(val taskHierarchy: TaskHierarchy) {

    var relevant = false
        private set

    fun setRelevant(taskRelevances: Map<TaskKey, TaskRelevance>, taskHierarchyRelevances: Map<TaskHierarchyKey, TaskHierarchyRelevance>, instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>, now: ExactTimeStamp) {
        if (relevant)
            return

        relevant = true

        listOf(taskHierarchy.parentTaskKey, taskHierarchy.childTaskKey).forEach {
            taskRelevances.getValue(it).setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now)
        }
    }
}
