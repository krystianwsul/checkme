package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.ImageNode
import com.krystianwsul.treeadapter.ModelNode

class NotDoneGroupNode(
    override val indentation: Int,
    private val nodeCollection: NodeCollection,
    val instanceDatas: MutableList<GroupListDataWrapper.InstanceData>,
) : NotDoneNode(
    instanceDatas.singleOrNull()
        ?.let { ContentDelegate.Instance(nodeCollection.groupAdapter, it, indentation) }
        ?: ContentDelegate.Group(nodeCollection.groupAdapter, instanceDatas, indentation)
) {

    init {
        check(instanceDatas.isNotEmpty())
    }

    override val isDraggable = true

    override val parentNode get() = nodeCollection.parentNode

    val timeStamp = instanceDatas.map { it.instanceTimeStamp }
        .distinct()
        .single()

    fun singleInstance() = contentDelegate is ContentDelegate.Instance

    override val groupAdapter by lazy { nodeCollection.groupAdapter }

    override fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean {
        val groupListFragment = groupAdapter.groupListFragment

        return if (singleInstance()
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
        is NotDoneGroupNode -> {
            val timeStampComparison = timeStamp.compareTo(other.timeStamp)
            if (timeStampComparison != 0) {
                timeStampComparison
            } else {
                fun NotDoneGroupNode.instanceData() = (contentDelegate as ContentDelegate.Instance).instanceData

                instanceData().compareTo(other.instanceData())
            }
        }
        is UnscheduledNode -> if (nodeCollection.searchResults) 1 else -1
        is DividerNode -> -1
        else -> throw IllegalArgumentException()
    }
}
