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
) : NotDoneNode(ContentDelegate.Instance(groupAdapter, instanceData, indentation)) {

    override fun compareTo(other: ModelNode<AbstractHolder>) =
        instanceData.compareTo((other as NotDoneInstanceNode).instanceData)

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

    override val deselectParent = true
}