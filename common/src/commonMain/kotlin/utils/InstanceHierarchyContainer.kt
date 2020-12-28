package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task

class InstanceHierarchyContainer<T : ProjectType>(private val task: Task<T>) {

    private val parentScheduleKeyToChildInstanceKeys = mutableMapOf<ScheduleKey, MutableSet<InstanceKey>>()

    fun addChildInstance(childInstance: Instance<T>) {
        check(childInstance.exists())

        val childInstanceKey = childInstance.instanceKey

        val parentInstanceKey = childInstance.parentInstanceData!!
                .instance
                .instanceKey
        check(parentInstanceKey.taskKey == task.taskKey)

        val childInstanceKeys =
                parentScheduleKeyToChildInstanceKeys.getOrPut(parentInstanceKey.scheduleKey) { mutableSetOf() }

        childInstanceKeys += childInstanceKey
    }

    fun removeChildInstance(childInstance: Instance<T>) {
        check(childInstance.exists())

        val childInstanceKey = childInstance.instanceKey

        val parentInstanceKey = childInstance.parentInstanceData!!
                .instance
                .instanceKey
        check(parentInstanceKey.taskKey == task.taskKey)

        val childInstanceKeys = parentScheduleKeyToChildInstanceKeys.getValue(parentInstanceKey.scheduleKey)
        check(childInstanceKeys.contains(childInstanceKey))

        if (childInstanceKeys.size == 1) {
            parentScheduleKeyToChildInstanceKeys.remove(parentInstanceKey.scheduleKey)
        } else {
            childInstanceKeys -= childInstanceKey
        }
    }

    fun getChildInstances(parentInstanceKey: InstanceKey): List<Instance<T>> {
        check(parentInstanceKey.taskKey == task.taskKey)

        val childInstanceKeys = parentScheduleKeyToChildInstanceKeys[parentInstanceKey.scheduleKey] ?: setOf()

        return childInstanceKeys.map(task.project::getInstance).onEach {
            check(it.exists())
            check(it.parentInstanceData!!.instance.instanceKey == parentInstanceKey)
        }
    }

    fun getParentScheduleKeys(): Set<ScheduleKey> = parentScheduleKeyToChildInstanceKeys.keys
}