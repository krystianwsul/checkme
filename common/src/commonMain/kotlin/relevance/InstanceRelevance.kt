package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.*


class InstanceRelevance(val instance: Instance<*>) {

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

    fun setRemoteRelevant(
            remoteCustomTimeRelevances: Map<CustomTimeKey<*>, RemoteCustomTimeRelevance>,
            remoteProjectRelevances: Map<ProjectKey<*>, RemoteProjectRelevance>
    ) {
        check(relevant)

        val remoteProject = instance.project

        val pair = instance.instanceDateTime
                .time
                .timePair
                .customTimeKey
        if (pair != null)
            remoteCustomTimeRelevances.getValue(pair).setRelevant()

        remoteProjectRelevances.getValue(remoteProject.id).setRelevant()

        (instance.scheduleCustomTimeKey)?.let {
            remoteCustomTimeRelevances.getValue(it).setRelevant()
        }
    }
}
