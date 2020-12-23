package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Project

class InstanceHierarchyContainer<T : ProjectType>(private val project: Project<T>) {

    companion object {

        fun <T : ProjectType> Instance<T>.parentKey() = (parentState as Instance.ParentState.Parent).parentInstanceKey
    }

    private val childToParent = mutableMapOf<InstanceKey, InstanceKey>()
    private val parentToChildren = mutableMapOf<InstanceKey, MutableSet<InstanceKey>>()

    fun addChild(child: Instance<T>) {
        check(child.exists())

        val childKey = child.instanceKey
        check(!childToParent.containsKey(childKey))

        val parentKey = child.parentKey()

        childToParent[childKey] = parentKey

        if (!parentToChildren.containsKey(parentKey)) parentToChildren[parentKey] = mutableSetOf()

        val childrenSet = parentToChildren.getValue(parentKey)
        check(!childrenSet.contains(childKey))

        childrenSet += childKey
    }

    // todo group remember to remove entries from here on delete/removeFromParent
    fun removeChild(child: Instance<T>) {
        check(child.exists())

        val childKey = child.instanceKey
        check(childToParent.containsKey(childKey))

        val parentKey = child.parentKey()

        check(childToParent.remove(childKey) == parentKey)

        val childrenSet = parentToChildren.getValue(parentKey)
        check(childrenSet.contains(childKey))

        childrenSet -= childKey
    }

    fun getByParentKey(parentKey: InstanceKey): List<Instance<T>> {
        val childKeys = parentToChildren[parentKey] ?: setOf()

        return childKeys.map(project::getInstance).onEach {
            check(it.exists())
            check(it.parentKey() == parentKey)
        }
    }
}