package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.treeadapter.TreeNode

open class InstanceRowsDelegate(
    private val instanceNodeDelegate: InstanceNodeDelegate,
    private val treeNode: TreeNode<*>,
) : MultiLineModelNode.RowsDelegate {

    override val name = instanceNodeDelegate.name

    override val details = instanceNodeDelegate.details

    override val children get() = instanceNodeDelegate.getChildren(treeNode)
}