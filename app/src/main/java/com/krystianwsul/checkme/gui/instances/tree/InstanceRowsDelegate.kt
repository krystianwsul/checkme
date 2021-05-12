package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.treeadapter.TreeNode

class InstanceRowsDelegate(
    private val instanceNodeDelegate: InstanceNodeDelegate,
    private val treeNode: TreeNode<*>,
    showDetails: Boolean = true,
) : MultiLineModelNode.RowsDelegate {

    override val name = instanceNodeDelegate.name

    override val details = if (showDetails) instanceNodeDelegate.details else null

    override val children get() = instanceNodeDelegate.getChildren(treeNode)
}