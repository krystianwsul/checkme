package com.krystianwsul.checkme.domainmodel.relevance

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TaskHierarchy
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskHierarchyKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp


class TaskHierarchyRelevance(private val domainFactory: DomainFactory, val taskHierarchy: TaskHierarchy) {

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
