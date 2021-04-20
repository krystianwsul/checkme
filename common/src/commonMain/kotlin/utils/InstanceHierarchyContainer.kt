package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task

class InstanceHierarchyContainer(private val task: Task) {

    private val parentScheduleKeyToChildInstanceKeys = mutableMapOf<ScheduleKey, MutableSet<InstanceKey>>()

    fun addChildInstance(childInstance: Instance) {
        check(childInstance.exists())

        val childInstanceKey = childInstance.instanceKey

        val parentInstanceKey = childInstance.parentInstance!!.instanceKey
        check(parentInstanceKey.taskKey == task.taskKey)

        val childInstanceKeys =
                parentScheduleKeyToChildInstanceKeys.getOrPut(parentInstanceKey.scheduleKey) { mutableSetOf() }
        check(!childInstanceKeys.contains(childInstanceKey))

        childInstanceKeys += childInstanceKey
    }

    fun removeChildInstance(childInstance: Instance) {
        check(childInstance.exists())

        val childInstanceKey = childInstance.instanceKey

        val parentInstanceKey = childInstance.parentInstance!!.instanceKey
        check(parentInstanceKey.taskKey == task.taskKey)

        val childInstanceKeys = parentScheduleKeyToChildInstanceKeys[parentInstanceKey.scheduleKey]
                ?: throw ScheduleKeyNotFoundException("instanceKey: $parentInstanceKey")
        check(childInstanceKeys.contains(childInstanceKey))

        if (childInstanceKeys.size == 1) {
            parentScheduleKeyToChildInstanceKeys.remove(parentInstanceKey.scheduleKey)
        } else {
            childInstanceKeys -= childInstanceKey
        }
    }

    fun getChildInstances(parentInstanceKey: InstanceKey): List<Instance> {
        check(parentInstanceKey.taskKey == task.taskKey)

        val childInstanceKeys = parentScheduleKeyToChildInstanceKeys[parentInstanceKey.scheduleKey] ?: setOf()

        return childInstanceKeys.map(task.project::getInstance).onEach {
            check(it.exists())
            check(it.parentInstance!!.instanceKey == parentInstanceKey)
        }
    }

    private class ScheduleKeyNotFoundException(message: String) : Exception(message)
}