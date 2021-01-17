package com.krystianwsul.treeadapter.locker

import com.krystianwsul.treeadapter.TreeHolder
import com.krystianwsul.treeadapter.TreeNode

class AdapterLocker<T : TreeHolder> {

    private val nodeLockers = mutableMapOf<Any, NodeLocker<T>>()

    fun getNodeLocker(treeNode: TreeNode<*>) = nodeLockers.getOrPut(treeNode.id) { NodeLocker() }
}