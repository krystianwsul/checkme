package com.krystianwsul.checkme.domainmodel.relevance

import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.utils.TaskHierarchyKey
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.TaskKey


class InstanceRelevance(val instance: Instance) {

    var relevant = false
        private set

    fun setRelevant(taskRelevances: Map<TaskKey, TaskRelevance>, taskHierarchyRelevances: Map<TaskHierarchyKey, TaskHierarchyRelevance>, instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>, now: ExactTimeStamp) {
        if (relevant) return

        relevant = true

        // set task relevant
        taskRelevances.getValue(instance.taskKey).setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now)

        // set parent instance relevant
        if (!instance.isRootInstance(now)) {
            val parentInstance = instance.getParentInstance(now)!!

            val parentInstanceKey = parentInstance.instanceKey

            if (!instanceRelevances.containsKey(parentInstanceKey))
                instanceRelevances[parentInstanceKey] = InstanceRelevance(parentInstance)

            instanceRelevances[parentInstanceKey]!!.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now)
        }

        // set child instances relevant
        instance.getChildInstances(now)
                .map { (instance) ->
                    val instanceKey = instance.instanceKey

                    if (!instanceRelevances.containsKey(instanceKey))
                        instanceRelevances[instanceKey] = InstanceRelevance(instance)

                    instanceRelevances[instanceKey]!!
                }
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }
    }

    fun setRemoteRelevant(remoteCustomTimeRelevances: Map<Pair<String, RemoteCustomTimeId>, RemoteCustomTimeRelevance>, remoteProjectRelevances: Map<String, RemoteProjectRelevance>) {
        check(relevant)

        val pair = instance.customTimeKey
        val remoteProject = instance.project
        if (pair != null)
            remoteCustomTimeRelevances.getValue(pair).setRelevant()

        remoteProjectRelevances.getValue(remoteProject.id).setRelevant()

        (instance.scheduleCustomTimeKey)?.let {
            remoteCustomTimeRelevances.getValue(Pair(it.remoteProjectId, it.remoteCustomTimeId)).setRelevant()
        }
    }
}
