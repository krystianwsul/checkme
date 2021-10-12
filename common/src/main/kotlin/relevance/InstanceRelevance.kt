package com.krystianwsul.common.relevance


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey


class InstanceRelevance(val instance: Instance) {

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
            now,
            listOf(instance.instanceKey.toString()),
        )

        // set parent instance relevant
        instance.parentInstance?.let {
            instanceRelevances.getOrPut(it).setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now)
        }

        // set child instances relevant
        instance.getChildInstances()
            .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true)) }
            .map { instanceRelevances.getOrPut(it) }
            .forEach { it.setRelevant(taskRelevances, taskHierarchyRelevances, instanceRelevances, now) }
    }

    fun setRemoteRelevant(
        customTimeRelevanceCollection: CustomTimeRelevanceCollection,
        remoteProjectRelevances: Map<ProjectKey<*>, RemoteProjectRelevance>,
    ) {
        check(relevant)

        instance.instanceDateTime
            .time
            .timePair
            .customTimeKey
            ?.let { customTimeRelevanceCollection.getRelevance(it).setRelevant() }

        remoteProjectRelevances.getValue(instance.getProject().projectKey).setRelevant()
        remoteProjectRelevances.getValue(instance.task.project.projectKey).setRelevant()

        instance.scheduleKey
            .scheduleTimePair
            .customTimeKey
            ?.let { customTimeRelevanceCollection.getRelevance(it).setRelevant() }
    }
}
