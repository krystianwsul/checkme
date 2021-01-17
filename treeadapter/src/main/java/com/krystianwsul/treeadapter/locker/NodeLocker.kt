package com.krystianwsul.treeadapter.locker

import com.krystianwsul.treeadapter.TreeHolder
import com.krystianwsul.treeadapter.TreeNode

class NodeLocker<T : TreeHolder> {

    var displayedNodes: List<TreeNode<T>>? = null
}