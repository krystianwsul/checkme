package com.krystianwsul.treeadapter.locker

import com.krystianwsul.treeadapter.TreeNode

class AdapterLocker {

    private val nodeLockers = mutableMapOf<Any, NodeLocker>()

    fun getNodeLocker(treeNode: TreeNode<*>) = nodeLockers.getOrPut(treeNode.id) { NodeLocker() }
}