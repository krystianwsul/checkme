package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Project

class InstanceHierarchyContainer<T : ProjectType>(private val project: Project<T>) {

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

        childInstanceKeyToParentInstanceKey[childInstanceKey] = parentInstanceKey

        val childInstanceKeys = parentInstanceKeyToChildInstanceKeys.getOrPut(parentInstanceKey) { mutableSetOf() }
        check(!childInstanceKeys.contains(childInstanceKey))

        childInstanceKeys += childInstanceKey
    }

    // todo group remember to remove entries from here on delete/removeFromParent
    fun removeChildInstance(childInstance: Instance<T>) {
        check(childInstance.exists())

        val childInstanceKey = childInstance.instanceKey
        check(childInstanceKeyToParentInstanceKey.containsKey(childInstanceKey))

        val parentInstanceKey = childInstance.parentInstanceKey()

        check(childInstanceKeyToParentInstanceKey.remove(childInstanceKey) == parentInstanceKey)

        val childInstanceKeys = parentInstanceKeyToChildInstanceKeys.getValue(parentInstanceKey)
        check(childInstanceKeys.contains(childInstanceKey))

        childInstanceKeys -= childInstanceKey
    }

    fun getChildInstances(parentInstanceKey: InstanceKey): List<Instance<T>> {
        val childInstanceKeys = parentInstanceKeyToChildInstanceKeys[parentInstanceKey] ?: setOf()

        return childInstanceKeys.map(project::getInstance).onEach {
            check(it.exists())
            check(it.parentInstanceKey() == parentInstanceKey)
        }
    }
}