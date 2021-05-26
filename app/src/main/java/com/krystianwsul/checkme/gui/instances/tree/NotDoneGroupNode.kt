package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.ImageNode
import com.krystianwsul.treeadapter.ModelNode

class NotDoneGroupNode(
    override val indentation: Int,
    private val nodeCollection: NodeCollection,
    contentDelegate: ContentDelegate,
) : NotDoneNode(contentDelegate) {

    override val parentNode get() = nodeCollection.parentNode

    override val groupAdapter by lazy { nodeCollection.groupAdapter }

    override fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean {
        val groupListFragment = groupAdapter.groupListFragment

        return if (contentDelegate is ContentDelegate.Instance
            && groupListFragment.parameters.groupListDataWrapper.taskEditable != false
            && groupAdapter.treeNodeCollection.selectedChildren.isEmpty()
            && treeNode.parent.displayedChildNodes.none { it.isExpanded }
            && (groupListFragment.parameters.draggable || indentation != 0)
        ) {
            groupListFragment.dragHelper.startDrag(viewHolder)

            true
        } else {
            false
        }
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) = when (other) {
        is ImageNode, is DetailsNode -> 1
        is NotDoneNode -> contentDelegate.compareTo(other.contentDelegate)
        is UnscheduledNode -> if (nodeCollection.searchResults) 1 else -1
        is DividerNode -> -1
        else -> throw IllegalArgumentException()
    }
}
