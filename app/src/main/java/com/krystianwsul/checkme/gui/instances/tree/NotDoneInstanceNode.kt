package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.treeadapter.ModelNode

class NotDoneInstanceNode(
    override val indentation: Int,
    singleBridge: GroupTypeFactory.SingleBridge,
    override val parentNode: ModelNode<AbstractHolder>,
    override val groupAdapter: GroupListFragment.GroupAdapter,
) : NotDoneNode(ContentDelegate.Instance(groupAdapter, singleBridge, indentation)) {

    val instanceData = singleBridge.instanceData

    override fun compareTo(other: ModelNode<AbstractHolder>) =
        contentDelegate.bridge.compareTo((other as NotDoneNode).contentDelegate.bridge)

    override fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean {
        val groupListFragment = groupAdapter.groupListFragment

        return if (groupListFragment.parameters.groupListDataWrapper.taskEditable != false &&
            groupAdapter.treeNodeCollection.selectedChildren.isEmpty() &&
            !treeNode.isExpanded
        ) {
            groupListFragment.dragHelper.startDrag(viewHolder)

            true
        } else {
            false
        }
    }
}