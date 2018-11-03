package com.krystianwsul.checkme.utils

import com.krystianwsul.checkme.domainmodel.Instance

class InstanceMap<T : Instance> {

    private val instances = HashMap<TaskKey, HashMap<ScheduleKey, T>>()

    private fun init(taskKey: TaskKey): HashMap<ScheduleKey, T> {
        if (instances[taskKey] == null)
            instances[taskKey] = HashMap()
        return instances[taskKey]!!
    }

    fun add(instance: T) {
        val taskKey = instance.taskKey

        val innerMap = init(taskKey)

        val instanceKey = instance.instanceKey
        check(!innerMap.containsKey(instanceKey.scheduleKey))

        innerMap[instanceKey.scheduleKey] = instance
    }

    fun removeForce(instance: Instance) {
        val taskKey = instance.taskKey

        val innerMap = instances[taskKey]!!

        val instanceKey = instance.instanceKey

        val innerInstance = innerMap[instanceKey.scheduleKey]
        check(instance == innerInstance)

        innerMap.remove(instanceKey.scheduleKey)
    }

    operator fun get(taskKey: TaskKey) = init(taskKey)

    fun getIfPresent(instanceKey: InstanceKey) = instances[instanceKey.taskKey]?.get(instanceKey.scheduleKey)

    fun size() = instances.values
            .map { it.size }
            .sum()

    fun values() = instances.values.flatMap { it.values }
}
