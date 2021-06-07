package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.treeadapter.ModelNode

class NotDoneInstanceNode(
    override val indentation: Int,
    val instanceData: GroupListDataWrapper.InstanceData,
    override val parentNode: ModelNode<AbstractHolder>,
    override val groupAdapter: GroupListFragment.GroupAdapter,
) : NotDoneNode(ContentDelegate.Instance(groupAdapter, instanceData, indentation, false)) {

    override val id: Any = Id(super.id)

    private data class Id(val innerId: Any)

    private fun GroupType.toBridge() = bridge as GroupType.TreeAdapterBridgeFactory.Bridge

    override fun compareTo(other: ModelNode<AbstractHolder>) =
        contentDelegate.groupType.toBridge().compareTo((other as NotDoneNode).contentDelegate.groupType.toBridge())

    override fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean {
        val groupListFragment = groupAdapter.groupListFragment

        return if (groupListFragment.parameters.groupListDataWrapper.taskEditable != false
            && groupAdapter.treeNodeCollection.selectedChildren.isEmpty()
            && treeNode.parent.displayedChildNodes.none { it.isExpanded }
        ) {
            groupListFragment.dragHelper.startDrag(viewHolder)

            true
        } else {
            false
        }
    }
}