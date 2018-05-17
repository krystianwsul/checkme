package com.krystianwsul.checkme.utils

import com.krystianwsul.checkme.domainmodel.Instance
import junit.framework.Assert
import java.util.*

class InstanceMap<T : Instance> {

    private val instances = HashMap<TaskKey, HashMap<ScheduleKey, T>>()

    fun add(instance: T) {
        val taskKey = instance.taskKey

        var innerMap: HashMap<ScheduleKey, T>? = instances[taskKey]
        if (innerMap == null) {
            innerMap = HashMap()
            instances[taskKey] = innerMap
        }

        val instanceKey = instance.instanceKey

        Assert.assertTrue(!innerMap.containsKey(instanceKey.scheduleKey))

        innerMap[instanceKey.scheduleKey] = instance
    }

    fun removeForce(instance: Instance) {
        val taskKey = instance.taskKey

        val innerMap = instances[taskKey]!!

        val instanceKey = instance.instanceKey

        val innerInstance = innerMap[instanceKey.scheduleKey]
        Assert.assertTrue(instance == innerInstance)

        innerMap.remove(instanceKey.scheduleKey)
    }

    operator fun get(taskKey: TaskKey): Map<ScheduleKey, T> {
        val innerMap = instances[taskKey]
        return innerMap ?: mapOf()
    }

    fun getIfPresent(instanceKey: InstanceKey): T? {
        val innerMap = instances[instanceKey.taskKey] ?: return null

        return innerMap[instanceKey.scheduleKey]
    }

    fun size() = instances.values
            .map { it.size }
            .sum()

    fun values() = instances.values.flatMap { it.values }
}
