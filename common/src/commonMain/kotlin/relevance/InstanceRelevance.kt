package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.*


class InstanceRelevance(val instance: Instance<*>) {

    var relevant = false
        private set

    fun setRelevant(
            taskRelevances: Map<TaskKey, TaskRelevance>,
            taskHierarchyRelevances: Map<TaskHierarchyKey, TaskHierarchyRelevance>,
            instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>,
            now: ExactTimeStamp.Local,
    ) {
        if (relevant) return

        relevant = true

        // set task relevant
        taskRelevances.getValue(instance.taskKey).setRelevant(
                taskRelevances,
                taskHierarchyRelevances,
                instanceRelevances,
                now
        )

        // set parent instance relevant
        if (!instance.isRootInstance(now)) {
            val (parentInstance, _) = instance.getParentInstance(now)!!

            val parentInstanceKey = parentInstance.instanceKey

            if (!instanceRelevances.containsKey(parentInstanceKey))
                instanceRelevances[parentInstanceKey] = InstanceRelevance(parentInstance)

            instanceRelevances.getValue(parentInstanceKey).setRelevant(
                    taskRelevances,
                    taskHierarchyRelevances,
                    instanceRelevances,
                    now
            )
        }

        val childPairs = instance.getChildInstances(now)

        // set child instances relevant
        childPairs.map { (instance, _) ->
            val instanceKey = instance.instanceKey

            if (!instanceRelevances.containsKey(instanceKey))
                instanceRelevances[instanceKey] = InstanceRelevance(instance)

            instanceRelevances.getValue(instanceKey)
        }
                .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }

        // set child hierarchies relevant (not redundant, needed for group tasks)
        childPairs.forEach { (_, taskHierarchy) ->
            taskHierarchyRelevances.getValue(taskHierarchy.taskHierarchyKey).setRelevant(
                    taskRelevances,
                    taskHierarchyRelevances,
                    instanceRelevances,
                    now
            )
        }
    }

    fun setRemoteRelevant(
            remoteCustomTimeRelevances: Map<CustomTimeKey<*>, RemoteCustomTimeRelevance>,
            remoteProjectRelevances: Map<ProjectKey<*>, RemoteProjectRelevance>
    ) {
        check(relevant)

        instance.instanceDateTime
                .time
                .timePair
                .customTimeKey
                ?.let { remoteCustomTimeRelevances.getValue(it).setRelevant() }

        remoteProjectRelevances.getValue(instance.task.project.projectKey).setRelevant()

        (instance.scheduleCustomTimeKey)?.let {
            remoteCustomTimeRelevances.getValue(it).setRelevant()
        }
    }
}
