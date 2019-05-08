package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class NoteNode(private val note: String) : GroupHolderNode(0) {

    override lateinit var treeNode: TreeNode
        private set

    private lateinit var nodeContainer: NodeContainer

    override val id get() = Id(nodeContainer.id)

    data class Id(val id: Any)

    init {
        check(note.isNotEmpty())
    }

    fun initialize(nodeContainer: NodeContainer): TreeNode {
        this.nodeContainer = nodeContainer

        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)
        treeNode.setChildTreeNodes(listOf())

        return treeNode
    }

    override val textSelectable = true

    override val name get() = Triple(note, colorPrimary, false)

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = true

    override fun compareTo(other: ModelNode) = -1
}
