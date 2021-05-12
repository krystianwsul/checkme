package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.treeadapter.TreeNode

class InstanceRowsDelegate(
    instanceData: GroupListDataWrapper.InstanceData,
    private val treeNode: TreeNode<*>,
    showDetails: Boolean = true,
) : MultiLineModelNode.RowsDelegate {

    private val instanceNodeDelegate = InstanceNodeDelegate(instanceData)

    override val name = instanceNodeDelegate.name

    override val details = if (showDetails) instanceNodeDelegate.details else null

    override val children get() = instanceNodeDelegate.getChildren(treeNode)
}