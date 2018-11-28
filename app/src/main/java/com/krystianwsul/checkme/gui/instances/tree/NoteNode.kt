package com.krystianwsul.checkme.gui.instances.tree

import android.support.v7.widget.RecyclerView
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import java.util.*

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
        treeNode = TreeNode(this, nodeContainer, false, false)

        treeNode.setChildTreeNodes(ArrayList())
        return treeNode
    }

    override val name get() = Triple(note, colorPrimary, false)

    override fun onLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener()

    override val isSelectable = false

    override fun onClick() = Unit

    override val isVisibleWhenEmpty = true

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = true

    override fun compareTo(other: ModelNode): Int {
        check(other is NotDoneGroupNode || other is UnscheduledNode || other is DividerNode)

        return -1
    }
}
