package com.krystianwsul.common.utils

class InstanceHierarchyContainer {

    private val childToParent = mutableMapOf<InstanceKey, InstanceKey>()
    private val parentToChildren = mutableMapOf<InstanceKey, MutableSet<InstanceKey>>()

    fun add(parent: InstanceKey, child: InstanceKey) {
        check(!childToParent.containsKey(child))

        childToParent[child] = parent

        if (!parentToChildren.containsKey(parent)) parentToChildren[parent] = mutableSetOf()

        val childrenSet = parentToChildren.getValue(parent)
        check(!childrenSet.contains(child))

        childrenSet += child
    }

    fun getByParent(parent: InstanceKey) = parentToChildren[parent] ?: setOf()
}