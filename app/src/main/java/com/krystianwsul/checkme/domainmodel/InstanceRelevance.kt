package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import junit.framework.Assert

class InstanceRelevance(val instance: Instance) {

    var relevant = false
        private set

    fun setRelevant(taskRelevances: Map<TaskKey, TaskRelevance>, instanceRelevances: MutableMap<InstanceKey, InstanceRelevance>, customTimeRelevances: Map<Int, LocalCustomTimeRelevance>, now: ExactTimeStamp) {
        if (relevant) return

        relevant = true

        // set task relevant
        val taskRelevance = taskRelevances[instance.taskKey]
        Assert.assertTrue(taskRelevance != null)

        taskRelevance!!.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now)

        // set parent instance relevant
        if (!instance.isRootInstance(now)) {
            val parentInstance = instance.getParentInstance(now)
            Assert.assertTrue(parentInstance != null)

            val parentInstanceKey = parentInstance!!.instanceKey

            if (!instanceRelevances.containsKey(parentInstanceKey))
                instanceRelevances[parentInstanceKey] = InstanceRelevance(parentInstance)

            val parentInstanceRelevance = instanceRelevances[parentInstanceKey]
            Assert.assertTrue(parentInstanceRelevance != null)

            parentInstanceRelevance!!.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now)
        }

        // set child instances relevant
        instance.getChildInstances(now)
                .map { (instance) ->
                    val instanceKey = instance.instanceKey

                    if (!instanceRelevances.containsKey(instanceKey))
                        instanceRelevances[instanceKey] = InstanceRelevance(instance)

                    instanceRelevances[instanceKey]!!
                }
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now) }

        // set custom time relevant
        val scheduleCustomTimeKey = instance.scheduleCustomTimeKey
        if (scheduleCustomTimeKey?.localCustomTimeId != null)
            customTimeRelevances[scheduleCustomTimeKey.localCustomTimeId]!!.setRelevant()

        // set custom time relevant
        val instanceCustomTimeId = instance.instanceCustomTimeKey
        if (instanceCustomTimeId?.localCustomTimeId != null)
            customTimeRelevances[instanceCustomTimeId.localCustomTimeId]!!.setRelevant()
    }

    fun setRemoteRelevant(remoteCustomTimeRelevances: Map<kotlin.Pair<String, String>, RemoteCustomTimeRelevance>, remoteProjectRelevances: Map<String, RemoteProjectRelevance>) {
        Assert.assertTrue(relevant)

        val pair = instance.remoteCustomTimeKey
        val remoteProject = instance.remoteNullableProject
        if (pair != null) {
            Assert.assertTrue(remoteProject != null)

            remoteCustomTimeRelevances[pair]!!.setRelevant()
        }

        if (remoteProject != null) remoteProjectRelevances[remoteProject.id]!!.setRelevant()
    }
}
