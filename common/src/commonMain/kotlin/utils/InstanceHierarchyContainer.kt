package com.krystianwsul.common.utils

class InstanceHierarchyContainer {

    private val childToParent = mutableMapOf<InstanceKey, InstanceKey>()
    private val parentToChildren = mutableMapOf<InstanceKey, MutableSet<InstanceKey>>()

    // todo group accept instances instead of keys where possible, do sanity checks like .exists()
    // todo group remember to remove entries from here on 1. parentState change, 2. delete/removeFromParent
    fun add(parent: InstanceKey, child: InstanceKey) {
        check(!childToParent.containsKey(child))

        childToParent[child] = parent

        if (!parentToChildren.containsKey(parent)) parentToChildren[parent] = mutableSetOf()

        val childrenSet = parentToChildren.getValue(parent)
        check(!childrenSet.contains(child))

        childrenSet += child
    }

    // todo group return instances instead of keys where possible, do sanity checks like .exists()
    fun getByParent(parent: InstanceKey) = parentToChildren[parent] ?: setOf()
}