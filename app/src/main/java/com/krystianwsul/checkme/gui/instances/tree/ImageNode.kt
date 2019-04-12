package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.firebase.ImageState
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import java.util.*

class ImageNode(override val imageState: ImageState) : GroupHolderNode(0) { // todo image add for tasks

    override lateinit var treeNode: TreeNode
        private set

    private lateinit var nodeContainer: NodeContainer

    override val id get() = Id(nodeContainer.id)

    data class Id(val id: Any)

    override val name: Triple<String, Int, Boolean>? = null

    override val isSeparatorVisibleWhenNotExpanded = true

    override val ignoreStale = true

    fun initialize(nodeContainer: NodeContainer): TreeNode {
        this.nodeContainer = nodeContainer
        treeNode = TreeNode(this, nodeContainer, false, false)

        treeNode.setChildTreeNodes(ArrayList())
        return treeNode
    }

    override fun compareTo(other: ModelNode) = if (other is NoteNode) 1 else -1
}