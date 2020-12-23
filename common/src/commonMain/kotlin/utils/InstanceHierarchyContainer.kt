package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task

class InstanceHierarchyContainer<T : ProjectType>(private val task: Task<T>) {

    companion object {

        fun <T : ProjectType> Instance<T>.parentInstanceKey() =
                (parentState as Instance.ParentState.Parent).parentInstanceKey
    }

    private val parentScheduleKeyToChildInstanceKeys = mutableMapOf<ScheduleKey, MutableSet<InstanceKey>>()

    fun addChildInstance(childInstance: Instance<T>) {
        check(childInstance.exists())

        val childInstanceKey = childInstance.instanceKey

        val parentInstanceKey = childInstance.parentInstanceKey()
        check(parentInstanceKey.taskKey == task.taskKey)

        val childInstanceKeys =
                parentScheduleKeyToChildInstanceKeys.getOrPut(parentInstanceKey.scheduleKey) { mutableSetOf() }

        check(!childInstanceKeys.contains(childInstanceKey))

        childInstanceKeys += childInstanceKey
    }

    // todo group remember to remove entries from here on delete/removeFromParent
    // todo group also remember to remove across parent tasks, when deleting whole task
    fun removeChildInstance(childInstance: Instance<T>) {
        check(childInstance.exists())

        val childInstanceKey = childInstance.instanceKey

        val parentInstanceKey = childInstance.parentInstanceKey()
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
            check(it.parentInstanceKey() == parentInstanceKey)
        }
    }

    fun getParentScheduleKeys(): Set<ScheduleKey> = parentScheduleKeyToChildInstanceKeys.keys
}