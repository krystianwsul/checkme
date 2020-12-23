package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task

class InstanceHierarchyContainer<T : ProjectType>(private val task: Task<T>) {

    companion object {

        fun <T : ProjectType> Instance<T>.parentInstanceKey() =
                (parentState as Instance.ParentState.Parent).parentInstanceKey
    }

    private val childInstanceKeyToParentInstanceKey = mutableMapOf<InstanceKey, InstanceKey>()
    private val parentInstanceKeyToChildInstanceKeys = mutableMapOf<InstanceKey, MutableSet<InstanceKey>>()

    fun addChildInstance(childInstance: Instance<T>) {
        check(childInstance.exists())

        val childInstanceKey = childInstance.instanceKey
        check(!childInstanceKeyToParentInstanceKey.containsKey(childInstanceKey))

        val parentInstanceKey = childInstance.parentInstanceKey()
        check(parentInstanceKey.taskKey == task.taskKey)

        childInstanceKeyToParentInstanceKey[childInstanceKey] = parentInstanceKey

        val childInstanceKeys = parentInstanceKeyToChildInstanceKeys.getOrPut(parentInstanceKey) { mutableSetOf() }
        check(!childInstanceKeys.contains(childInstanceKey))

        childInstanceKeys += childInstanceKey
    }

    // todo group remember to remove entries from here on delete/removeFromParent
    // todo group also remember to remove across parent tasks, when deleting whole task
    fun removeChildInstance(childInstance: Instance<T>) {
        check(childInstance.exists())

        val childInstanceKey = childInstance.instanceKey
        check(childInstanceKeyToParentInstanceKey.containsKey(childInstanceKey))

        val parentInstanceKey = childInstance.parentInstanceKey()
        check(parentInstanceKey.taskKey == task.taskKey)

        check(childInstanceKeyToParentInstanceKey.remove(childInstanceKey) == parentInstanceKey)

        val childInstanceKeys = parentInstanceKeyToChildInstanceKeys.getValue(parentInstanceKey)
        check(childInstanceKeys.contains(childInstanceKey))

        childInstanceKeys -= childInstanceKey
    }

    fun getChildInstances(parentInstanceKey: InstanceKey): List<Instance<T>> {
        check(parentInstanceKey.taskKey == task.taskKey)

        val childInstanceKeys = parentInstanceKeyToChildInstanceKeys[parentInstanceKey] ?: setOf()

        return childInstanceKeys.map(task.project::getInstance).onEach {
            check(it.exists())
            check(it.parentInstanceKey() == parentInstanceKey)
        }
    }
}