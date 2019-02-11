package com.krystianwsul.checkme.domainmodel.relevance

import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp


class InstanceRelevance(val instance: Instance) {

    var relevant = false
        private set

    fun setRelevant(taskRelevances: Map<TaskKey, TaskRelevance>, instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>, now: ExactTimeStamp) {
        if (relevant) return

        relevant = true

        // set task relevant
        taskRelevances.getValue(instance.taskKey).setRelevant(taskRelevances, instanceRelevances, now)

        // set parent instance relevant
        if (!instance.isRootInstance(now)) {
            val parentInstance = instance.getParentInstance(now)!!

            val parentInstanceKey = parentInstance.instanceKey

            if (!instanceRelevances.containsKey(parentInstanceKey))
                instanceRelevances[parentInstanceKey] = InstanceRelevance(parentInstance)

            instanceRelevances[parentInstanceKey]!!.setRelevant(taskRelevances, instanceRelevances, now)
        }

        // set child instances relevant
        instance.getChildInstances(now)
                .map { (instance) ->
                    val instanceKey = instance.instanceKey

                    if (!instanceRelevances.containsKey(instanceKey))
                        instanceRelevances[instanceKey] = InstanceRelevance(instance)

                    instanceRelevances[instanceKey]!!
                }
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, now) }
    }

    fun setRemoteRelevant(remoteCustomTimeRelevances: Map<Pair<String, RemoteCustomTimeId>, RemoteCustomTimeRelevance>, remoteProjectRelevances: Map<String, RemoteProjectRelevance>) {
        check(relevant)

        val pair = instance.remoteCustomTimeKey
        val remoteProject = instance.remoteNullableProject
        if (pair != null) {
            check(remoteProject != null)

            remoteCustomTimeRelevances.getValue(pair).setRelevant()
        }

        if (remoteProject != null) remoteProjectRelevances.getValue(remoteProject.id).setRelevant()

        (instance.scheduleCustomTimeKey as? CustomTimeKey.RemoteCustomTimeKey<*>)?.let {
            remoteCustomTimeRelevances.getValue(Pair(it.remoteProjectId, it.remoteCustomTimeId)).setRelevant()
        }
    }
}
